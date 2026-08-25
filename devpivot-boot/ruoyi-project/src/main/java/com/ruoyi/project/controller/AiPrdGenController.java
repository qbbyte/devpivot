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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.utils.ParamValidator;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.domain.AiPrdDoc;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiClarifySessionService;
import com.ruoyi.project.service.IAiPrdDocService;
import com.ruoyi.project.service.IAiProjectService;

/**
 * 门户·PRD 生成 · AI 接口（/ai/doc）
 * 仅承载 AI/流式能力：可用模型、流式生成 PRD。
 * 数据读写见同包 PrdController（/system/prd）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/ai/doc")
public class AiPrdGenController extends BaseController
{

    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiClarifySessionService clarifySessionService;

    @Autowired
    private IAiPrdDocService prdDocService;

    @Autowired
    private IAiProjectService projectService;

    /** 流式推送任务线程池 */
    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(AiPrdGenController.class);

    // 用户提示词构建已迁移到 ai_prompt_template.user_template，由 PromptTemplateService.render 统一处理（见 generate 方法）。

    /** 回源澄清会话，提取干净的需求上下文文本 */
    private String buildClarifyContext(Long projectId)
    {
        try
        {
            AiClarifySession s = clarifySessionService.getOrCreateSession(projectId);
            StringBuilder sb = new StringBuilder();
            if (s.getConclusion() != null && !s.getConclusion().isEmpty())
            {
                sb.append("最终结论：").append(s.getConclusion()).append("\n");
            }
            appendArray(sb, "采纳结论", s.getAdopted());
            appendArray(sb, "保留要点", s.getRetained());
            return sb.length() == 0 ? "（无澄清结论）" : sb.toString();
        }
        catch (Exception e)
        {
            return "（读取澄清结论失败）";
        }
    }

    @SuppressWarnings("unchecked")
    private void appendArray(StringBuilder sb, String label, String json)
    {
        if (json == null || json.isEmpty()) return;
        try
        {
            List<Object> list = (List<Object>) JSON.parse(json);
            if (list == null || list.isEmpty()) return;
            sb.append(label).append("：\n");
            for (Object o : list)
            {
                if (o instanceof Map)
                {
                    Map<String, Object> m = (Map<String, Object>) o;
                    Object content = m.get("content");
                    if (content != null) sb.append("- ").append(content).append("\n");
                }
                else if (o != null)
                {
                    sb.append("- ").append(o).append("\n");
                }
            }
        }
        catch (Exception ignored) { }
    }

    /** 取第一个启用模型的 modelCode（model_code），无配置时回退 "deepseek" */
    private String defaultModelCode()
    {
        try
        {
            AiModelConfig query = new AiModelConfig();
            query.setIsEnabled("0");
            List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(query);
            if (list != null && !list.isEmpty())
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

    /** 简化的不可变 Map 构造（值不允许为 null） */
    private static Map<String, Object> mapOf(Object... kv)
    {
        Map<String, Object> m = new HashMap<>(kv.length / 2 + 1);
        for (int i = 0; i + 1 < kv.length; i += 2)
        {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }



    /**
     * 可用模型列表：返回 ai_model_config 中「启用」的模型，映射为前端所需的
     * { modelId, modelName } 结构。本阶段为单模型，maxCompareCount 固定为 1。
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
     * 生成 PRD（流式 SSE）
     *
     * 事件约定（data 为 JSON）：
     *  - {type:"start"}                 开始时下发
     *  - {type:"token", delta}          每个文本片段
     *  - {type:"done"}                  生成完成
     *  - {type:"error", content}        失败（content 为说明）
     */
    @PostMapping("/generate")
    public SseEmitter generate(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        log.info("[prd-generate] 收到请求 projectId={}", body == null ? null : body.get("projectId"));

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

        String rawModel = body.get("model") == null ? null : String.valueOf(body.get("model")).trim();
        final String model = (rawModel == null || rawModel.isEmpty()) ? defaultModelCode() : rawModel;
        String projectName = str(body.get("projectName"), "产品");
        String industryType = str(body.get("industryType"), "通用行业");
        String targetUser = str(body.get("targetUser"), "目标用户");
        String templateType = str(body.get("templateType"), "STANDARD");
        String mode = "multi".equals(body.get("mode")) ? "多模型协同" : "单模型";

        // 入参防护：自由文本长度上限，避免超长内容撑爆存储或模型上下文
        if (projectName.length() > 200 || industryType.length() > 200 || targetUser.length() > 200)
        {
            writeError(emitter, "项目名称/行业类型/目标用户长度不能超过 200 字符");
            return emitter;
        }
        if (templateType.length() > 50)
        {
            writeError(emitter, "模板类型长度不能超过 50 字符");
            return emitter;
        }

        // 澄清上下文：优先使用前端已清洗的 clarifySummary，否则回源澄清会话
        String clarifyContext = body.get("clarifySummary") == null ? null
                : String.valueOf(body.get("clarifySummary"));
        if (clarifyContext == null || clarifyContext.trim().isEmpty())
        {
            clarifyContext = buildClarifyContext(projectId);
        }

        // 提示词工程化：从 ai_prompt_template 读取 PRD 默认模板并渲染变量（DB 缺失时回退内置常量，零回归）
        String tpl = "STANDARD".equals(templateType) ? "标准" : "DETAIL".equals(templateType) ? "详细" : "精简";
        Map<String, Object> prdVars = new HashMap<>(8);
        prdVars.put("templateLabel", tpl);
        prdVars.put("mode", mode);
        prdVars.put("projectName", projectName);
        prdVars.put("industryType", industryType);
        prdVars.put("targetUser", targetUser);
        prdVars.put("clarifyContext", clarifyContext);
        String kbContext = knowledgeRetrievalService.retrieveAsContext(projectId, "PRD", clarifyContext, model);
        prdVars.put("kbContext", kbContext);
        RenderedPrompt prdRendered = promptTemplateService.render("PRD", model, prdVars);
        String systemPrompt = prdRendered.getSystemPrompt();
        String userPrompt = prdRendered.getUserPrompt();

        try
        {
            emitter.send(SseEmitter.event().name("start").data(mapOf("type", "start")));
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
            return emitter;
        }

        STREAM_POOL.submit(() -> {
            try
            {
                aiModelClient.chatStream(model, systemPrompt, userPrompt, delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token", "delta", delta)));
                    }
                    catch (IOException ignored)
                    {
                        // 前端断开：停止推送
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(mapOf("type", "done")));
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("[prd-generate] 生成异常", e);
                try
                {
                    emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", e.getMessage())));
                }
                catch (IOException ignored) { }
                emitter.complete();
            }
        });
        return emitter;
    }
}
