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
import com.ruoyi.ai.domain.AiVersionRecord;
import com.ruoyi.ai.service.IAiVersionRecordService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 版本全链路记录Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/record")
public class AiVersionRecordController extends BaseController
{
    @Autowired
    private IAiVersionRecordService aiVersionRecordService;

    /**
     * 查询版本全链路记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiVersionRecord aiVersionRecord)
    {
        startPage();
        List<AiVersionRecord> list = aiVersionRecordService.selectAiVersionRecordList(aiVersionRecord);
        return getDataTable(list);
    }

    /**
     * 导出版本全链路记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:record:export')")
    @Log(title = "版本全链路记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiVersionRecord aiVersionRecord)
    {
        List<AiVersionRecord> list = aiVersionRecordService.selectAiVersionRecordList(aiVersionRecord);
        ExcelUtil<AiVersionRecord> util = new ExcelUtil<AiVersionRecord>(AiVersionRecord.class);
        util.exportExcel(response, list, "版本全链路记录数据");
    }

    /**
     * 获取版本全链路记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:record:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(aiVersionRecordService.selectAiVersionRecordByRecordId(recordId));
    }

    /**
     * 新增版本全链路记录
     */
    @PreAuthorize("@ss.hasPermi('system:record:add')")
    @Log(title = "版本全链路记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiVersionRecord aiVersionRecord)
    {
        return toAjax(aiVersionRecordService.insertAiVersionRecord(aiVersionRecord));
    }

    /**
     * 修改版本全链路记录
     */
    @PreAuthorize("@ss.hasPermi('system:record:edit')")
    @Log(title = "版本全链路记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiVersionRecord aiVersionRecord)
    {
        return toAjax(aiVersionRecordService.updateAiVersionRecord(aiVersionRecord));
    }

    /**
     * 删除版本全链路记录
     */
    @PreAuthorize("@ss.hasPermi('system:record:remove')")
    @Log(title = "版本全链路记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(aiVersionRecordService.deleteAiVersionRecordByRecordIds(recordIds));
    }
}
