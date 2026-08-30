package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.service.IAiClarifySessionService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 澄清会话管理Controller（管理端只读审阅）
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/clarifysession")
public class AiClarifySessionController extends BaseController
{
    @Autowired
    private IAiClarifySessionService aiClarifySessionService;

    /**
     * 查询澄清会话列表
     */
    @PreAuthorize("@ss.hasPermi('system:clarifysession:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiClarifySession aiClarifySession)
    {
        startPage();
        List<AiClarifySession> list = aiClarifySessionService.selectAiClarifySessionList(aiClarifySession);
        return getDataTable(list);
    }

    /**
     * 获取澄清会话详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:clarifysession:query')")
    @GetMapping(value = "/{sessionId}")
    public AjaxResult getInfo(@PathVariable("sessionId") Long sessionId)
    {
        return success(aiClarifySessionService.selectAiClarifySessionBySessionId(sessionId));
    }
}
