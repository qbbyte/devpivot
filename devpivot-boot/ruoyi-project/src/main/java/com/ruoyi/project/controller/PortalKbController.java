package com.ruoyi.project.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.ai.domain.AiKbDoc;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.project.service.IAiProjectService;

/**
 * 门户·知识库接口（/portal/kb，仅登录态）
 * 列表/检索预览为只读；上传/删除仅允许向「当前用户可写的项目库」写入（isProjectWriter 归属校验），
 * 组织共享库（projectId=-1）与全局管理仍归后台 AiKbController（/system/kb，admin 角色）。
 * 注：本控制器位于业务模块（ruoyi-project），以便注入 IAiProjectService 做归属校验（引擎层不反向依赖业务）。
 * @author devpivot
 */
@RestController
@RequestMapping("/portal/kb")
public class PortalKbController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(PortalKbController.class);

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private IAiProjectService aiProjectService;

    /** 文档列表（只读，登录即可）。
     *  - shared=true：查组织共享库（projectId=-1）
     *  - shared=false 且 projectId 非空：查该项目库
     *  - 两者均为空：全量文档（含共享库） */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long projectId,
                           @RequestParam(required = false, defaultValue = "false") boolean shared,
                           @RequestParam(required = false) String stage)
    {
        if (shared)
        {
            return success(knowledgeRetrievalService.listDocs(IKnowledgeRetrievalService.SHARED_PROJECT_ID, stage));
        }
        if (projectId != null)
        {
            return success(knowledgeRetrievalService.listDocs(projectId, stage));
        }
        return success(knowledgeRetrievalService.listAllDocs(stage));
    }

    /** 检索预览/调试。只读，登录即可。 */
    @GetMapping("/retrieve")
    public AjaxResult retrieve(@RequestParam Long projectId,
                               @RequestParam(required = false) String stage,
                               @RequestParam String query)
    {
        String ctx = knowledgeRetrievalService.retrieveAsContext(projectId, stage, query);
        Map<String, Object> m = new HashMap<>(2);
        m.put("context", ctx);
        return success(m);
    }

    /** 门户上传文档到「自己的项目库」：projectId 必填、禁止 shared 共享库、需项目写权限 */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestBody Map<String, Object> body)
    {
        if (Boolean.TRUE.equals(body.get("shared")))
        {
            return error("共享库仅限管理员维护，请选择项目库上传");
        }
        Long projectId = toLong(body.get("projectId"));
        if (projectId == null || projectId <= 0)
        {
            return error("项目ID不能为空");
        }
        if (!aiProjectService.isProjectWriter(projectId, getUserId(), getUsername()))
        {
            return error("无权向该项目知识库写入");
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

    /** 门户删除自己项目库的文档（归属校验；共享库文档不可由门户删除） */
    @DeleteMapping("/{docId}")
    public AjaxResult remove(@PathVariable Long docId)
    {
        AiKbDoc doc = knowledgeRetrievalService.getDocById(docId);
        if (doc == null)
        {
            return error("文档不存在");
        }
        if (doc.getProjectId() == null || doc.getProjectId() <= 0)
        {
            return error("共享库文档仅限管理员维护");
        }
        if (!aiProjectService.isProjectWriter(doc.getProjectId(), getUserId(), getUsername()))
        {
            return error("无权删除该项目知识库文档");
        }
        return toAjax(knowledgeRetrievalService.deleteDoc(docId));
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
