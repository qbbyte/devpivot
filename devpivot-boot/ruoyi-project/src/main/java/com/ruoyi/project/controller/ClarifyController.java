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
 * AI 澄清 · 数据接口（/portal/clarify）
 * 仅承载澄清会话/结论的读写、进度、采纳、提交、历史版本。
 * AI 能力见同包 AiClarifyController（/ai/clarify）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/portal/clarify")
public class ClarifyController extends BaseController
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

    private static final Logger log = LoggerFactory.getLogger(ClarifyController.class);

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
     * 获取（或创建）项目的澄清会话
     */
    @GetMapping("/session/{projectId}")
    public AjaxResult session(@PathVariable("projectId") Long projectId)
    {
        return success(aiClarifySessionService.getOrCreateSession(projectId));
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
        ParamValidator.projectId(projectId);
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
        ParamValidator.projectId(projectId);
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
        ParamValidator.projectId(projectId);
        String conclusionJson = JSON.toJSONString(conclusion);
        aiClarifySessionService.submitSession(projectId, conclusionJson);
        return success("提交成功");
    }

    /**
     * 保存当前澄清结论为历史版本（快照）。
     * body: { snapshot(完整澄清结论对象), versionName, remark, sourceModel }
     */
    @PostMapping("/version/{projectId}")
    @Transactional
    public AjaxResult saveClarifyVersion(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body)
    {
        ParamValidator.projectId(projectId);
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
        ParamValidator.projectId(projectId);
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
}
