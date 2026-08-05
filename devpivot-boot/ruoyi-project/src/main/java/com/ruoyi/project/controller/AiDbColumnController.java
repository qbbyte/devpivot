package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.AiDbColumn;
import com.ruoyi.project.service.IAiDbColumnService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 数据库字段定义Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/column")
public class AiDbColumnController extends BaseController
{
    @Autowired
    private IAiDbColumnService aiDbColumnService;

    /**
     * 查询数据库字段定义列表
     */
    @PreAuthorize("@ss.hasPermi('system:column:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiDbColumn aiDbColumn)
    {
        startPage();
        List<AiDbColumn> list = aiDbColumnService.selectAiDbColumnList(aiDbColumn);
        return getDataTable(list);
    }

    /**
     * 导出数据库字段定义列表
     */
    @PreAuthorize("@ss.hasPermi('system:column:export')")
    @Log(title = "数据库字段定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiDbColumn aiDbColumn)
    {
        List<AiDbColumn> list = aiDbColumnService.selectAiDbColumnList(aiDbColumn);
        ExcelUtil<AiDbColumn> util = new ExcelUtil<AiDbColumn>(AiDbColumn.class);
        util.exportExcel(response, list, "数据库字段定义数据");
    }

    /**
     * 获取数据库字段定义详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:column:query')")
    @GetMapping(value = "/{columnId}")
    public AjaxResult getInfo(@PathVariable("columnId") Long columnId)
    {
        return success(aiDbColumnService.selectAiDbColumnByColumnId(columnId));
    }

    /**
     * 新增数据库字段定义
     */
    @PreAuthorize("@ss.hasPermi('system:column:add')")
    @Log(title = "数据库字段定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiDbColumn aiDbColumn)
    {
        return toAjax(aiDbColumnService.insertAiDbColumn(aiDbColumn));
    }

    /**
     * 修改数据库字段定义
     */
    @PreAuthorize("@ss.hasPermi('system:column:edit')")
    @Log(title = "数据库字段定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiDbColumn aiDbColumn)
    {
        return toAjax(aiDbColumnService.updateAiDbColumn(aiDbColumn));
    }

    /**
     * 删除数据库字段定义
     */
    @PreAuthorize("@ss.hasPermi('system:column:remove')")
    @Log(title = "数据库字段定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{columnIds}")
    public AjaxResult remove(@PathVariable Long[] columnIds)
    {
        return toAjax(aiDbColumnService.deleteAiDbColumnByColumnIds(columnIds));
    }
}
