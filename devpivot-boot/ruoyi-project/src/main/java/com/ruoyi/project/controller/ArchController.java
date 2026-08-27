package com.ruoyi.project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.project.domain.AiArchDoc;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiArchDocService;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.project.support.EditHistoryRecorder;

/**
 * 门户·系统架构设计 · 数据接口（/portal/arch）
 * 仅承载架构设计文档的读取、保存、提交（推进阶段到 TECH）。
 * AI 能力见同包 AiArchController（/ai/arch）。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@RestController
@RequestMapping("/portal/arch")
public class ArchController extends BaseController
{
    @Autowired
    private IAiArchDocService archDocService;

    @Autowired
    private IAiProjectService projectService;

    @Autowired
    private EditHistoryRecorder editHistoryRecorder;

    /**
     * 门户读取项目当前系统架构设计（按 projectId 取最新一条），供进入页面恢复编辑内容。
     */
    @PostMapping("/doc")
    public AjaxResult getByProject(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);
        AiArchDoc q = new AiArchDoc();
        q.setProjectId(projectId);
        List<AiArchDoc> list = archDocService.selectAiArchDocList(q);
        if (list != null && !list.isEmpty()) return success(list.get(0));
        return success(null);
    }

    /**
     * 门户保存/更新系统架构设计（按 projectId upsert），供编辑保存与生成后落库。
     * 兼容字段：docName / content / multiSource / sourceModel / status。
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody Map<String, Object> body)
    {
        Long projectId = parseProjectId(body.get("projectId"));
        ParamValidator.projectId(projectId);

        AiArchDoc q = new AiArchDoc();
        q.setProjectId(projectId);
        List<AiArchDoc> list = archDocService.selectAiArchDocList(q);

        AiArchDoc doc = new AiArchDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(body.get("docName"), "系统架构设计"));
        doc.setContent(body.get("content") == null ? "" : String.valueOf(body.get("content")));
        doc.setMultiSource(str(body.get("multiSource"), null));
        doc.setSourceModel(str(body.get("sourceModel"), null));
        doc.setStatus(str(body.get("status"), "0"));

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            archDocService.updateAiArchDoc(doc);
            editHistoryRecorder.record(projectId, "ARCH", "UPDATE", "系统架构设计", "编辑了系统架构设计", null, null);
            return success(doc.getDocId());
        }
        archDocService.insertAiArchDoc(doc);
        editHistoryRecorder.record(projectId, "ARCH", "UPDATE", "系统架构设计", "编辑了系统架构设计", null, null);
        return success(doc.getDocId());
    }

    /**
     * 提交系统架构设计：落库 status=1（upsert）并推进项目阶段到 TECH（技术方案）。
     * 后端统一处理阶段推进，前端无需再单独调用项目更新接口。
     */
    @PostMapping("/submit/{projectId}")
    public AjaxResult submit(@PathVariable("projectId") Long projectId,
                             @RequestBody(required = false) Map<String, Object> body)
    {
        ParamValidator.projectId(projectId);
        Map<String, Object> b = body == null ? new HashMap<>(0) : body;

        AiArchDoc q = new AiArchDoc();
        q.setProjectId(projectId);
        List<AiArchDoc> list = archDocService.selectAiArchDocList(q);

        AiArchDoc doc = new AiArchDoc();
        doc.setProjectId(projectId);
        doc.setDocName(str(b.get("docName"), "系统架构设计"));
        doc.setContent(b.get("content") == null ? "" : String.valueOf(b.get("content")));
        doc.setMultiSource(str(b.get("multiSource"), null));
        doc.setSourceModel(str(b.get("sourceModel"), null));
        doc.setStatus("1");

        if (list != null && !list.isEmpty())
        {
            doc.setDocId(list.get(0).getDocId());
            archDocService.updateAiArchDoc(doc);
        }
        else
        {
            archDocService.insertAiArchDoc(doc);
        }

        // 推进项目阶段到 TECH（技术方案）
        AiProject project = new AiProject();
        project.setProjectId(projectId);
        project.setStep("TECH");
        projectService.updateAiProject(project);

        editHistoryRecorder.record(projectId, "ARCH", "RELEASE", "系统架构设计", "确认系统架构设计，进入技术方案", null, null);

        return success();
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
}
