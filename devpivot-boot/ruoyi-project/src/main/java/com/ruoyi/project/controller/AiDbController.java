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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.AiDbDoc;
import com.ruoyi.project.domain.AiPrdDoc;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.domain.AiTechDoc;
import com.ruoyi.project.service.IAiDbDocService;
import com.ruoyi.project.service.IAiPrdDocService;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.project.service.IAiTechDocService;

/**
 * 门户·数据库设计接口（/ai/db）
 *
 * 服务于门户「数据库设计」步骤页，提供：
 *  - GET  /ai/db/models  可用模型列表（来自 ai_model_config 启用项）
 *  - POST /ai/db/doc     按项目读取当前数据库设计（进入页面恢复编辑内容）
 *  - POST /ai/db/save    保存/更新数据库设计（草稿 upsert）
 *  - POST /ai/db/generate 流式生成数据库设计（SSE，复用 AiModelClient）
 *  - POST /ai/db/submit/{projectId} 提交数据库设计：落库 status=1 并推进项目阶段到 DONE
 *
 * 与后台管理接口 /system/table、/system/column（结构化库表 CRUD）完全独立，互不影响。
 * 所有接口仅校验登录态（SecurityConfig 中未匿名放行 /ai/**，需携带有效令牌）。
 *
 * @author devpivot
 * @date 2026-08-08
 */
@RestController
@RequestMapping("/ai/db")
public class AiDbController extends BaseController
{
    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiDbDocService dbDocService;

    @Autowired
    private IAiProjectService projectService;

    @Autowired
    private IAiPrdDocService prdDocService;

    @Autowired
    private IAiTechDocService techDocService;

    /** 流式推送任务线程池 */
    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(AiDbController.class);

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
     * 门户读取项目当前数据库设计（按 projectId 取最新一条），供进入页面恢复编辑内容。
     */
    @PostMapping("/doc")
    public AjaxResult getByProject(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        if (projectId == null) return error("项目ID不能为空");
        AiDbDoc q = new AiDbDoc();
        q.setProjectId(projectId);
        List<AiDbDoc> list = dbDocService.selectAiDbDocList(q);
        if (list != null && !list.isEmpty()) return success(list.get(0));
        return success(null);
    }

    /**
     * 门户保存/更新数据库设计（按 projectId upsert），供编辑保存与生成后落库。
     * 兼容字段：docName / dbType / content / multiSource / sourceModel / status。
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        if (projectId == null) return error("项目ID不能为空");

        AiDbDoc q = new AiDbDoc();
        q.setProjectId(projectId);
        List<AiDbDoc> list = dbDocService.selectAiDbDocList(q);

        AiDbDoc doc = new AiDbDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(body.get("docName"), "数据库设计"));
        doc.setDbType(str(body.get("dbType"), null));
        doc.setContent(body.get("content") == null ? "" : String.valueOf(body.get("content")));
        doc.setMultiSource(str(body.get("multiSource"), null));
        doc.setSourceModel(str(body.get("sourceModel"), null));
        doc.setStatus(str(body.get("status"), "0"));

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            dbDocService.updateAiDbDoc(doc);
            return success(doc.getDocId());
        }
        dbDocService.insertAiDbDoc(doc);
        return success(doc.getDocId());
    }

    /**
     * 提交数据库设计：落库 status=1（upsert）并推进项目阶段到 DONE。
     * 后端统一处理阶段推进，前端无需再单独调用项目更新接口。
     */
    @PostMapping("/submit/{projectId}")
    public AjaxResult submit(@PathVariable("projectId") Long projectId,
                             @RequestBody(required = false) Map<String, Object> body)
    {
        if (projectId == null) return error("项目ID不能为空");
        Map<String, Object> b = body == null ? new HashMap<>(0) : body;

        AiDbDoc q = new AiDbDoc();
        q.setProjectId(projectId);
        List<AiDbDoc> list = dbDocService.selectAiDbDocList(q);

        AiDbDoc doc = new AiDbDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(b.get("docName"), "数据库设计"));
        doc.setDbType(str(b.get("dbType"), null));
        doc.setContent(b.get("content") == null ? "" : String.valueOf(b.get("content")));
        doc.setMultiSource(str(b.get("multiSource"), null));
        doc.setSourceModel(str(b.get("sourceModel"), null));
        doc.setStatus("1");

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            dbDocService.updateAiDbDoc(doc);
        }
        else
        {
            dbDocService.insertAiDbDoc(doc);
        }

        // 推进项目阶段到 DONE（完成）
        AiProject project = new AiProject();
        project.setProjectId(projectId);
        project.setStep("DONE");
        projectService.updateAiProject(project);

        return success();
    }

    /**
     * 生成数据库设计（流式 SSE）
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
        log.info("[db-generate] 收到请求 projectId={}", body == null ? null : body.get("projectId"));

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

        // 单模型：取前端传入模型的第一个
        List<Object> models = body.get("models") instanceof List ? (List<Object>) body.get("models") : null;
        String model = (models != null && !models.isEmpty()) ? String.valueOf(models.get(0)) : defaultModelCode();
        final String usedModel = model;

        String projectName = str(body.get("projectName"), "产品");
        String industryType = str(body.get("industryType"), "通用行业");
        String targetUser = str(body.get("targetUser"), "目标用户");
        String dbType = str(body.get("dbType"), "MySQL");
        String extraReq = str(body.get("extraReq"), "");

        // 上游上下文：优先回源 PRD 与技术方案，使生成真正参考上一阶段产物
        String upstream = buildUpstream(projectId);

        // 提示词工程化：从 ai_prompt_template 渲染（DB 缺失时回退内置常量，零回归）
        String extraBlock = (extraReq != null && !extraReq.trim().isEmpty())
                ? "【补充要求】\n" + extraReq + "\n\n" : "";
        Map<String, Object> dbVars = new HashMap<>(8);
        dbVars.put("projectName", projectName);
        dbVars.put("industryType", industryType);
        dbVars.put("targetUser", targetUser);
        dbVars.put("dbType", dbType);
        dbVars.put("upstream", upstream);
        dbVars.put("extraBlock", extraBlock);
        RenderedPrompt dbPrompt = promptTemplateService.render("DB", usedModel, dbVars);
        String systemPrompt = dbPrompt.getSystemPrompt();
        String userPrompt = dbPrompt.getUserPrompt();

        try
        {
            emitter.send(SseEmitter.event().name("start").data(mapOf("type", "start", "modelId", usedModel)));
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
            return emitter;
        }

        STREAM_POOL.submit(() -> {
            try
            {
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
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("[db-generate] 生成异常", e);
                try
                {
                    emitter.send(SseEmitter.event().name("error")
                            .data(mapOf("type", "error", "content", e.getMessage(), "modelId", usedModel)));
                }
                catch (IOException ignored) { }
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 回源读取项目最新 PRD 与技术方案内容，作为生成上下文；无资料时返回提示 */
    private String buildUpstream(Long projectId)
    {
        StringBuilder sb = new StringBuilder();
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
                    if (c.length() > 3000)
                    {
                        c = c.substring(0, 3000) + "\n…（内容已截断，仅取前 3000 字作为上下文）";
                    }
                    sb.append("【上游 PRD 文档摘要】\n").append(c).append("\n\n");
                }
            }
        }
        catch (Exception e)
        {
            log.warn("[db-generate] 读取 PRD 上下文失败", e);
        }
        try
        {
            AiTechDoc q = new AiTechDoc();
            q.setProjectId(projectId);
            List<AiTechDoc> list = techDocService.selectAiTechDocList(q);
            if (list != null && !list.isEmpty())
            {
                String c = list.get(0).getContent();
                if (c != null && !c.trim().isEmpty())
                {
                    if (c.length() > 2000)
                    {
                        c = c.substring(0, 2000) + "\n…（内容已截断，仅取前 2000 字作为上下文）";
                    }
                    sb.append("【上游技术方案摘要】\n").append(c).append("\n\n");
                }
            }
        }
        catch (Exception e)
        {
            log.warn("[db-generate] 读取技术方案上下文失败", e);
        }
        if (sb.length() == 0) return "（暂无上游 PRD 与技术方案文档）";
        return sb.toString();
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
}
