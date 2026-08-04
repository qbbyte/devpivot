package com.ruoyi.ai.controller;

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
import com.ruoyi.ai.domain.AiDbTable;
import com.ruoyi.ai.service.IAiDbTableService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 数据库结构Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/table")
public class AiDbTableController extends BaseController
{
    @Autowired
    private IAiDbTableService aiDbTableService;

    /**
     * 查询数据库结构列表
     */
    @PreAuthorize("@ss.hasPermi('system:table:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiDbTable aiDbTable)
    {
        startPage();
        List<AiDbTable> list = aiDbTableService.selectAiDbTableList(aiDbTable);
        return getDataTable(list);
    }

    /**
     * 导出数据库结构列表
     */
    @PreAuthorize("@ss.hasPermi('system:table:export')")
    @Log(title = "数据库结构", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiDbTable aiDbTable)
    {
        List<AiDbTable> list = aiDbTableService.selectAiDbTableList(aiDbTable);
        ExcelUtil<AiDbTable> util = new ExcelUtil<AiDbTable>(AiDbTable.class);
        util.exportExcel(response, list, "数据库结构数据");
    }

    /**
     * 获取数据库结构详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:table:query')")
    @GetMapping(value = "/{tableId}")
    public AjaxResult getInfo(@PathVariable("tableId") Long tableId)
    {
        return success(aiDbTableService.selectAiDbTableByTableId(tableId));
    }

    /**
     * 新增数据库结构
     */
    @PreAuthorize("@ss.hasPermi('system:table:add')")
    @Log(title = "数据库结构", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiDbTable aiDbTable)
    {
        return toAjax(aiDbTableService.insertAiDbTable(aiDbTable));
    }

    /**
     * 修改数据库结构
     */
    @PreAuthorize("@ss.hasPermi('system:table:edit')")
    @Log(title = "数据库结构", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiDbTable aiDbTable)
    {
        return toAjax(aiDbTableService.updateAiDbTable(aiDbTable));
    }

    /**
     * 删除数据库结构
     */
    @PreAuthorize("@ss.hasPermi('system:table:remove')")
    @Log(title = "数据库结构", businessType = BusinessType.DELETE)
	@DeleteMapping("/{tableIds}")
    public AjaxResult remove(@PathVariable Long[] tableIds)
    {
        return toAjax(aiDbTableService.deleteAiDbTableByTableIds(tableIds));
    }
}
