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
import com.ruoyi.project.domain.AiClarifyRecord;
import com.ruoyi.project.service.IAiClarifyRecordService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI澄清问题记录Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/clarify")
public class AiClarifyRecordController extends BaseController
{
    @Autowired
    private IAiClarifyRecordService aiClarifyRecordService;

    /**
     * 查询AI澄清问题记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiClarifyRecord aiClarifyRecord)
    {
        startPage();
        List<AiClarifyRecord> list = aiClarifyRecordService.selectAiClarifyRecordList(aiClarifyRecord);
        return getDataTable(list);
    }

    /**
     * 导出AI澄清问题记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:export')")
    @Log(title = "AI澄清问题记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiClarifyRecord aiClarifyRecord)
    {
        List<AiClarifyRecord> list = aiClarifyRecordService.selectAiClarifyRecordList(aiClarifyRecord);
        ExcelUtil<AiClarifyRecord> util = new ExcelUtil<AiClarifyRecord>(AiClarifyRecord.class);
        util.exportExcel(response, list, "AI澄清问题记录数据");
    }

    /**
     * 获取AI澄清问题记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(aiClarifyRecordService.selectAiClarifyRecordByRecordId(recordId));
    }

    /**
     * 新增AI澄清问题记录
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:add')")
    @Log(title = "AI澄清问题记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiClarifyRecord aiClarifyRecord)
    {
        return toAjax(aiClarifyRecordService.insertAiClarifyRecord(aiClarifyRecord));
    }

    /**
     * 修改AI澄清问题记录
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:edit')")
    @Log(title = "AI澄清问题记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiClarifyRecord aiClarifyRecord)
    {
        return toAjax(aiClarifyRecordService.updateAiClarifyRecord(aiClarifyRecord));
    }

    /**
     * 删除AI澄清问题记录
     */
    @PreAuthorize("@ss.hasPermi('system:clarify:remove')")
    @Log(title = "AI澄清问题记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(aiClarifyRecordService.deleteAiClarifyRecordByRecordIds(recordIds));
    }
}
