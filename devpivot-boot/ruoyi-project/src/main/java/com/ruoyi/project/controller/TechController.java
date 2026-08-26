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
import com.ruoyi.project.support.EditHistoryRecorder;
import com.ruoyi.project.service.IAiTechDocService;

/**
 * 门户·技术方案 · 数据接口（/portal/tech）
 * 仅承载技术方案的读取、保存、提交（推进阶段到 DB）。
 * 避开后台管理接口 /system/techdoc（AiTechDocController）。
 * AI 能力见同包 AiTechController（/ai/tech）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/portal/tech")
public class TechController extends BaseController
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
    private IAiTechDocService techDocService;

    @Autowired
    private IAiProjectService projectService;

    @Autowired
    private EditHistoryRecorder editHistoryRecorder;

    @Autowired
    private IAiPrdDocService prdDocService;

    /** 流式推送任务线程池 */
    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(TechController.class);

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
     * 门户读取项目当前技术方案（按 projectId 取最新一条），供进入页面恢复编辑内容。
     */
    @PostMapping("/doc")
    public AjaxResult getByProject(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);
        AiTechDoc q = new AiTechDoc();
        q.setProjectId(projectId);
        List<AiTechDoc> list = techDocService.selectAiTechDocList(q);
        if (list != null && !list.isEmpty()) return success(list.get(0));
        return success(null);
    }

    /**
     * 门户保存/更新技术方案（按 projectId upsert），供编辑保存与生成后落库。
     * 兼容字段：docName / techStack / content / multiSource / sourceModel / status。
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);

        AiTechDoc q = new AiTechDoc();
        q.setProjectId(projectId);
        List<AiTechDoc> list = techDocService.selectAiTechDocList(q);

        AiTechDoc doc = new AiTechDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(body.get("docName"), "技术方案"));
        doc.setTechStack(str(body.get("techStack"), null));
        doc.setContent(body.get("content") == null ? "" : String.valueOf(body.get("content")));
        doc.setMultiSource(str(body.get("multiSource"), null));
        doc.setSourceModel(str(body.get("sourceModel"), null));
        doc.setStatus(str(body.get("status"), "0"));

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            techDocService.updateAiTechDoc(doc);
            editHistoryRecorder.record(projectId, "TECH", "UPDATE", "技术方案", "编辑了技术方案", null, null);
            return success(doc.getDocId());
        }
        techDocService.insertAiTechDoc(doc);
        editHistoryRecorder.record(projectId, "TECH", "UPDATE", "技术方案", "编辑了技术方案", null, null);
        return success(doc.getDocId());
    }

    /**
     * 提交技术方案：落库 status=1（upsert）并推进项目阶段到 DB。
     * 后端统一处理阶段推进，前端无需再单独调用项目更新接口。
     */
    @PostMapping("/submit/{projectId}")
    public AjaxResult submit(@PathVariable("projectId") Long projectId,
                             @RequestBody(required = false) Map<String, Object> body)
    {
        ParamValidator.projectId(projectId);
        Map<String, Object> b = body == null ? new HashMap<>(0) : body;

        AiTechDoc q = new AiTechDoc();
        q.setProjectId(projectId);
        List<AiTechDoc> list = techDocService.selectAiTechDocList(q);

        AiTechDoc doc = new AiTechDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(b.get("docName"), "技术方案"));
        doc.setTechStack(str(b.get("techStack"), null));
        doc.setContent(b.get("content") == null ? "" : String.valueOf(b.get("content")));
        doc.setMultiSource(str(b.get("multiSource"), null));
        doc.setSourceModel(str(b.get("sourceModel"), null));
        doc.setStatus("1");

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            techDocService.updateAiTechDoc(doc);
        }
        else
        {
            techDocService.insertAiTechDoc(doc);
        }

        // 推进项目阶段到 DB（数据库设计阶段）
        AiProject project = new AiProject();
        project.setProjectId(projectId);
        project.setStep("DB");
        projectService.updateAiProject(project);

        editHistoryRecorder.record(projectId, "TECH", "RELEASE", "技术方案", "确认技术方案，进入数据库设计", null, null);

        return success();
    }
}
