package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiProtoComponent;
import com.ruoyi.project.domain.AiProtoPage;
import com.ruoyi.project.domain.AiProtoVersion;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiProtoComponentService;
import com.ruoyi.project.service.IAiProtoPageService;
import com.ruoyi.project.service.IAiProtoVersionService;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IAiModelConfigService;

/**
 * 原型设计 · 门户接口门面（/ai/proto）
 *
 * 说明：本控制器服务门户(portal)的原型设计步骤页，提供页面读写、AI 生成、确认推进、
 * AI 对话等能力。路径前缀 /ai/proto 与现有后台管理接口 /system/page、/system/component
 * （AiProtoPageController / AiProtoComponentController）完全独立，互不影响，未改动任何现有接口。
 *
 * @author devpivot
 * @date 2026-08-07
 */
@RestController
@RequestMapping("/ai/proto")
public class AiProtoController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AiProtoController.class);

    @Autowired
    private IAiProtoPageService aiProtoPageService;

    @Autowired
    private IAiProtoComponentService aiProtoComponentService;

    @Autowired
    private IAiProjectService aiProjectService;

    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private IAiModelConfigService modelConfigService;

    @Autowired
    private IAiProtoVersionService aiProtoVersionService;

    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

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
     * 按项目读取已存原型页面（含组件）。门户加载原型工作台的权威数据源。
     */
    @GetMapping("/pages/{projectId}")
    public AjaxResult pages(@PathVariable("projectId") Long projectId)
    {
        List<AiProtoPage> pages = aiProtoPageService.selectAiProtoPageByProjectId(projectId);
        List<Map<String, Object>> result = new ArrayList<>(pages.size());
        for (AiProtoPage p : pages)
        {
            result.add(toPageMap(p, aiProtoComponentService.selectAiProtoComponentByPageId(p.getPageId())));
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("pages", result);
        return success(data);
    }

    /**
     * 保存（upsert）整个原型：删除该项目旧页面与组件，插入新页面与组件。
     * body: { pages: [前端页面结构], sourceModel: '人工/AI生成' }
     */
    @PostMapping("/save/{projectId}")
    @Transactional
    public AjaxResult save(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        Object pagesObj = body.get("pages");
        if (!(pagesObj instanceof List))
        {
            return error("页面数据格式不正确");
        }
        try
        {
            persistPages(projectId, (List<?>) pagesObj, body.get("sourceModel"));
        }
        catch (Exception e)
        {
            log.error("[proto] save 失败 projectId={}", projectId, e);
            return error("保存失败：" + e.getMessage());
        }
        return success("保存成功");
    }

    /**
     * AI 生成原型（非流式）：读取 PRD（优先 body.prdText），调用模型输出页面+组件 JSON，
     * 落库并返回。若未配置模型 / 调用失败 / 解析失败，回退到后端模板生成。
     */
    @PostMapping("/generate/{projectId}")
    @Transactional
    public AjaxResult generate(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        String projectName = str(body.get("projectName"));
        String deviceType = str(body.get("deviceType"));
        if (deviceType == null || deviceType.isEmpty())
        {
            deviceType = "WEB";
        }
        String prdText = str(body.get("prdText"));
        String model = str(body.get("model"));

        List<Map<String, Object>> pages = null;
        try
        {
            pages = tryGenerateByAi(projectId, projectName, deviceType, prdText, model);
        }
        catch (Exception e)
        {
            log.warn("[proto] AI 生成失败，回退模板 projectId={} {}", projectId, e.getMessage());
        }
        if (pages == null || pages.isEmpty())
        {
            pages = buildTemplatePages(projectId, projectName, deviceType);
        }
        try
        {
            persistPages(projectId, pages, "AI生成");
        }
        catch (Exception e)
        {
            log.error("[proto] generate 落库失败 projectId={}", projectId, e);
            return error("生成落库失败：" + e.getMessage());
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("pages", pages);
        data.put("deviceType", deviceType);
        return success(data);
    }

    /**
     * 确认原型：将项目阶段推进到 TECH（技术方案）。
     */
    @PostMapping("/confirm/{projectId}")
    @Transactional
    public AjaxResult confirm(@PathVariable("projectId") Long projectId)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        AiProject project = aiProjectService.selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            return error("项目不存在");
        }
        project.setStep("TECH");
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(DateUtils.getNowDate());
        aiProjectService.updateAiProject(project);
        return success("已确认原型，进入技术方案阶段");
    }

    /**
     * AI 对话（流式 SSE）：针对用户的原型设计提问，给出建议文本。
     * 若未配置模型或调用失败，回退到本地规则建议文本。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onError(e -> emitter.completeWithError(e));

        String message = str(body.get("message"));
        String model = str(body.get("model"));
        String modelId = resolveModel(model);

        if (modelId == null)
        {
            // 无可用模型：本地规则兜底
            try
            {
                String reply = buildLocalReply(message);
                emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token", "delta", reply)));
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                emitter.complete();
            }
            catch (IOException e)
            {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 提示词工程化：从 ai_prompt_template 按 templateCode 渲染（DB 缺失回退内置常量，零回归）
        Map<String, Object> chatVars = new HashMap<>(1);
        chatVars.put("message", message);
        RenderedPrompt chatPrompt = promptTemplateService.renderByCode("PROTO_CHAT", modelId, chatVars);
        String systemPrompt = chatPrompt.getSystemPrompt();
        String userPrompt = chatPrompt.getUserPrompt();
        STREAM_POOL.submit(() -> {
            try
            {
                aiModelClient.chatStream(modelId, systemPrompt, userPrompt, delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token")
                                .data(mapOf("type", "token", "delta", delta)));
                    }
                    catch (IOException ignored)
                    {
                        // 前端断开，停止推送
                    }
                });
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                emitter.complete();
            }
            catch (Exception e)
            {
                try
                {
                    emitter.send(SseEmitter.event().name("token")
                            .data(mapOf("type", "token", "delta", buildLocalReply(message))));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                    emitter.complete();
                }
                catch (IOException io)
                {
                    emitter.completeWithError(io);
                }
            }
        });
        return emitter;
    }

    /**
     * AI 生成原型（真流式 SSE）：边生成边推 token，结束解析 JSON 数组落库并推 pages 事件。
     * 前端以此获得实时生成体验；原 /generate（一次性 JSON）保留不删。
     */
    @PostMapping("/generate/stream")
    public SseEmitter generateStream(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onError(e -> emitter.completeWithError(e));

        Long projectId = toLong(body.get("projectId"));
        String projectName = str(body.get("projectName"));
        String rawDevice = str(body.get("deviceType"));
        String deviceType = (rawDevice == null || rawDevice.isEmpty()) ? "WEB" : rawDevice;
        String prdText = str(body.get("prdText"));
        String model = str(body.get("model"));

        STREAM_POOL.submit(() -> {
            try
            {
                String text = tryGenerateAiText(projectName, deviceType, prdText, model, delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token")
                                .data(mapOf("type", "token", "delta", delta)));
                    }
                    catch (IOException ignored) { }
                });
                List<Map<String, Object>> pages = parsePagesFromText(text);
                if (pages == null || pages.isEmpty())
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（未配置AI模型或解析失败，使用内置模板生成）\n")));
                    pages = buildTemplatePages(projectId, projectName, deviceType);
                }
                persistPages(projectId, pages, "AI生成");
                emitter.send(SseEmitter.event().name("pages")
                        .data(mapOf("type", "pages", "pages", pages, "deviceType", deviceType)));
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("[proto] 流式生成失败 projectId={}", projectId, e);
                try
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（生成失败：" + e.getMessage() + "）")));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                    emitter.complete();
                }
                catch (IOException io)
                {
                    emitter.completeWithError(io);
                }
            }
        });
        return emitter;
    }

    /**
     * AI 局部改稿（SSE）：把当前页面 + 指令发给模型，要求仅改指令涉及处、其余原样保留，
     * 返回完整 pages 数组并落库。无模型或解析失败时保持原样返回。
     */
    @PostMapping("/patch/{projectId}")
    public SseEmitter patch(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body,
                            HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onError(e -> emitter.completeWithError(e));

        String instruction = str(body.get("instruction"));
        String model = str(body.get("model"));
        Object pagesObj = body.get("pages");

        STREAM_POOL.submit(() -> {
            try
            {
                List<Map<String, Object>> currentPages;
                if (pagesObj instanceof List && !((List<?>) pagesObj).isEmpty())
                {
                    currentPages = (List<Map<String, Object>>) pagesObj;
                }
                else
                {
                    currentPages = loadPagesFromDb(projectId);
                }
                if (currentPages == null || currentPages.isEmpty())
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（当前还没有原型页面，无法局部改稿。请先生成或添加页面。）")));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                    emitter.complete();
                    return;
                }
                String modelId = resolveModel(model);
                if (modelId == null)
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（未配置AI模型，局部改稿需模型支持。当前保持原样，不做改动。）\n")));
                    emitter.send(SseEmitter.event().name("pages").data(mapOf("type", "pages", "pages", currentPages)));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                    emitter.complete();
                    return;
                }
                String currentJson = JSON.toJSONString(currentPages);
                // 提示词工程化：从 ai_prompt_template 按 templateCode 渲染（DB 缺失回退内置常量，零回归）
                Map<String, Object> patchVars = new HashMap<>(2);
                patchVars.put("currentJson", currentJson);
                patchVars.put("instruction", instruction == null ? "" : instruction);
                RenderedPrompt patchPrompt = promptTemplateService.renderByCode("PROTO_PATCH", modelId, patchVars);
                StringBuilder sb = new StringBuilder();
                aiModelClient.chatStream(modelId,
                        patchPrompt.getSystemPrompt(),
                        patchPrompt.getUserPrompt(), delta -> {
                            sb.append(delta);
                            try
                            {
                                emitter.send(SseEmitter.event().name("token")
                                        .data(mapOf("type", "token", "delta", delta)));
                            }
                            catch (IOException ignored) { }
                        });
                List<Map<String, Object>> pages = parsePagesFromText(sb.toString());
                if (pages == null || pages.isEmpty())
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（AI 返回内容无法解析为页面 JSON，已保持原样。）\n")));
                    pages = currentPages;
                }
                persistPages(projectId, pages, "AI生成");
                emitter.send(SseEmitter.event().name("pages").data(mapOf("type", "pages", "pages", pages)));
                emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("[proto] 局部改稿失败 projectId={}", projectId, e);
                try
                {
                    emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token",
                            "delta", "（局部改稿失败：" + e.getMessage() + "）")));
                    emitter.send(SseEmitter.event().name("done-all").data(mapOf("type", "done-all")));
                    emitter.complete();
                }
                catch (IOException io)
                {
                    emitter.completeWithError(io);
                }
            }
        });
        return emitter;
    }

    /* ============================ 历史版本 ============================ */

    /** 保存当前原型为历史版本（快照） */
    @PostMapping("/version/{projectId}")
    @Transactional
    public AjaxResult saveVersion(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        if (projectId == null) return error("项目ID不能为空");
        Object pagesObj = body.get("pages");
        if (!(pagesObj instanceof List)) return error("页面数据格式不正确");
        String versionName = str(body.get("versionName"));
        if (versionName == null || versionName.isEmpty())
        {
            versionName = "版本 " + DateUtils.dateTimeNow("yyyy-MM-dd HH:mm");
        }
        String deviceType = str(body.get("deviceType"));
        if (deviceType == null && !((List<?>) pagesObj).isEmpty())
        {
            Object first = ((List<?>) pagesObj).get(0);
            if (first instanceof Map) deviceType = str(((Map<?, ?>) first).get("deviceType"));
        }
        AiProtoVersion v = new AiProtoVersion();
        v.setProjectId(projectId);
        v.setVersionName(versionName);
        v.setDeviceType(deviceType);
        v.setSourceModel(str(body.get("sourceModel")));
        v.setSnapshot(JSON.toJSONString(pagesObj));
        v.setCreateBy(SecurityUtils.getUsername());
        v.setRemark(str(body.get("remark")));
        aiProtoVersionService.insertAiProtoVersion(v);
        return success(mapOf("versionId", v.getVersionId()));
    }

    /** 历史版本列表（不含快照） */
    @GetMapping("/versions/{projectId}")
    public AjaxResult listVersions(@PathVariable("projectId") Long projectId)
    {
        if (projectId == null) return error("项目ID不能为空");
        List<AiProtoVersion> list = aiProtoVersionService.selectAiProtoVersionByProjectId(projectId);
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (AiProtoVersion v : list)
        {
            result.add(mapOf("versionId", v.getVersionId(), "versionName", v.getVersionName(),
                    "deviceType", v.getDeviceType(), "sourceModel", v.getSourceModel(),
                    "createBy", v.getCreateBy(), "createTime", v.getCreateTime(), "remark", v.getRemark()));
        }
        return success(result);
    }

    /** 获取单个版本（含页面快照） */
    @GetMapping("/version/{versionId}")
    public AjaxResult getVersion(@PathVariable("versionId") Long versionId)
    {
        if (versionId == null) return error("版本ID不能为空");
        AiProtoVersion v = aiProtoVersionService.selectAiProtoVersionByVersionId(versionId);
        if (v == null) return error("版本不存在");
        Map<String, Object> data = new HashMap<>(4);
        data.put("version", mapOf("versionId", v.getVersionId(), "versionName", v.getVersionName(),
                "deviceType", v.getDeviceType(), "sourceModel", v.getSourceModel(),
                "createBy", v.getCreateBy(), "createTime", v.getCreateTime(), "remark", v.getRemark()));
        data.put("pages", parseJsonList(v.getSnapshot()));
        return success(data);
    }

    /** 还原历史版本（覆盖当前原型并落库） */
    @PostMapping("/version/restore/{versionId}")
    @Transactional
    public AjaxResult restoreVersion(@PathVariable("versionId") Long versionId)
    {
        if (versionId == null) return error("版本ID不能为空");
        AiProtoVersion v = aiProtoVersionService.selectAiProtoVersionByVersionId(versionId);
        if (v == null) return error("版本不存在");
        List<Map<String, Object>> pages = parseJsonList(v.getSnapshot());
        if (pages == null || pages.isEmpty()) return error("该版本快照为空");
        persistPages(v.getProjectId(), pages, "历史还原");
        Map<String, Object> data = new HashMap<>(2);
        data.put("pages", pages);
        data.put("deviceType", v.getDeviceType());
        return success(data);
    }

    /* ============================ 内部方法 ============================ */

    /** 落库：删除旧页面+组件，插入新页面与组件 */
    private void persistPages(Long projectId, List<?> pagesObj, Object sourceModel)
    {
        aiProtoComponentService.deleteAiProtoComponentByProjectId(projectId);
        aiProtoPageService.deleteAiProtoPageByProjectId(projectId);

        String creator = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        List<AiProtoComponent> allComps = new ArrayList<>();

        for (Object o : pagesObj)
        {
            if (!(o instanceof Map)) continue;
            Map<?, ?> pm = (Map<?, ?>) o;
            AiProtoPage page = new AiProtoPage();
            page.setProjectId(projectId);
            page.setPageName(str(pm.get("pageName")));
            page.setPageDesc(str(pm.get("pageDesc")));
            page.setStatus(str(pm.get("status")) == null ? "0" : str(pm.get("status")));
            page.setDeviceType(str(pm.get("deviceType")) == null ? "WEB" : str(pm.get("deviceType")));
            page.setSourceModel(sourceModel == null ? null : str(sourceModel));
            page.setCreateBy(creator);
            page.setCreateTime(now);
            aiProtoPageService.insertAiProtoPage(page); // useGeneratedKeys 回填 pageId

            Object compsObj = pm.get("components");
            if (compsObj instanceof List)
            {
                int idx = 0;
                for (Object co : (List<?>) compsObj)
                {
                    if (!(co instanceof Map)) continue;
                    Map<?, ?> cm = (Map<?, ?>) co;
                    allComps.add(toEntity(page.getPageId(), cm, creator, now, idx++));
                }
            }
        }
        aiProtoComponentService.batchInsertAiProtoComponent(allComps);
    }

    /** 前端组件 Map → 后端实体 */
    private AiProtoComponent toEntity(Long pageId, Map<?, ?> cm, String creator, Date now, int sort)
    {
        AiProtoComponent c = new AiProtoComponent();
        c.setPageId(pageId);
        c.setType(str(cm.get("type")));
        c.setCompType(str(cm.get("compType")));
        c.setCompName(str(cm.get("compName")));
        c.setFieldName(str(cm.get("fieldName")));
        c.setFieldType(str(cm.get("fieldType")));
        c.setRequired(str(cm.get("required")) == null ? "N" : str(cm.get("required")));
        Object ws = cm.get("widthSpan");
        c.setWidthSpan(ws instanceof Number ? ((Number) ws).longValue() : 12L);
        c.setBizDesc(str(cm.get("bizDesc")));
        c.setInteractDesc(str(cm.get("interactDesc")));
        Object pid = cm.get("parentId");
        c.setParentId(pid instanceof Number ? ((Number) pid).longValue() : 0L);
        c.setSort((long) sort);
        c.setProps(jsonStr(cm.get("props")));
        c.setStyle(jsonStr(cm.get("style")));
        c.setInteraction(jsonStr(cm.get("interaction")));
        // ep / epProps / epText 等渲染扩展信息统一存入 meta
        Map<String, Object> meta = new HashMap<>(3);
        meta.put("ep", cm.get("ep"));
        meta.put("epProps", cm.get("epProps"));
        meta.put("epText", cm.get("epText"));
        c.setMeta(JSON.toJSONString(meta));
        c.setCreateBy(creator);
        c.setCreateTime(now);
        return c;
    }

    /** 后端页面实体 + 组件实体 → 前端页面 Map（含 components） */
    private Map<String, Object> toPageMap(AiProtoPage p, List<AiProtoComponent> comps)
    {
        Map<String, Object> m = new HashMap<>(8);
        m.put("pageId", p.getPageId());
        m.put("pageName", p.getPageName());
        m.put("pageDesc", p.getPageDesc());
        m.put("status", p.getStatus());
        m.put("deviceType", p.getDeviceType());
        m.put("sourceModel", p.getSourceModel());
        List<Map<String, Object>> list = new ArrayList<>(comps.size());
        for (AiProtoComponent c : comps)
        {
            list.add(toCompMap(c));
        }
        m.put("components", list);
        return m;
    }

    /** 后端组件实体 → 前端组件 Map（uid=compId，meta 还原为 ep/epProps/epText 顶层字段） */
    private Map<String, Object> toCompMap(AiProtoComponent c)
    {
        Map<String, Object> m = new HashMap<>(20);
        m.put("uid", String.valueOf(c.getCompId()));
        m.put("compId", c.getCompId());
        m.put("pageId", c.getPageId());
        m.put("type", c.getType());
        m.put("compType", c.getCompType());
        m.put("compName", c.getCompName());
        m.put("fieldName", c.getFieldName());
        m.put("fieldType", c.getFieldType());
        m.put("required", c.getRequired());
        m.put("widthSpan", c.getWidthSpan() == null ? 12 : c.getWidthSpan().intValue());
        m.put("bizDesc", c.getBizDesc());
        m.put("interactDesc", c.getInteractDesc());
        m.put("parentId", c.getParentId());
        m.put("sort", c.getSort());
        m.put("props", parseJson(c.getProps()));
        m.put("style", parseJson(c.getStyle()));
        m.put("interaction", parseJson(c.getInteraction()));
        Map<String, Object> meta = parseJson(c.getMeta());
        if (meta != null)
        {
            m.put("ep", meta.get("ep"));
            m.put("epProps", meta.get("epProps"));
            m.put("epText", meta.get("epText"));
        }
        return m;
    }

    /** 尝试用 AI 生成页面结构；返回 null 表示不可用 */
    private List<Map<String, Object>> tryGenerateByAi(Long projectId, String projectName, String deviceType,
                                                      String prdText, String model)
    {
        String text = tryGenerateAiText(projectName, deviceType, prdText, model, null);
        return parsePagesFromText(text);
    }

    /**
     * 调用模型生成页面结构文本（流式：每个 token 通过 onToken 回调，可为 null）。
     * 返回模型累积的原始文本；无可用模型或调用失败返回 null。
     */
    private String tryGenerateAiText(String projectName, String deviceType, String prdText, String model,
                                     Consumer<String> onToken)
    {
        String modelId = resolveModel(model);
        if (modelId == null) return null;

                // 提示词工程化：从 ai_prompt_template 按 templateCode 渲染（DB 缺失回退内置常量，零回归）
        String prdBlock = (prdText != null && !prdText.isEmpty()) ? "；需求背景：" + prdText + " " : "";
        Map<String, Object> genVars = new HashMap<>(3);
        genVars.put("projectName", projectName == null ? "未命名产品" : projectName);
        genVars.put("deviceType", deviceType);
        genVars.put("prdBlock", prdBlock);
        RenderedPrompt genPrompt = promptTemplateService.renderByCode("PROTO_GEN", modelId, genVars);
        String schemaHint = genPrompt.getSystemPrompt();
        String userMsg = genPrompt.getUserPrompt();

        StringBuilder sb = new StringBuilder();
        aiModelClient.chatStream(modelId, schemaHint, userMsg, delta -> {
            sb.append(delta);
            if (onToken != null)
            {
                onToken.accept(delta);
            }
        });
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 从模型文本中提取并解析页面 JSON 数组；无法解析返回 null */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePagesFromText(String text)
    {
        if (text == null) return null;
        String json = extractJsonArray(text);
        if (json == null) return null;
        try
        {
            JSONArray arr = JSON.parseArray(json);
            List<Map<String, Object>> pages = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++)
            {
                Object o = arr.get(i);
                if (o instanceof JSONObject)
                {
                    pages.add(((JSONObject) o).to(Map.class));
                }
            }
            return pages.isEmpty() ? null : pages;
        }
        catch (Exception e)
        {
            log.warn("[proto] AI 返回 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /** 后端模板生成（无 AI 或 AI 失败时的兜底） */
    private List<Map<String, Object>> buildTemplatePages(Long projectId, String projectName, String deviceType)
    {
        String name = projectName == null || projectName.isEmpty() ? "产品" : projectName;
        List<Map<String, Object>> pages = new ArrayList<>();
        if (!"WEB".equals(deviceType))
        {
            pages.add(mobilePage(name + " · 首页", "移动端首页（卡片流）", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "首页", null, 12, mapOf("menus", new String[]{"首页", "分类", "我的"})),
                    comp("input", "FORM", "搜索", "搜索", "STRING", 12, mapOf("label", "搜索", "placeholder", "搜索")),
                    comp("card", "VIEW", "商品卡片", "商品卡片", null, 6, mapOf("title", "商品 A", "desc", "¥99 · 已售 1.2k")),
                    comp("card", "VIEW", "商品卡片", "商品卡片", null, 6, mapOf("title", "商品 B", "desc", "¥129 · 已售 860")),
                    comp("button", "BASE", "发布", "发布", null, 12, mapOf("text", "＋ 发布", "type", "primary"))
            ));
            pages.add(mobilePage(name + " · 列表", "移动端列表（单元格）", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{"商品", "分类", "我的"})),
                    comp("list", "VIEW", "商品列表", "商品列表", null, 12, mapOf("items", new String[]{"商品 A · ¥99", "商品 B · ¥129", "商品 C · ¥59"}))
            ));
            pages.add(mobilePage(name + " · 详情", "移动端详情（单列表单）", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{"详情", "编辑", "返回"})),
                    comp("text", "BASE", "标题", "标题", null, 12, mapOf("text", name + " 详情")),
                    comp("input", "FORM", "名称", "名称", "STRING", 12, mapOf("label", "名称", "placeholder", "示例")),
                    comp("select", "FORM", "规格", "规格", "ENUM", 12, mapOf("label", "规格", "options", new String[]{"标准", "豪华"}))
            ));
            pages.add(mobilePage(name + " · 我的", "移动端个人中心", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{"首页", "分类", "我的"})),
                    comp("list", "VIEW", "我的菜单", "我的菜单", null, 12, mapOf("items", new String[]{"我的订单", "收货地址", "优惠券", "设置"}))
            ));
        }
        else
        {
            pages.add(webPage(name + " · 列表页", "数据列表与操作入口", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{name, "数据管理", "系统设置"})),
                    comp("table", "VIEW", "数据表格", "数据表格", null, 12, mapOf("columns", new String[]{"名称", "状态", "负责人", "更新时间", "操作"}, "rows", 5)),
                    comp("button", "BASE", "新建", "新建", null, 3, mapOf("text", "＋ 新建", "type", "primary"))
            ));
            pages.add(webPage(name + " · 详情页", "单条数据详情展示", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{name, "返回列表", "编辑"})),
                    comp("input", "FORM", "名称", "名称", "STRING", 12, mapOf("label", "名称", "placeholder", "示例数据")),
                    comp("select", "FORM", "状态", "状态", "ENUM", 12, mapOf("label", "状态", "options", new String[]{"启用", "停用"})),
                    comp("button", "BASE", "返回", "返回", null, 2, mapOf("text", "← 返回")),
                    comp("button", "BASE", "编辑", "编辑", null, 2, mapOf("text", "编辑", "type", "primary"))
            ));
            pages.add(webPage(name + " · 新增/编辑页", "表单录入与提交", deviceType,
                    comp("nav", "NAV", "顶部导航栏", "导航", null, 12, mapOf("menus", new String[]{name, "返回列表", "保存"})),
                    comp("input", "FORM", "名称", "名称", "STRING", 12, mapOf("label", "名称", "placeholder", "请输入名称")),
                    comp("number", "FORM", "数量", "数量", "NUMBER", 12, mapOf("label", "数量")),
                    comp("switch", "FORM", "是否启用", "是否启用", "BOOLEAN", 12, mapOf("label", "是否启用")),
                    comp("submit", "FORM", "提交", "提交", null, 3, mapOf("text", "保存提交"))
            ));
        }
        return pages;
    }

    private Map<String, Object> webPage(String pageName, String desc, String deviceType, Map<String, Object>... comps)
    {
        return pageOf(pageName, desc, deviceType, comps);
    }

    private Map<String, Object> mobilePage(String pageName, String desc, String deviceType, Map<String, Object>... comps)
    {
        return pageOf(pageName, desc, deviceType, comps);
    }

    private Map<String, Object> pageOf(String pageName, String desc, String deviceType, Map<String, Object>... comps)
    {
        Map<String, Object> p = new HashMap<>(6);
        p.put("pageName", pageName);
        p.put("pageDesc", desc);
        p.put("status", "0");
        p.put("deviceType", deviceType);
        p.put("components", new ArrayList<Map<String, Object>>(java.util.Arrays.asList(comps)));
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> comp(String type, String compType, String compName, String fieldName,
                                     String fieldType, int widthSpan, Map<String, Object> props)
    {
        Map<String, Object> c = new HashMap<>(10);
        c.put("type", type);
        c.put("compType", compType);
        c.put("compName", compName);
        c.put("fieldName", fieldName == null ? "" : fieldName);
        c.put("fieldType", fieldType == null ? "" : fieldType);
        c.put("required", "N");
        c.put("widthSpan", widthSpan);
        c.put("bizDesc", "");
        c.put("interactDesc", "");
        c.put("props", props == null ? new HashMap<String, Object>() : props);
        c.put("style", new HashMap<String, Object>());
        c.put("interaction", mapOf("action", "none"));
        return c;
    }

    /* ============================ 工具 ============================ */

    private String resolveModel(String preferred)
    {
        if (preferred != null && !preferred.isEmpty())
        {
            return preferred;
        }
        try
        {
            AiModelConfig q = new AiModelConfig();
            q.setIsEnabled("0");
            List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(q);
            if (list != null && !list.isEmpty())
            {
                return list.get(0).getModelCode();
            }
        }
        catch (Exception e)
        {
            log.warn("[proto] 解析默认模型失败：{}", e.getMessage());
        }
        return null;
    }

    private String buildLocalReply(String message)
    {
        String msg = message == null ? "" : message.trim();
        if (msg.contains("表单") || msg.contains("字段"))
        {
            return "表单设计建议：必填字段（带 *）放在靠前位置；把 fieldType 设为 NUMBER/DATE/ENUM 便于下游自动推导列类型；需要我直接生成一套标准增删改查表单吗？";
        }
        if (msg.contains("列表") || msg.contains("表格"))
        {
            return "列表页建议：操作列固定靠右并配合分页；「新建」按钮链接到新增/编辑页形成可走查原型；行数据先放 5 行示例便于演示。";
        }
        if (msg.contains("导航") || msg.contains("菜单"))
        {
            return "导航设计建议：顶部导航承载一级模块，菜单 ≤ 5 个，超出用「更多」收起；导航项可配置跳转实现页面走查。";
        }
        return "我已了解你的需求。可以用一句话描述你想改的页面或组件，例如「给列表页加一个搜索框」，我会给出具体设计建议。";
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o);
    }

    private static Long toLong(Object o)
    {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o).trim()); } catch (Exception e) { return null; }
    }

    /** 从数据库读取当前原型页面（前端 Map 结构），供 patch 兜底使用 */
    private List<Map<String, Object>> loadPagesFromDb(Long projectId)
    {
        List<AiProtoPage> pages = aiProtoPageService.selectAiProtoPageByProjectId(projectId);
        List<Map<String, Object>> result = new ArrayList<>(pages.size());
        for (AiProtoPage p : pages)
        {
            result.add(toPageMap(p, aiProtoComponentService.selectAiProtoComponentByPageId(p.getPageId())));
        }
        return result;
    }

    /** 解析快照 JSON 数组字符串为页面列表 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonList(String s)
    {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        try
        {
            Object o = JSON.parse(s);
            if (o instanceof List) return (List<Map<String, Object>>) o;
        }
        catch (Exception e) { }
        return new ArrayList<>();
    }

    private static String jsonStr(Object o)
    {
        if (o == null) return null;
        if (o instanceof String) return (String) o;
        return JSON.toJSONString(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String s)
    {
        if (s == null || s.isEmpty()) return new HashMap<>();
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
            // ignore
        }
        return new HashMap<>();
    }

    /** 从模型返回文本中提取 JSON 数组（兼容 ```json 围栏 与裸 JSON） */
    private static String extractJsonArray(String text)
    {
        if (text == null) return null;
        String t = text.trim();
        int fence = t.indexOf("```");
        if (fence >= 0)
        {
            int start = t.indexOf('[', fence);
            int end = t.lastIndexOf(']');
            if (start >= 0 && end > start)
            {
                return t.substring(start, end + 1);
            }
        }
        int first = t.indexOf('[');
        int last = t.lastIndexOf(']');
        if (first >= 0 && last > first)
        {
            return t.substring(first, last + 1);
        }
        return null;
    }

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
}
