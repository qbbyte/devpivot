package com.ruoyi.ai.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.common.utils.StringUtils;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * 大模型调用客户端：调用 OpenAI 兼容的 /chat/completions 接口。
 *
 * 配置来源于 ai_model_config 表（按 modelCode 查询启用项）。
 * 若未配置 baseUrl / apiKey，或调用失败，返回友好的兜底文案，保证门户页面不报错。
 *
 * @author devpivot
 * @date 2026-08-05
 */
@Service
public class AiModelClient
{
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final IAiModelConfigService modelConfigService;

    public AiModelClient(IAiModelConfigService modelConfigService)
    {
        this.modelConfigService = modelConfigService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
        // 流式调用客户端：连接超时 15s（模型地址不可达快速失败），读超时放宽到 120s（长生成不被中途断开）
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                .responseTimeout(Duration.ofSeconds(120));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 调用指定模型进行对话补全
     *
     * @param modelCode     模型标识（对应 ai_model_config.model_code）
     * @param systemPrompt  系统提示词
     * @param userPrompt    用户输入
     * @return 模型回复文本；配置缺失或调用失败时返回兜底说明
     */
    public String chat(String modelCode, String systemPrompt, String userPrompt)
    {
        AiModelConfig cfg = resolveConfig(modelCode);

        if (cfg == null || StringUtils.isBlank(cfg.getApiKey()) || StringUtils.isBlank(cfg.getBaseUrl()))
        {
            return "（未配置模型「" + modelCode + "」的接口地址或密钥，请在「AI 模型配置」中维护 " + modelCode
                    + " 的 baseUrl 与 apiKey 后重试。当前为本地兜底回复。）\n\n" + fallbackAnalysis(userPrompt);
        }

        // 与 chatStream 一致：仅对传输层瞬断重试，模型返回的状态码/业务错误按原文返回不重试。
        int attempt = 0;
        while (true)
        {
            try
            {
                return chatOnce(cfg, modelCode, systemPrompt, userPrompt);
            }
            catch (Exception e)
            {
                boolean retryable = isRetryable(e);
                if (!retryable || attempt >= STREAM_MAX_RETRIES)
                {
                    String prefix = (attempt == 0 && !retryable) ? "发生错误" : ("重试 " + attempt + " 次仍失败");
                    return "（调用模型「" + modelCode + "」时" + prefix + "：" + e.getMessage() + "）";
                }
                attempt++;
                try
                {
                    Thread.sleep(STREAM_RETRY_BACKOFF_MS * attempt);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    return "（调用模型「" + modelCode + "」时重试被中断）";
                }
            }
        }
    }

    /**
     * 单次非流式请求（不含重试）。仅传输层异常会向上抛出，由 {@link #chat} 决定是否重试；
     * 模型返回的状态码异常或业务 error 均以普通字符串返回，不抛异常。
     */
    private String chatOnce(AiModelConfig cfg, String modelCode, String systemPrompt, String userPrompt) throws Exception
    {
        String base = cfg.getBaseUrl().trim();
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/chat/completions";

        Map<String, Object> body = new HashMap<>(8);
        // 发给 OpenAI 兼容接口的 model 字段应使用「模型标识(model_code)」而非展示名(model_name)，
        // 否则展示名里带中文/特殊字符（如「（完整）」）会导致 dashscope 报 model_not_found。
        body.put("model", StringUtils.isNotBlank(cfg.getModelCode()) ? cfg.getModelCode() : cfg.getModelName());
        List<Map<String, Object>> messages = new ArrayList<>(2);
        Map<String, Object> sys = new HashMap<>(2);
        sys.put("role", "system");
        sys.put("content", systemPrompt == null ? "" : systemPrompt);
        messages.add(sys);
        Map<String, Object> user = new HashMap<>(2);
        user.put("role", "user");
        user.put("content", userPrompt == null ? "" : userPrompt);
        messages.add(user);
        body.put("messages", messages);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cfg.getApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null)
        {
            return "（模型「" + modelCode + "」返回异常状态码：" + resp.getStatusCode() + "）";
        }
        JSONObject json = JSON.parseObject(resp.getBody());
        if (json.containsKey("error"))
        {
            JSONObject error = json.getJSONObject("error");
            return "（模型调用失败：" + (error == null ? "未知错误" : error.getString("message")) + "）";
        }
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && choices.size() > 0)
        {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message.getString("content");
            return content == null ? "" : content;
        }
        return "（模型「" + modelCode + "」返回内容为空）";
    }

    /**
     * 测试指定模型配置是否可用（非流式、低开销）。
     *
     * 直接以传入的 {@link AiModelConfig} 为准发送一次极简对话请求（max_tokens 很小），
     * 不依赖 enable/modelCode 过滤，便于在后台管理页对单条配置做连通性自检。
     *
     * @param cfg 待测试的模型配置（通常来自 ai_model_config 按 modelId 查询）
     * @return 测试结果 Map：success(布尔)、message(详情)、statusCode(可选)、latencyMs(可选)、sample(可选)
     */
    public Map<String, Object> testModel(AiModelConfig cfg)
    {
        Map<String, Object> result = new HashMap<>(6);
        if (cfg == null)
        {
            result.put("success", false);
            result.put("message", "模型配置不存在");
            return result;
        }
        if (StringUtils.isBlank(cfg.getModelCode()) || StringUtils.isBlank(cfg.getBaseUrl())
                || StringUtils.isBlank(cfg.getApiKey()))
        {
            result.put("success", false);
            result.put("message", "配置不完整：请检查 模型标识(model_code)、接口地址(baseUrl)、API密钥(apiKey) 是否已填写");
            return result;
        }

        long start = System.currentTimeMillis();
        try
        {
            String base = cfg.getBaseUrl().trim();
            if (base.endsWith("/"))
            {
                base = base.substring(0, base.length() - 1);
            }
            String url = base + "/chat/completions";

            Map<String, Object> body = new HashMap<>(8);
            // 测试必须使用 model_code（API 真实模型 ID），与 chat/chatStream 保持一致
            body.put("model", cfg.getModelCode());
            List<Map<String, Object>> messages = new ArrayList<>(1);
            Map<String, Object> user = new HashMap<>(2);
            user.put("role", "user");
            user.put("content", "请只回复两个字：正常");
            messages.add(user);
            body.put("messages", messages);
            body.put("temperature", 0);
            body.put("max_tokens", 16);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + cfg.getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            long cost = System.currentTimeMillis() - start;

            result.put("statusCode", resp.getStatusCode().value());
            result.put("latencyMs", cost);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null)
            {
                JSONObject json = JSON.parseObject(resp.getBody());
                if (json.containsKey("error"))
                {
                    JSONObject error = json.getJSONObject("error");
                    result.put("success", false);
                    result.put("message", "HTTP 200，但模型返回错误：" + (error == null ? "未知错误" : error.getString("message")));
                }
                else
                {
                    JSONArray choices = json.getJSONArray("choices");
                    String content = choices != null && choices.size() > 0
                            ? choices.getJSONObject(0).getJSONObject("message").getString("content") : "";
                    result.put("success", true);
                    result.put("message", "调用成功，模型已正常响应（耗时 " + cost + "ms）");
                    if (StringUtils.isNotBlank(content))
                    {
                        result.put("sample", content);
                    }
                }
            }
            else
            {
                result.put("success", false);
                result.put("message", "HTTP 状态码异常：" + resp.getStatusCode());
            }
        }
        catch (Exception e)
        {
            long cost = System.currentTimeMillis() - start;
            result.put("success", false);
            result.put("message", "调用失败：" + e.getMessage());
            result.put("latencyMs", cost);
        }
        return result;
    }

    private AiModelConfig resolveConfig(String modelCode)
    {
        try
        {
            AiModelConfig query = new AiModelConfig();
            query.setIsEnabled("0");
            List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(query);
            if (list != null)
            {
                for (AiModelConfig c : list)
                {
                    if (modelCode != null && modelCode.equals(c.getModelCode()))
                    {
                        return c;
                    }
                }
            }
        }
        catch (Exception e)
        {
            // 配置查询异常，返回 null 走兜底
        }
        return null;
    }

    /**
     * 流式调用指定模型进行对话补全（SSE）。
     *
     * 与 {@link #chat(String, String, String)} 不同，本方法在模型产生每个 token 时立即通过
     * onToken 回调推送，便于上层（如 SseEmitter）实现逐字输出。配置缺失或调用失败时，
     * 将一次性把兜底文案通过 onToken 推送。
     *
     * @param modelCode     模型标识（对应 ai_model_config.model_code）
     * @param systemPrompt  系统提示词
     * @param userPrompt    用户输入
     * @param onToken       每个文本片段（delta）的回调
     */
    /** 流式调用最大重试次数（瞬断重试，不重试模型配置错误）。 */
    private static final int STREAM_MAX_RETRIES = 2;
    /** 重试退避基数（毫秒），第 N 次重试等待 N * 该值。 */
    private static final long STREAM_RETRY_BACKOFF_MS = 600;

    /**
     * 流式调用指定模型进行对话补全（SSE），带瞬断重试。
     *
     * 仅对传输层瞬断（Connection reset / 超时 / 连接被拒 / 连接过早关闭 / 5xx / 429）自动重试；
     * 模型配置错误（401/400/404 等以异常形式抛出的 4xx）或模型以 SSE data 返回的
     * 业务错误（model_not_found 等）不属于瞬断，不会触发重试。
     *
     * @param modelCode     模型标识（对应 ai_model_config.model_code）
     * @param systemPrompt  系统提示词
     * @param userPrompt    用户输入
     * @param onToken       每个文本片段（delta）的回调
     */
    public void chatStream(String modelCode, String systemPrompt, String userPrompt, Consumer<String> onToken)
    {
        AiModelConfig cfg = resolveConfig(modelCode);

        if (cfg == null || StringUtils.isBlank(cfg.getApiKey()) || StringUtils.isBlank(cfg.getBaseUrl()))
        {
            onToken.accept("（未配置模型「" + modelCode + "」的接口地址或密钥，请在「AI 模型配置」中维护 " + modelCode
                    + " 的 baseUrl 与 apiKey 后重试。当前为本地兜底回复。）\n\n" + fallbackAnalysis(userPrompt));
            return;
        }

        int attempt = 0;
        while (true)
        {
            try
            {
                streamOnce(cfg, modelCode, systemPrompt, userPrompt, onToken);
                return;
            }
            catch (Exception e)
            {
                boolean retryable = isRetryable(e);
                if (!retryable || attempt >= STREAM_MAX_RETRIES)
                {
                    String prefix = (attempt == 0 && !retryable) ? "发生错误" : ("重试 " + attempt + " 次仍失败");
                    onToken.accept("\n（调用模型「" + modelCode + "」时" + prefix + "：" + e.getMessage() + "）");
                    return;
                }
                attempt++;
                // 推一条重试提示，告知用户连接中断正在恢复（避免误以为卡死）
                onToken.accept("\n（连接中断，正在第 " + attempt + " 次重试…）");
                try
                {
                    Thread.sleep(STREAM_RETRY_BACKOFF_MS * attempt);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    onToken.accept("\n（调用模型「" + modelCode + "」时重试被中断）");
                    return;
                }
            }
        }
    }

    /**
     * 单次流式请求（不含重试）。异常会向上抛出，由 {@link #chatStream} 决定是否重试。
     */
    private void streamOnce(AiModelConfig cfg, String modelCode, String systemPrompt, String userPrompt,
                            Consumer<String> onToken) throws Exception
    {
        String base = cfg.getBaseUrl().trim();
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/chat/completions";

        Map<String, Object> body = new HashMap<>(8);
        // 发给 OpenAI 兼容接口的 model 字段应使用「模型标识(model_code)」而非展示名(model_name)，
        // 否则展示名里带中文/特殊字符（如「（完整）」）会导致 dashscope 报 model_not_found。
        body.put("model", StringUtils.isNotBlank(cfg.getModelCode()) ? cfg.getModelCode() : cfg.getModelName());
        List<Map<String, Object>> messages = new ArrayList<>(2);
        Map<String, Object> sys = new HashMap<>(2);
        sys.put("role", "system");
        sys.put("content", systemPrompt == null ? "" : systemPrompt);
        messages.add(sys);
        Map<String, Object> user = new HashMap<>(2);
        user.put("role", "user");
        user.put("content", userPrompt == null ? "" : userPrompt);
        messages.add(user);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("stream", true);

        StringBuilder sseBuffer = new StringBuilder();
        webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(db -> {
                    String s = db.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(db);
                    return s;
                })
                .doOnNext(chunk -> {
                    if (chunk == null) return;
                    sseBuffer.append(chunk);
                    int nl;
                    while ((nl = sseBuffer.indexOf("\n")) >= 0)
                    {
                        String line = sseBuffer.substring(0, nl);
                        sseBuffer.delete(0, nl + 1);
                        handleSseLine(line.trim(), onToken);
                    }
                })
                .blockLast();
        // 处理缓冲区残留（极少数情况下最后一段未以换行结尾）
        if (sseBuffer.length() > 0)
        {
            handleSseLine(sseBuffer.toString().trim(), onToken);
        }
    }

    /**
     * 判断异常是否属于「传输层瞬断」，可安全重试：
     * 连接重置、超时、连接被拒、连接过早关闭、Broken pipe、IOException / SocketException，
     * 以及 WebClient 的 5xx 与 429。4xx（401/400/404 等配置错误）不重试。
     */
    private boolean isRetryable(Exception e)
    {
        Throwable t = e;
        while (t != null)
        {
            if (t instanceof java.io.IOException || t instanceof java.net.SocketException)
            {
                // 传输层异常，绝大多数属于瞬断（连接重置 / 过早关闭 / 管道破裂等）
                return true;
            }
            if (t instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre)
            {
                int code = wcre.getStatusCode().value();
                return code == 429 || (code >= 500 && code < 600);
            }
            String msg = t.getMessage();
            if (msg != null)
            {
                String m = msg.toLowerCase();
                if (m.contains("connection reset") || m.contains("reset by peer")
                        || m.contains("connection timed out") || m.contains("read timed out")
                        || m.contains("connection refused") || m.contains("prematurely closed")
                        || m.contains("broken pipe") || m.contains("socket closed")
                        || m.contains("timed out") || m.contains("timeout"))
                {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 解析单行 SSE（data: {...}），提取 choices[0].delta.content 并回调。
     * 忽略注释行、[DONE] 以及非 JSON 内容。
     */
    private void handleSseLine(String line, Consumer<String> onToken)
    {
        if (line == null || line.isEmpty() || !line.startsWith("data:")) return;
        String data = line.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) return;
        try
        {
            JSONObject o = JSON.parseObject(data);
            if (o == null) return;
            if (o.containsKey("error"))
            {
                JSONObject err = o.getJSONObject("error");
                onToken.accept("\n（模型返回错误：" + (err == null ? "未知错误" : err.getString("message")) + "）");
                return;
            }
            JSONArray choices = o.getJSONArray("choices");
            if (choices != null && !choices.isEmpty())
            {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject delta = choice == null ? null : choice.getJSONObject("delta");
                if (delta != null)
                {
                    String c = delta.getString("content");
                    if (c != null && !c.isEmpty())
                    {
                        onToken.accept(c);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            // 非 JSON 行（如 SSE 注释行以冒号开头）忽略
        }
    }

    private String fallbackAnalysis(String userPrompt)
    {
        return "基于您的回答「" + (userPrompt == null ? "" : userPrompt) + "」，建议结合项目实际规模、预算与团队运维能力综合评估，"
                + "并在架构设计阶段明确容量规划、高可用方案与数据一致性策略。";
    }
}
