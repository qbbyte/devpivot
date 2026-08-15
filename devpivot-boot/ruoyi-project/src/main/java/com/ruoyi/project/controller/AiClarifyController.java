package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.domain.AiVersionRecord;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.project.service.IAiClarifySessionService;
import com.ruoyi.project.service.IAiVersionRecordService;
import com.ruoyi.ai.service.IAiModelConfigService;

/**
 * AI澄清会话对话接口
 *
 * 说明：本控制器服务于门户(portal)的需求澄清步骤页，提供模型列表、会话读写、
 * 消息发送（真实多模型调用）、采纳、进度与提交等能力，并将提交结论落库、推进项目阶段到 PRD。
 * 路径前缀 /ai/clarify 与现有后台管理接口 /system/clarify（AiClarifyRecordController）
 * 完全独立，互不影响，未改动任何现有接口。
 *
 * @author devpivot
 * @date 2026-08-05
 */
@RestController
@RequestMapping("/ai/clarify")
public class AiClarifyController extends BaseController
{
    @Autowired
    private IAiClarifySessionService aiClarifySessionService;

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
     * 获取（或创建）项目的澄清会话
     */
    @GetMapping("/session/{projectId}")
    public AjaxResult session(@PathVariable("projectId") Long projectId)
    {
        return success(aiClarifySessionService.getOrCreateSession(projectId));
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
        String kbContext = knowledgeRetrievalService.retrieveAsContext(projectId, "CLARIFY", message);
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

    /**
     * 获取澄清进度（基于会话中的 ai_question 提问数与已回答数估算）
     */
    @GetMapping("/progress/{projectId}")
    public AjaxResult progress(@PathVariable("projectId") Long projectId)
    {
        AiClarifySession session = aiClarifySessionService.getOrCreateSession(projectId);
        List<Object> conversation = parseConversation(session.getConversation());
        int total = 0;
        int answered = 0;
        boolean pendingQuestion = false;
        for (Object o : conversation)
        {
            if (!(o instanceof Map)) continue;
            Map<?, ?> msg = (Map<?, ?>) o;
            Object type = msg.get("type");
            if ("ai_question".equals(type))
            {
                total++;
                pendingQuestion = true;
            }
            else if (pendingQuestion && ("user".equals(type) || "model".equals(type) || "user_free".equals(type)
                    || "user_answer".equals(type) || "user_text".equals(type) || "user_adopt".equals(type)))
            {
                answered++;
                pendingQuestion = false;
            }
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("total", total);
        result.put("answered", answered);
        return success(result);
    }

    /**
     * 采纳模型回答：将采纳数据追加到会话 adopted 字段，并同步持久化对话（含 adoptedModel 标记）
     */
    @PostMapping("/adopt")
    public AjaxResult adopt(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        AiClarifySession session = aiClarifySessionService.getOrCreateSession(projectId);

        // 前端传回权威对话（含 adoptedModel 标记与 user_adopt 消息），优先持久化
        Object convObj = body.get("conversation");
        if (convObj instanceof List)
        {
            session.setConversation(JSON.toJSONString(convObj));
        }

        List<Object> adopted = parseConversation(session.getAdopted());
        Map<String, Object> item = new HashMap<>(body);
        item.remove("conversation");
        item.put("adoptTime", new Date().toString());
        adopted.add(item);
        session.setAdopted(JSON.toJSONString(adopted));
        session.setStatus("0");
        aiClarifySessionService.saveSession(session);
        return success(session);
    }

    /**
     * 持久化完整对话（前端为权威源）：将整个聊天记录(含 ai_question / user_answer /
     * ai_multi_response / user_adopt 等)与保留要点原样落库，刷新页面后可完整恢复。
     * 与 send() 内“边流边存”互补：send 仅存用户消息+AI回答，本接口补齐前端播种的
     * ai_question 与采纳/保留等业务消息，保证持久化记录与页面展示一致。
     */
    @PostMapping("/save/{projectId}")
    public AjaxResult save(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        AiClarifySession session = aiClarifySessionService.getOrCreateSession(projectId);
        Object convObj = body.get("conversation");
        if (convObj instanceof List)
        {
            session.setConversation(JSON.toJSONString(convObj));
        }
        Object retainedObj = body.get("retained");
        if (retainedObj instanceof List)
        {
            session.setRetained(JSON.toJSONString(retainedObj));
        }
        session.setStatus("0");
        aiClarifySessionService.saveSession(session);
        return success("保存成功");
    }

    /**
     * 提交澄清结果：持久化结论快照并推进项目阶段到 PRD
     */
    @PostMapping("/submit/{projectId}")
    public AjaxResult submit(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> conclusion)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        String conclusionJson = JSON.toJSONString(conclusion);
        aiClarifySessionService.submitSession(projectId, conclusionJson);
        return success("提交成功");
    }

    /* ============================ 历史版本（复用 ai_version_record，bizType=CLARIFY） ============================ */

    /** 澄清产物类型常量，与 ai_version_record.biz_type 约定一致 */
    private static final String CLARIFY_BIZ_TYPE = "CLARIFY";

    /**
     * 保存当前澄清结论为历史版本（快照）。
     * body: { snapshot(完整澄清结论对象), versionName, remark, sourceModel }
     */
    @PostMapping("/version/{projectId}")
    @Transactional
    public AjaxResult saveClarifyVersion(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        Object snapshotObj = body.get("snapshot");
        if (snapshotObj == null)
        {
            return error("快照内容不能为空");
        }
        String versionName = str(body.get("versionName"));
        if (versionName == null || versionName.isEmpty())
        {
            versionName = "版本 " + new Date().toString();
        }
        // 版本号：本项目 CLARIFY 已有数量 + 1
        AiVersionRecord query = new AiVersionRecord();
        query.setProjectId(projectId);
        query.setBizType(CLARIFY_BIZ_TYPE);
        int cnt = versionRecordService.selectAiVersionRecordList(query).size();
        String versionNo = "V" + (cnt + 1);

        AiVersionRecord rec = new AiVersionRecord();
        rec.setProjectId(projectId);
        rec.setBizType(CLARIFY_BIZ_TYPE);
        rec.setBizId(projectId);
        rec.setVersionNo(versionNo);
        rec.setContentSnapshot(JSON.toJSONString(snapshotObj));
        rec.setChangeRemark(str(body.get("remark")));
        rec.setSourceModel(str(body.get("sourceModel")));
        rec.setStatus("1");
        rec.setCreateBy(SecurityUtils.getUsername());
        rec.setRemark(versionName);
        versionRecordService.insertAiVersionRecord(rec);
        return success(mapOf("versionId", rec.getRecordId(), "versionNo", versionNo));
    }

    /**
     * 历史版本列表（含派生文件清单，不含大快照正文）。同一个项目会经历多阶段，
     * 故必须按 bizType=CLARIFY 过滤，避免把 PRD/原型等其它阶段的版本混入澄清历史。
     */
    @GetMapping("/versions/{projectId}")
    public AjaxResult listClarifyVersions(@PathVariable("projectId") Long projectId)
    {
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        AiVersionRecord query = new AiVersionRecord();
        query.setProjectId(projectId);
        query.setBizType(CLARIFY_BIZ_TYPE);
        List<AiVersionRecord> list = versionRecordService.selectAiVersionRecordList(query);
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (AiVersionRecord v : list)
        {
            Map<String, Object> files = buildClarifyFiles(v.getContentSnapshot());
            Map<String, Object> item = new HashMap<>(8);
            item.put("versionId", v.getRecordId());
            item.put("version", v.getVersionNo());
            item.put("status", "正式版本");
            item.put("time", v.getCreateTime());
            item.put("author", v.getCreateBy());
            item.put("summary", v.getChangeRemark());
            item.put("files", files.get("files"));
            result.add(item);
        }
        return success(result);
    }

    /** 获取单个版本（含快照正文，供查看/还原） */
    @GetMapping("/version/{versionId}")
    public AjaxResult getClarifyVersion(@PathVariable("versionId") Long versionId)
    {
        if (versionId == null)
        {
            return error("版本ID不能为空");
        }
        AiVersionRecord v = versionRecordService.selectAiVersionRecordByRecordId(versionId);
        if (v == null)
        {
            return error("版本不存在");
        }
        Map<String, Object> files = buildClarifyFiles(v.getContentSnapshot());
        Map<String, Object> meta = new HashMap<>(6);
        meta.put("versionId", v.getRecordId());
        meta.put("version", v.getVersionNo());
        meta.put("status", "正式版本");
        meta.put("time", v.getCreateTime());
        meta.put("author", v.getCreateBy());
        meta.put("summary", v.getChangeRemark());
        Map<String, Object> data = new HashMap<>(3);
        data.put("version", meta);
        data.put("snapshot", parseJsonMap(v.getContentSnapshot()));
        data.put("files", files.get("files"));
        return success(data);
    }

    /** 还原历史版本：把快照写回当前澄清会话（对话/采纳/保留/结论），便于在前端继续编辑 */
    @PostMapping("/version/restore/{versionId}")
    @Transactional
    public AjaxResult restoreClarifyVersion(@PathVariable("versionId") Long versionId)
    {
        if (versionId == null)
        {
            return error("版本ID不能为空");
        }
        AiVersionRecord v = versionRecordService.selectAiVersionRecordByRecordId(versionId);
        if (v == null)
        {
            return error("版本不存在");
        }
        Map<String, Object> snapshot = parseJsonMap(v.getContentSnapshot());
        if (snapshot == null)
        {
            return error("该版本快照为空");
        }
        AiClarifySession session = aiClarifySessionService.getOrCreateSession(v.getProjectId());
        Object conv = snapshot.get("conversation");
        if (conv instanceof List)
        {
            session.setConversation(JSON.toJSONString(conv));
        }
        Object adopted = snapshot.get("adopted");
        if (adopted instanceof List)
        {
            session.setAdopted(JSON.toJSONString(adopted));
        }
        Object retained = snapshot.get("retained");
        if (retained instanceof List)
        {
            session.setRetained(JSON.toJSONString(retained));
        }
        session.setConclusion(v.getContentSnapshot());
        session.setStatus("0");
        aiClarifySessionService.saveSession(session);
        return success("已还原该历史版本到当前会话");
    }

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
}
