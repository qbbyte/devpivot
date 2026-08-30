package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.project.domain.AiEditHistory;
import com.ruoyi.project.service.IAiEditHistoryService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 编辑历史审计Controller（只读）
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/edithistory")
public class AiEditHistoryController extends BaseController
{
    @Autowired
    private IAiEditHistoryService aiEditHistoryService;

    /**
     * 查询编辑历史列表
     */
    @PreAuthorize("@ss.hasPermi('system:edithistory:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiEditHistory aiEditHistory)
    {
        startPage();
        List<AiEditHistory> list = aiEditHistoryService.selectHistoryList(aiEditHistory, null, null);
        return getDataTable(list);
    }
}
