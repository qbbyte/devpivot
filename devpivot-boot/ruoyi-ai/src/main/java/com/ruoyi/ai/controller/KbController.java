package com.ruoyi.ai.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;

/**
 * 门户·知识库只读接口（/portal/kb，仅登录态）
 * 承载门户知识库页的文档列表（按项目/共享库/阶段过滤）与检索预览。
 * 写操作（upload/delete）与日志接口（/logs）见同包 AiKbController（/system/kb，admin 角色）。
 * 注意：AI 生成阶段的检索走引擎 service 层，不经本 HTTP 接口。
 * @author devpivot
 */
@RestController
@RequestMapping("/portal/kb")
public class KbController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(KbController.class);

    @Autowired
    private IKnowledgeRetrievalService knowledgeRetrievalService;

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
}
