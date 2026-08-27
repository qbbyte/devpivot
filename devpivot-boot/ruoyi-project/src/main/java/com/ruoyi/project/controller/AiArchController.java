package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.AiPrdDoc;
import com.ruoyi.project.domain.AiProtoPage;
import com.ruoyi.project.service.IAiPrdDocService;
import com.ruoyi.project.service.IAiProtoPageService;

/**
 * 系统架构设计 · AI 接口（/ai/arch）
 * 仅承载 AI/流式能力：可用模型、流式生成系统架构设计、AI 对话建议。
 * 数据读写见同包 ArchController（/portal/arch）。
 * 上游上下文 = PRD 文档 + 原型页面清单，KB 检索 stage=ARCH。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@RestController
@Validated
@RequestMapping("/ai/arch")
public class AiArchController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AiArchController.class);

    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiPrdDocService prdDocService;

    @Autowired
    private IAiProtoPageService protoPageService;

    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    /* ============================ 工具 ============================ */

    /** 回源读取项目最新 PRD 文档内容（截断 4000 字） */
    private String buildPrdContext(Long projectId)
    {
        try
        {
            AiPrdDoc q = new AiPrdDoc();
            q.setProjectId(projectId);
            List<AiPrdDoc> list = prdDocService.selectAiPrdDocList(q);
            if (list != null && !list.isEmpty())
            {
                String c = list.get(0).getContent();
                if (c != null && !c.trim().isEmpty())
                {
                    if (c.length() > 4000)
                    {
                        c = c.substring(0, 4000) + "\n…（内容已截断，仅取前 4000 字作为上下文）";
                    }
                    return "【上游 PRD 文档摘要】\n" + c;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("[arch-generate] 读取 PRD 上下文失败", e);
        }
        return "（暂无上游 PRD 文档）";
    }

    /** 回源读取原型页面清单（页面名 + 描述），作为架构设计的界面上下文 */
    private String buildProtoContext(Long projectId)
    {
        try
        {
            List<AiProtoPage> pages = protoPageService.selectAiProtoPageByProjectId(projectId);
            if (pages != null && !pages.isEmpty())
            {
                StringBuilder sb = new StringBuilder("【上游原型页面清单】\n");
                for (AiProtoPage p : pages)
                {
                    sb.append("- ").append(p.getPageName())
                      .append(p.getPageDesc() == null || p.getPageDesc().isEmpty() ? "" : "：" + p.getPageDesc())
                      .append("（").append(p.getDeviceType() == null ? "WEB" : p.getDeviceType()).append("）\n");
                }
                return sb.toString();
            }
        }
        catch (Exception e)
        {
            log.warn("[arch-generate] 读取原型页面清单失败", e);
        }
        return "";
    }

    private String defaultModelCode()
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
                    if (c.getModelCode() != null && !c.getModelCode().isEmpty())
                    {
                        return c.getModelCode();
                    }
                }
            }
        }
        catch (Exception e) { }
        return "deepseek";
    }

    private void writeError(SseEmitter emitter, String msg)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", msg)));
            emitter.complete();
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
        }
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

    private static String str(Object obj, String def)
    {
        return obj == null ? def : String.valueOf(obj);
    }

    private static Map<String, Object> mapOf(Object... kv)
    {
        Map<String, Object> m = new HashMap<>(kv.length / 2 + 1);
        for (int i = 0; i + 1 < kv.length; i += 2)
        {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    /* ============================ 端点 ============================ */

    /**
     * 可用模型列表（单模型模式，maxCompareCount=1）
     */
    @GetMapping("/models")
    public AjaxResult models()
    {
        AiModelConfig query = new AiModelConfig();
        query.setIsEnabled("0");
        List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(query);
        List<Map<String, Object>> models = new ArrayList<>();
        if (list != null)
        {
            for (AiModelConfig c : list)
            {
                if (c.getModelCode() == null || c.getModelCode().isEmpty())
                {
                    continue;
                }
                Map<String, Object> m = new HashMap<>(2);
                m.put("modelId", c.getModelCode());
                m.put("modelName", c.getModelName() == null ? c.getModelCode() : c.getModelName());
                models.add(m);
            }
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("models", models);
        data.put("maxCompareCount", 1);
        return success(data);
    }

    /**
     * 生成系统架构设计（流式 SSE）
     * 事件约定（data 为 JSON）：
     *  - {type:"start", modelId}        开始时下发
     *  - {type:"token", delta, modelId} 每个文本片段
     *  - {type:"done", modelId}         生成完成
     *  - {type:"error", content}        失败
     */
    @PostMapping("/generate")
    public SseEmitter generate(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        log.info("[arch-generate] 收到请求 projectId={}", body == null ? null : body.get("projectId"));

        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onTimeout(() -> {
            try
            {
                emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", "（请求超时）")));
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

        String modelCode = str(body.get("model"), defaultModelCode());
        String projectName = str(body.get("projectName"), "产品");
        String extraReq = str(body.get("extraReq"), "");
        if (projectName.length() > 200)
        {
            writeError(emitter, "项目名称长度不能超过 200 字符");
            return emitter;
        }
        if (extraReq.length() > 4000)
        {
            writeError(emitter, "补充要求长度不能超过 4000 字符");
            return emitter;
        }
        ParamValidator.projectId(projectId);

        String prdContext = buildPrdContext(projectId);
        String protoContext = buildProtoContext(projectId);
        String extraBlock = (extraReq != null && !extraReq.trim().isEmpty())
                ? "【补充要求】\n" + extraReq + "\n\n" : "";

        STREAM_POOL.submit(() -> {
            try
            {
                Map<String, Object> archVars = new HashMap<>(6);
                archVars.put("projectName", projectName);
                archVars.put("prdContext", prdContext);
                archVars.put("protoContext", protoContext);
                archVars.put("extraBlock", extraBlock);
                String kbContext = knowledgeRetrievalService.retrieveAsContext(projectId, "ARCH", prdContext + protoContext, modelCode);
                archVars.put("kbContext", kbContext);
                RenderedPrompt archPrompt = promptTemplateService.renderByCode("ARCH_GEN", modelCode, archVars);
                String systemPrompt = archPrompt.getSystemPrompt();
                String userPrompt = archPrompt.getUserPrompt();

                emitter.send(SseEmitter.event().name("start").data(mapOf("type", "start", "modelId", modelCode)));
                aiModelClient.chatStream(modelCode, systemPrompt, userPrompt, delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token")
                                .data(mapOf("type", "token", "delta", delta, "modelId", modelCode)));
                    }
                    catch (IOException ignored)
                    {
                        // 前端断开：停止推送
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(mapOf("type", "done", "modelId", modelCode)));
            }
            catch (Exception e)
            {
                log.error("[arch-generate] 生成异常 projectId={}", projectId, e);
                try
                {
                    emitter.send(SseEmitter.event().name("error")
                            .data(mapOf("type", "error", "content", e.getMessage())));
                }
                catch (IOException ignored) { }
            }
            finally
            {
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * AI 对话（流式 SSE）：针对架构设计的提问给建议；无模型或失败回退本地规则建议。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onError(e -> emitter.completeWithError(e));
        emitter.onTimeout(() -> emitter.complete());

        String message = str(body.get("message"), "");
        String modelCode = str(body.get("model"), defaultModelCode());

        if (message.isBlank())
        {
            writeError(emitter, "消息不能为空");
            return emitter;
        }

        STREAM_POOL.submit(() -> {
            try
            {
                Map<String, Object> chatVars = new HashMap<>(1);
                chatVars.put("message", message);
                RenderedPrompt chatPrompt = promptTemplateService.renderByCode("ARCH_CHAT", modelCode, chatVars);
                aiModelClient.chatStream(modelCode, chatPrompt.getSystemPrompt(), chatPrompt.getUserPrompt(), delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token")
                                .data(mapOf("type", "token", "delta", delta)));
                    }
                    catch (IOException ignored) { }
                });
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
            }
            catch (Exception e)
            {
                log.warn("[arch-chat] 对话失败，回退本地建议", e);
                try
                {
                    emitter.send(SseEmitter.event().name("token")
                            .data(mapOf("type", "token", "delta", buildLocalReply(message))));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                }
                catch (IOException ignored) { }
            }
            finally
            {
                emitter.complete();
            }
        });
        return emitter;
    }

    private String buildLocalReply(String message)
    {
        String msg = message == null ? "" : message.trim();
        if (msg.contains("模块") || msg.contains("拆分"))
        {
            return "架构建议：按业务域拆分模块（如用户/订单/商品），模块间通过接口交互；核心链路模块优先落地，边缘能力后置。";
        }
        if (msg.contains("接口") || msg.contains("API"))
        {
            return "接口契约建议：RESTful 资源化设计，统一 /api/{domain} 前缀；写操作校验 + 幂等，读操作支持分页与过滤。";
        }
        if (msg.contains("部署") || msg.contains("架构图"))
        {
            return "部署架构建议：前端静态托管 + 后端无状态多实例 + 数据库主从；图示建议用 Mermaid 分层图表达网关/应用/存储三层。";
        }
        return "可以把想问的架构问题描述给我，例如「如何拆分模块」「核心流程怎么设计」「接口契约怎么定」。";
    }
}
