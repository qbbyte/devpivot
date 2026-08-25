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
 * 门户·PRD 数据接口（/portal/prd）
 * 仅承载 PRD 的读取、保存、提交（推进阶段到 PROTO）。
 * 避开后台管理接口 /system/doc（AiPrdDocController）。
 * AI 能力见同包 AiPrdGenController（/ai/doc）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/portal/prd")
public class PrdController extends BaseController
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

    private static final Logger log = LoggerFactory.getLogger(PrdController.class);

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
     * 提交 PRD：落库 status=1（upsert）并推进项目阶段到 PROTO。
     * 后端统一处理阶段推进，前端无需再单独调用项目更新接口。
     */
    @PostMapping("/submit/{projectId}")
    public AjaxResult submit(@PathVariable("projectId") Long projectId,
                             @RequestBody(required = false) Map<String, Object> body)
    {
        ParamValidator.projectId(projectId);
        Map<String, Object> b = body == null ? new HashMap<>(0) : body;

        AiPrdDoc q = new AiPrdDoc();
        q.setProjectId(projectId);
        List<AiPrdDoc> list = prdDocService.selectAiPrdDocList(q);

        AiPrdDoc doc = new AiPrdDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(b.get("docName"), "PRD"));
        doc.setTemplateType(str(b.get("templateType"), "STANDARD"));
        doc.setContent(b.get("content") == null ? "" : String.valueOf(b.get("content")));
        doc.setSourceModel(str(b.get("sourceModel"), null));
        doc.setStatus("1");

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            prdDocService.updateAiPrdDoc(doc);
        }
        else
        {
            prdDocService.insertAiPrdDoc(doc);
        }

        AiProject project = new AiProject();
        project.setProjectId(projectId);
        project.setStep("PROTO");
        projectService.updateAiProject(project);

        return success();
    }

    /**
     * 门户读取项目当前 PRD（按 projectId 取最新一条），供进入页面恢复编辑内容。
     * 与后台 /system/doc/list 独立，仅校验登录态，门户用户可用。
     */
    @PostMapping("/get")
    public AjaxResult getByProject(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);
        AiPrdDoc q = new AiPrdDoc();
        q.setProjectId(projectId);
        List<AiPrdDoc> list = prdDocService.selectAiPrdDocList(q);
        if (list != null && !list.isEmpty()) return success(list.get(0));
        return success(null);
    }

    /**
     * 门户保存/更新 PRD（按 projectId upsert），供编辑保存与生成后落库。
     * 与后台 /system/doc 的增改端点独立，仅校验登录态，门户用户可用。
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);
        AiPrdDoc q = new AiPrdDoc();
        q.setProjectId(projectId);
        List<AiPrdDoc> list = prdDocService.selectAiPrdDocList(q);

        AiPrdDoc doc = new AiPrdDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(body.get("docName"), "PRD"));
        doc.setTemplateType(str(body.get("templateType"), "STANDARD"));
        doc.setContent(body.get("content") == null ? "" : String.valueOf(body.get("content")));
        doc.setStatus(str(body.get("status"), "0"));
        doc.setSourceModel(str(body.get("sourceModel"), null));

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            prdDocService.updateAiPrdDoc(doc);
            return success(doc.getDocId());
        }
        prdDocService.insertAiPrdDoc(doc);
        return success(doc.getDocId());
    }
}
