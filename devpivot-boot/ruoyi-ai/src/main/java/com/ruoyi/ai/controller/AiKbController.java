package com.ruoyi.ai.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;

/**
 * 知识库管理 Controller（AI 引擎层，后台管理侧）。
 *
 * <p>权限策略（2026-08-12 调整）：知识库的「写操作」（上传索引 POST /upload、删除 DELETE /{docId}）
 * 要求管理员角色（@ss.hasRole('admin')），与若依后台其他 AI 配置（模型/提示词/API Key）一致；
 * 检索日志的查询/清理（GET/DELETE /logs）同样要求管理员。
 * 「只读」接口（列表 GET /list、检索预览 GET /retrieve）已迁至同包 KbController（/portal/kb），仅登录态。
 * 注意：AI 生成阶段对知识库的检索走引擎 service 层（KnowledgeRetrievalServiceImpl.retrieveAsContext），
 * 不经本 HTTP 接口，故角色校验不影响生成流水线。
 *
 * @author devpivot
 * @date 2026-08-12
 */
@RestController
@RequestMapping("/system/kb")
public class AiKbController extends BaseController
{
    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    /** 上传并索引一篇文档（upload 来源；shared=true 时存入组织共享库 projectId=-1） */
    @PreAuthorize("@ss.hasRole('admin')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestBody Map<String, Object> body)
    {
        boolean shared = Boolean.TRUE.equals(body.get("shared"));
        Long projectId = shared ? IKnowledgeRetrievalService.SHARED_PROJECT_ID : toLong(body.get("projectId"));
        if (projectId == null)
        {
            return error("项目ID不能为空");
        }
        String stage = body.get("stage") == null ? null : String.valueOf(body.get("stage"));
        String title = body.get("title") == null ? "" : String.valueOf(body.get("title"));
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        if (content.isBlank())
        {
            return error("文档内容不能为空");
        }
        // 入参防护：标题/正文长度上限，避免超长文本撑爆存储或索引
        ParamValidator.requireText(title, 200, "文档标题", true);
        ParamValidator.requireText(content, 100000, "文档内容", false);
        knowledgeRetrievalService.indexDocument(projectId, stage, title, content, "upload");
        return success("索引成功");
    }

    /** 删除文档（级联删切片） */
    @PreAuthorize("@ss.hasRole('admin')")
    @DeleteMapping("/{docId}")
    public AjaxResult remove(@PathVariable Long docId)
    {
        return toAjax(knowledgeRetrievalService.deleteDoc(docId));
    }

    /** 清理检索日志（管理员；保留天数由 kb.retrieval-log.keep-days 控制）。仅登录态不足以调用，需管理员。 */
    @PreAuthorize("@ss.hasRole('admin')")
    @DeleteMapping("/logs")
    public AjaxResult clearLogs()
    {
        return toAjax(knowledgeRetrievalService.cleanupRetrievalLog());
    }

    /** 检索日志查询（管理员；按时间倒序返回最近 limit 条，可选 projectId/stage 过滤）。
     *  与清理接口配套，用于查看"哪些检索命中差/哪些项目在用"。 */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/logs")
    public AjaxResult listLogs(@RequestParam(required = false) Long projectId,
                               @RequestParam(required = false) String stage,
                               @RequestParam(required = false, defaultValue = "100") int limit)
    {
        return success(knowledgeRetrievalService.listRetrievalLogs(projectId, stage, limit));
    }

    private Long toLong(Object o)
    {
        if (o == null)
        {
            return null;
        }
        try
        {
            return Long.valueOf(String.valueOf(o).trim());
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
