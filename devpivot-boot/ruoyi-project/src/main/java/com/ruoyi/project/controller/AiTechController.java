package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.AiPrdDoc;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.domain.AiTechDoc;
import com.ruoyi.project.service.IAiPrdDocService;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.project.service.IAiTechDocService;

/**
 * 门户·技术方案 · AI 接口（/ai/tech）
 * 仅承载 AI/流式能力：可用模型、流式生成技术方案。
 * 数据读写见同包 TechController（/system/tech）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/ai/tech")
public class AiTechController extends BaseController
{

    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    /** 多模型对比最大模型数（前端 models 接口 maxCompareCount 与此保持一致） */
    private static final int MAX_COMPARE_MODELS = 3;

    @Autowired
    private IAiTechDocService techDocService;

    @Autowired
    private IAiProjectService projectService;

    @Autowired
    private IAiPrdDocService prdDocService;

    /** 流式推送任务线程池 */
    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(AiTechController.class);

    /** 回源读取项目最新 PRD 文档内容，作为生成上下文；无 PRD 时返回提示 */
    private String buildUpstream(Long projectId)
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
            log.warn("[tech-generate] 读取 PRD 上下文失败", e);
        }
        return "（暂无上游 PRD 文档）";
    }

    /** 取第一个启用模型的 modelCode，无配置时回退 "deepseek" */
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

    /** 简化的不可变 Map 构造 */
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
        data.put("maxCompareCount", MAX_COMPARE_MODELS);
        return success(data);
    }

    /**
     * 生成技术方案（流式 SSE）
     *
     * 事件约定（data 为 JSON，含 modelId 便于前端多路复用回调）：
     *  - {type:"start", modelId}           开始时下发
     *  - {type:"token", delta, modelId}    每个文本片段
     *  - {type:"done", modelId}            生成完成
     *  - {type:"error", content, modelId}  失败（content 为说明）
     */
    @PostMapping("/generate")
    public SseEmitter generate(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        log.info("[tech-generate] 收到请求 projectId={}", body == null ? null : body.get("projectId"));

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

        // 多模型：前端传入 models 数组（各模型并行流式）；空则默认模型
        List<Object> raw = body.get("models") instanceof List ? (List<Object>) body.get("models") : null;
        List<String> modelCodes = new ArrayList<>();
        if (raw != null)
        {
            for (Object o : raw)
            {
                String code = String.valueOf(o);
                if (code != null && !code.isBlank() && !modelCodes.contains(code))
                {
                    modelCodes.add(code);
                }
            }
        }
        if (modelCodes.isEmpty())
        {
            modelCodes.add(defaultModelCode());
        }
        if (modelCodes.size() > MAX_COMPARE_MODELS)
        {
            writeError(emitter, "对比模型数不能超过 " + MAX_COMPARE_MODELS + " 个");
            return emitter;
        }

        String projectName = str(body.get("projectName"), "产品");
        String industryType = str(body.get("industryType"), "通用行业");
        String targetUser = str(body.get("targetUser"), "目标用户");
        String techStack = str(body.get("techStack"), "Java + Vue 3");
        String extraReq = str(body.get("extraReq"), "");

        // 入参防护：自由文本长度上限，避免超长内容撑爆存储或模型上下文
        if (projectName.length() > 200 || industryType.length() > 200 || targetUser.length() > 200 || techStack.length() > 200)
        {
            writeError(emitter, "项目名称/行业类型/目标用户/技术栈长度不能超过 200 字符");
            return emitter;
        }
        if (extraReq.length() > 4000)
        {
            writeError(emitter, "补充要求长度不能超过 4000 字符");
            return emitter;
        }

        // 上游上下文：优先回源 PRD 文档，使生成真正参考上一阶段产物
        String upstream = buildUpstream(projectId);
        String extraBlock = (extraReq != null && !extraReq.trim().isEmpty())
                ? "【补充要求】\n" + extraReq + "\n\n" : "";

        // 各模型共享同一 SseEmitter，事件带各自 modelId（前端多路复用）；全部完成才 complete
        final CountDownLatch latch = new CountDownLatch(modelCodes.size());
        for (String modelCode : modelCodes)
        {
            final String usedModel = modelCode;
            STREAM_POOL.submit(() -> {
                try
                {
                    streamOneModel(emitter, projectId, usedModel,
                            projectName, industryType, targetUser, techStack, extraBlock, upstream);
                }
                finally
                {
                    latch.countDown();
                }
            });
        }
        STREAM_POOL.submit(() -> {
            try
            {
                latch.await();
            }
            catch (InterruptedException ignored) { }
            emitter.complete();
        });
        return emitter;
    }

    /** 单模型流式：渲染提示词（含该模型的 KB 检索与模板）→ 发 start/token/done 事件，事件带 modelId 供前端多路复用 */
    private void streamOneModel(SseEmitter emitter, Long projectId, String usedModel,
                                String projectName, String industryType, String targetUser,
                                String techStack, String extraBlock, String upstream)
    {
        try
        {
            Map<String, Object> techVars = new HashMap<>(8);
            techVars.put("projectName", projectName);
            techVars.put("industryType", industryType);
            techVars.put("targetUser", targetUser);
            techVars.put("techStack", techStack);
            techVars.put("upstream", upstream);
            techVars.put("extraBlock", extraBlock);
            String kbContext = knowledgeRetrievalService.retrieveAsContext(projectId, "TECH", upstream, usedModel);
            techVars.put("kbContext", kbContext);
            RenderedPrompt techPrompt = promptTemplateService.render("TECH", usedModel, techVars);
            String systemPrompt = techPrompt.getSystemPrompt();
            String userPrompt = techPrompt.getUserPrompt();

            emitter.send(SseEmitter.event().name("start").data(mapOf("type", "start", "modelId", usedModel)));
            aiModelClient.chatStream(usedModel, systemPrompt, userPrompt, delta -> {
                try
                {
                    emitter.send(SseEmitter.event().name("token")
                            .data(mapOf("type", "token", "delta", delta, "modelId", usedModel)));
                }
                catch (IOException ignored)
                {
                    // 前端断开：停止推送
                }
            });
            emitter.send(SseEmitter.event().name("done").data(mapOf("type", "done", "modelId", usedModel)));
        }
        catch (Exception e)
        {
            log.error("[tech-generate] 模型 {} 生成异常", usedModel, e);
            try
            {
                emitter.send(SseEmitter.event().name("error")
                        .data(mapOf("type", "error", "content", e.getMessage(), "modelId", usedModel)));
            }
            catch (IOException ignored) { }
        }
    }
}
