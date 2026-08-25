package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.utils.ParamValidator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.domain.AiVersionRecord;
import com.ruoyi.project.domain.AiReqBaseline;
import com.ruoyi.project.service.IAiReqBaselineService;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.project.service.IAiClarifySessionService;
import com.ruoyi.project.service.IAiVersionRecordService;
import com.ruoyi.ai.service.IAiModelConfigService;

/**
 * AI 澄清 · AI 接口（/ai/clarify）
 * 仅承载 AI/流式能力：可用模型、发送消息、动态出题。
 * 数据读写见同包 ClarifyController（/system/clarify）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/ai/clarify")
public class AiClarifyController extends BaseController
{

    @Autowired
    private IAiClarifySessionService aiClarifySessionService;

    @Autowired
    private IAiReqBaselineService reqBaselineService;

    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiVersionRecordService versionRecordService;

    /** 流式推送任务线程池（IO 密集，按需扩缩） */
    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(AiClarifyController.class);

    /** 通用兜底问题：模型未配置或解析失败时保证前端仍有题可问 */
    private Map<String, Object> fallbackQuestion()
    {
        Map<String, Object> q = new HashMap<>(2);
        q.put("content", "为了进一步明确需求细节，请继续描述您的想法或回答以下问题：\n\n"
                + "**「关于刚才讨论的需求点，您还有什么补充或需要调整的地方吗？」**");
        q.put("options", Arrays.asList(
                mapOf("label", "没有补充，进入下一步", "value", "done"),
                mapOf("label", "我有补充说明", "value", "supplement")
        ));
        return q;
    }

    /** 从模型文本中解析出 {content, options} 结构；容错 markdown 围栏与脏字符，失败返回 null */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseQuestionJson(String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return null;
        }
        String s = raw.trim();
        // 去除 ```json ... ``` 围栏
        if (s.startsWith("```"))
        {
            int first = s.indexOf('{');
            int last = s.lastIndexOf('}');
            if (first >= 0 && last > first)
            {
                s = s.substring(first, last + 1);
            }
        }
        try
        {
            Object o = JSON.parse(s);
            if (!(o instanceof Map))
            {
                return null;
            }
            Map<String, Object> m = (Map<String, Object>) o;
            Object content = m.get("content");
            if (content == null || String.valueOf(content).trim().isEmpty())
            {
                return null;
            }
            List<Map<String, Object>> opts = new ArrayList<>();
            Object options = m.get("options");
            if (options instanceof List)
            {
                for (Object op : (List<?>) options)
                {
                    if (op instanceof Map)
                    {
                        Map<?, ?> om = (Map<?, ?>) op;
                        String label = str(om.get("label"));
                        String value = str(om.get("value"));
                        if (label != null && value != null)
                        {
                            opts.add(mapOf("label", label, "value", value));
                        }
                    }
                }
            }
            Map<String, Object> result = new HashMap<>(2);
            result.put("content", String.valueOf(content));
            result.put("options", opts);
            return result;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void writeError(SseEmitter emitter, String msg)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error")
                    .data(mapOf("type", "error", "status", "failed", "content", msg)));
            emitter.complete();
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
        }
    }

    /** 简化的不可变 Map 构造（值不允许为 null） */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object... kv)
    {
        Map<String, Object> m = new HashMap<>(kv.length / 2 + 1);
        for (int i = 0; i + 1 < kv.length; i += 2)
        {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    /** 安全转字符串（null → null），与 AiProtoController 同名工具保持一致 */
    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o);
    }

    /* ============================ 历史版本（复用 ai_version_record，bizType=CLARIFY） ============================ */

    /** 澄清产物类型常量，与 ai_version_record.biz_type 约定一致 */
    private static final String CLARIFY_BIZ_TYPE = "CLARIFY";

    /** 由快照派生文件清单（供历史抽屉展示与查看） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildClarifyFiles(String snapshotJson)
    {
        Map<String, Object> snapshot = parseJsonMap(snapshotJson);
        List<Map<String, Object>> files = new ArrayList<>(2);
        int totalSize = snapshot == null ? 0 : JSON.toJSONString(snapshot).length();
        files.add(fileItem("澄清结论.json", "json", totalSize, "conclusion"));
        Object conv = snapshot == null ? null : snapshot.get("conversation");
        int convSize = conv == null ? 0 : JSON.toJSONString(conv).length();
        files.add(fileItem("澄清对话记录.json", "json", convSize, "conversation"));
        Map<String, Object> m = new HashMap<>(2);
        m.put("files", files);
        return m;
    }

    private Map<String, Object> fileItem(String name, String type, int size, String key)
    {
        Map<String, Object> f = new HashMap<>(4);
        f.put("name", name);
        f.put("type", type);
        f.put("size", humanSize(size));
        f.put("key", key);
        return f;
    }

    /** 把字节数格式化为可读大小（B/KB/MB） */
    private static String humanSize(int bytes)
    {
        if (bytes < 1024)
        {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024)
        {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        try
        {
            Object o = JSON.parse(s);
            if (o instanceof Map)
            {
                return (Map<String, Object>) o;
            }
        }
        catch (Exception e)
        {
            // 解析失败返回 null，前端按空处理
        }
        return null;
    }

    private Long parseProjectId(Object obj)
    {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try
        {
            return Long.valueOf(String.valueOf(obj).trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseConversation(String json)
    {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try
        {
            return (List<Object>) JSON.parse(json);
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }
    }



    /**
     * 获取可用模型列表（读取 ai_model_config 启用项；未配置时返回空列表，由前端提示“没有可用模型”）
     */
    @GetMapping("/models")
    public AjaxResult models()
    {
        List<AiModelConfig> configs = null;
        try
        {
            AiModelConfig query = new AiModelConfig();
            query.setIsEnabled("0");
            configs = modelConfigService.selectAiModelConfigList(query);
        }
        catch (Exception e)
        {
            configs = null;
        }
        if (configs == null || configs.isEmpty())
        {
            return success(new ArrayList<Map<String, Object>>(0));
        }
        List<Map<String, Object>> list = new ArrayList<>(configs.size());
        for (int i = 0; i < configs.size(); i++)
        {
            AiModelConfig c = configs.get(i);
            Map<String, Object> m = new HashMap<>(6);
            m.put("id", c.getModelCode());
            m.put("name", c.getModelName());
            m.put("provider", c.getProvider());
            m.put("description", c.getProvider() != null ? c.getProvider() + " 提供" : "AI 模型");
            m.put("enabled", true);
            m.put("isDefault", i == 0);
            list.add(m);
        }
        return success(list);
    }

    /**
     * 获取系统配置（最大对比模型数）
     */
    @GetMapping("/models/config")
    public AjaxResult modelConfig()
    {
        Map<String, Object> config = new HashMap<>(2);
        config.put("maxCompareCount", 4);
        return success(config);
    }

    /**
     * 发送消息（流式 SSE）：保存用户消息，真实并发调用每个选中模型，
     * 通过 SseEmitter 逐 token 推送模型回答，避免前端因整体响应过慢而超时。
     *
     * 事件约定（data 为 JSON，含 type 字段）：
     *  - {type:"start", models:[{modelId,modelName}]}        开始时下发模型清单
     *  - {type:"token", modelId, delta}                      每个文本片段
     *  - {type:"done",  modelId, latency, status}            单模型结束
     *  - {type:"error", modelId, status:"failed", content}   单模型失败（content 为兜底文案）
     *  - {type:"done-all"}                                   全部完成（随后 complete 连接）
     */
    @PostMapping("/send")
    public SseEmitter send(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        // 关闭任何中间代理(Nginx/Vite proxy)对 SSE 的缓冲，确保 token 即时下发
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        log.info("[clarify] send 收到请求 body.projectId={}", body == null ? null : body.get("projectId"));
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onTimeout(() -> {
            try
            {
                emitter.send(SseEmitter.event().name("error")
                        .data(mapOf("type", "error", "status", "failed", "content", "（请求超时）")));
            }
            catch (IOException ignored) { }
            emitter.complete();
        });
        emitter.onError(e -> emitter.completeWithError(e));

        Long projectId = parseProjectId(body.get("projectId"));
        if (projectId == null)
        {
            writeError(emitter, "项目ID不能为空");
            return emitter;
        }
        String message = body.get("message") == null ? "" : String.valueOf(body.get("message"));
        if (message.length() > 8000)
        {
            writeError(emitter, "消息内容长度不能超过 8000 字符");
            return emitter;
        }
        Object selectedModelsObj = body.get("selectedModels");

        AiClarifySession session = aiClarifySessionService.getOrCreateSession(projectId);
        List<Object> conversation = parseConversation(session.getConversation());

        // 追加用户消息
        Map<String, Object> userMsg = new HashMap<>(4);
        userMsg.put("id", UUID.randomUUID().toString());
        userMsg.put("type", "user_answer");
        userMsg.put("content", message);
        userMsg.put("timestamp", new Date().toString());
        // 方案C：记录消息作者（当前登录用户），供多人协作时追溯“谁说的”
        userMsg.put("author", SecurityUtils.getUsername());
        conversation.add(userMsg);

        // 解析选中模型（按 modelId 去重，避免前端重复选择或后端返回重复模型）
        List<String> modelIds = new ArrayList<>();
        List<String> modelNames = new ArrayList<>();
        if (selectedModelsObj instanceof List)
        {
            for (Object o : (List<?>) selectedModelsObj)
            {
                if (o instanceof Map)
                {
                    Object id = ((Map<?, ?>) o).get("id");
                    Object nm = ((Map<?, ?>) o).get("name");
                    if (id != null && !modelIds.contains(String.valueOf(id)))
                    {
                        modelIds.add(String.valueOf(id));
                        modelNames.add(nm == null ? String.valueOf(id) : String.valueOf(nm));
                    }
                }
                else if (o != null && !modelIds.contains(String.valueOf(o)))
                {
                    modelIds.add(String.valueOf(o));
                    modelNames.add(String.valueOf(o));
                }
            }
        }
        if (modelIds.isEmpty())
        {
            modelIds.add("deepseek");
            modelNames.add("DeepSeek");
        }

        // 先落库「用户消息 + 占位 AI 多模型消息」，刷新页面也能恢复（含各模型加载中占位）
        Map<String, Object> aiMsg = new HashMap<>(4);
        aiMsg.put("id", UUID.randomUUID().toString());
        aiMsg.put("type", "ai_multi_response");
        List<Map<String, Object>> placeholderResponses = new ArrayList<>(modelIds.size());
        Map<String, String> respIdByModel = new HashMap<>(modelIds.size());
        for (int i = 0; i < modelIds.size(); i++)
        {
            String mid = modelIds.get(i);
            String mname = i < modelNames.size() ? modelNames.get(i) : mid;
            String respId = UUID.randomUUID().toString();
            respIdByModel.put(mid, respId);
            Map<String, Object> r = new HashMap<>(6);
            r.put("respId", respId);
            r.put("modelId", mid);
            r.put("modelName", mname);
            r.put("content", "");
            r.put("status", "loading");
            r.put("latency", 0);
            placeholderResponses.add(r);
        }
        aiMsg.put("modelResponses", placeholderResponses);
        aiMsg.put("timestamp", new Date().toString());
        // 方案C：AI 多模型回答统一标记为“AI”
        aiMsg.put("author", "AI");
        conversation.add(aiMsg);
        session.setConversation(JSON.toJSONString(conversation));
        session.setStatus("0");
        aiClarifySessionService.saveSession(session);

        // 提示词工程化：从 ai_prompt_template 读取澄清默认模板并渲染变量（DB 缺失时回退内置常量，零回归）
        Map<String, Object> clarifyVars = new HashMap<>(2);
        clarifyVars.put("message", message);
        String kbContext = knowledgeRetrievalService.retrieveAsContext(projectId, "CLARIFY", message, modelIds.get(0));
        clarifyVars.put("kbContext", kbContext);
        RenderedPrompt clarifyRendered = promptTemplateService.render("CLARIFY", modelIds.get(0), clarifyVars);
        String systemPrompt = clarifyRendered.getSystemPrompt();

        Map<String, StringBuilder> contents = new ConcurrentHashMap<>();
        Map<String, Long> latencies = new ConcurrentHashMap<>();
        Map<String, String> statuses = new ConcurrentHashMap<>();

        // 下发模型清单，便于前端预渲染空卡片
        try
        {
            List<Map<String, Object>> modelMeta = new ArrayList<>(modelIds.size());
            for (int i = 0; i < modelIds.size(); i++)
            {
                Map<String, Object> m = new HashMap<>(2);
                m.put("modelId", modelIds.get(i));
                m.put("modelName", i < modelNames.size() ? modelNames.get(i) : modelIds.get(i));
                modelMeta.add(m);
            }
            emitter.send(SseEmitter.event().name("start")
                    .data(mapOf("type", "start", "models", modelMeta)));
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
            return emitter;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(modelIds.size());
        for (int i = 0; i < modelIds.size(); i++)
        {
            final String mid = modelIds.get(i);
            final String mname = i < modelNames.size() ? modelNames.get(i) : mid;
            futures.add(CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                StringBuilder full = new StringBuilder();
                try
                {
                    aiModelClient.chatStream(mid, systemPrompt, clarifyRendered.getUserPrompt(), delta -> {
                        full.append(delta);
                        try
                        {
                            emitter.send(SseEmitter.event().name("token")
                                    .data(mapOf("type", "token", "modelId", mid, "delta", delta)));
                        }
                        catch (IOException ignored)
                        {
                            // 前端断开：停止推送但继续收集，确保最终能落库
                        }
                    });
                    latencies.put(mid, System.currentTimeMillis() - start);
                    statuses.put(mid, "completed");
                }
                catch (Exception e)
                {
                    String err = "（调用模型「" + mid + "」失败：" + e.getMessage() + "）";
                    full.append(err);
                    statuses.put(mid, "failed");
                    latencies.put(mid, System.currentTimeMillis() - start);
                    try
                    {
                        emitter.send(SseEmitter.event().name("error")
                                .data(mapOf("type", "error", "modelId", mid, "status", "failed", "content", err)));
                    }
                    catch (IOException ignored) { }
                }
                finally
                {
                    contents.put(mid, full);
                }
            }, STREAM_POOL));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, ex) -> {
            try
            {
                // 组装每个模型的最终回答（aiMsg 已在同一 conversation 引用中，直接更新其字段）
                // 复用占位时分配的 respId，确保刷新后前端 v-for key 稳定且唯一
                List<Map<String, Object>> modelResponses = new ArrayList<>(modelIds.size());
                for (int i = 0; i < modelIds.size(); i++)
                {
                    String mid = modelIds.get(i);
                    StringBuilder c = contents.get(mid);
                    Map<String, Object> r = new HashMap<>(6);
                    r.put("respId", respIdByModel.getOrDefault(mid, UUID.randomUUID().toString()));
                    r.put("modelId", mid);
                    r.put("modelName", i < modelNames.size() ? modelNames.get(i) : mid);
                    r.put("content", c == null ? "" : c.toString());
                    r.put("status", statuses.getOrDefault(mid, "failed"));
                    r.put("latency", latencies.getOrDefault(mid, 0L).intValue());
                    modelResponses.add(r);
                }
                aiMsg.put("modelResponses", modelResponses);
                // 落库完整对话（含最终模型回答）
                session.setConversation(JSON.toJSONString(conversation));
                session.setStatus("0");
                aiClarifySessionService.saveSession(session);

                // 逐个模型下发最终状态，便于前端立即把卡片从「思考中」切换到完成/失败
                for (Map<String, Object> r : modelResponses)
                {
                    String mid = String.valueOf(r.get("modelId"));
                    String st = String.valueOf(r.get("status"));
                    int latency = r.get("latency") instanceof Number ? ((Number) r.get("latency")).intValue() : 0;
                    emitter.send(SseEmitter.event().name("done")
                            .data(mapOf("type", "done", "modelId", mid, "status", st, "latency", latency)));
                }
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                emitter.complete();
            }
            catch (Exception e)
            {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 动态生成下一题：依据「需求基线」+「澄清对话历史」，由 AI 生成下一道针对性澄清问题。
     * 返回 { content, options }；若模型未配置或解析失败，回退到通用兜底问题，保证前端不报错。
     */
    @PostMapping("/nextQuestion")
    public AjaxResult nextQuestion(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);

        // 1) 需求基线（作为提问的上下文）
        String baselineText = "";
        try
        {
            AiReqBaseline b = reqBaselineService.selectAiReqBaselineByProjectId(projectId);
            if (b != null && b.getContent() != null)
            {
                baselineText = b.getContent();
            }
        }
        catch (Exception e)
        {
            baselineText = "";
        }

        // 2) 澄清对话历史（只抽取与提问相关的消息类型，避免上下文过长）
        StringBuilder hist = new StringBuilder();
        try
        {
            AiClarifySession session = aiClarifySessionService.getOrCreateSession(projectId);
            List<Object> conversation = parseConversation(session.getConversation());
            for (Object o : conversation)
            {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                String type = str(m.get("type"));
                if ("ai_question".equals(type))
                {
                    hist.append("【AI提问】").append(str(m.get("content"))).append("\n");
                }
                else if ("user_answer".equals(type) || "user_free".equals(type)
                        || "user_text".equals(type) || "user".equals(type))
                {
                    hist.append("【用户回答】").append(str(m.get("content"))).append("\n");
                }
                else if ("ai_multi_response".equals(type))
                {
                    Object mrs = m.get("modelResponses");
                    if (mrs instanceof List)
                    {
                        for (Object r : (List<?>) mrs)
                        {
                            if (r instanceof Map)
                            {
                                String c = str(((Map<?, ?>) r).get("content"));
                                if (c != null && !c.isEmpty())
                                {
                                    hist.append("【AI多模型回答片段】")
                                            .append(c.length() > 200 ? c.substring(0, 200) : c).append("\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            // 历史解析失败不影响出题，使用空历史
        }

        // 3) 选一个启用模型
        String modelCode = "deepseek";
        try
        {
            AiModelConfig query = new AiModelConfig();
            query.setIsEnabled("0");
            List<AiModelConfig> configs = modelConfigService.selectAiModelConfigList(query);
            if (configs != null && !configs.isEmpty())
            {
                modelCode = configs.get(0).getModelCode();
            }
        }
        catch (Exception e)
        {
            // 配置查询异常，使用默认 code；chat 内部会在未配置时返回兜底文案
        }

        // 4) 提示词：要求 AI 输出结构化 JSON（下一道澄清问题 + 选项）
        String systemPrompt = "你是资深产品需求分析师。基于已有的「需求基线」与「澄清对话历史」，"
                + "为当前项目生成【下一个】最值得追问的澄清问题。问题应聚焦用户尚未明确的关键点"
                + "（如核心用户角色、关键业务流程、数据规模、性能/安全约束、系统集成、异常处理、合规要求等），"
                + "一次只问一个问题，且不要重复已问过的问题。请用专业但易懂的中文表述，关键问题用 **加粗**。";

        String userPrompt = "【需求基线】\n" + baselineText + "\n\n"
                + "【已发生的澄清对话】\n" + (hist.length() > 0 ? hist.toString() : "（暂无）") + "\n\n"
                + "请输出下一道澄清问题。严格仅返回一个 JSON 对象（不要包含 ``` 代码块标记、不要任何额外文字），格式如下：\n"
                + "{\"content\":\"问题正文（关键问题用**加粗**），\"options\":[{\"label\":\"选项文字\",\"value\":\"选项值\"}]}\n"
                + "若认为需求信息已足够清晰、无需继续追问，请返回：\n"
                + "{\"content\":\"需求信息已较为完整，您可以点击「进入下一阶段」结束澄清。\","
                + "\"options\":[{\"label\":\"没有补充，进入下一步\",\"value\":\"done\"},{\"label\":\"我还有补充\",\"value\":\"supplement\"}]}";

        String raw = aiModelClient.chat(modelCode, systemPrompt, userPrompt);

        // 5) 解析模型返回的 JSON；解析失败则回退通用兜底问题，保证前端不报错
        Map<String, Object> question = parseQuestionJson(raw);
        if (question == null)
        {
            question = fallbackQuestion();
        }
        return success(question);
    }
}
