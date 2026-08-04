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
import com.ruoyi.ai.domain.AiModelCallLog;
import com.ruoyi.ai.service.IAiModelCallLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI模型调用日志Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/ailog")
public class AiModelCallLogController extends BaseController
{
    @Autowired
    private IAiModelCallLogService aiModelCallLogService;

    /**
     * 查询AI模型调用日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModelCallLog aiModelCallLog)
    {
        startPage();
        List<AiModelCallLog> list = aiModelCallLogService.selectAiModelCallLogList(aiModelCallLog);
        return getDataTable(list);
    }

    /**
     * 导出AI模型调用日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:export')")
    @Log(title = "AI模型调用日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiModelCallLog aiModelCallLog)
    {
        List<AiModelCallLog> list = aiModelCallLogService.selectAiModelCallLogList(aiModelCallLog);
        ExcelUtil<AiModelCallLog> util = new ExcelUtil<AiModelCallLog>(AiModelCallLog.class);
        util.exportExcel(response, list, "AI模型调用日志数据");
    }

    /**
     * 获取AI模型调用日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(aiModelCallLogService.selectAiModelCallLogByLogId(logId));
    }

    /**
     * 新增AI模型调用日志
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:add')")
    @Log(title = "AI模型调用日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelCallLog aiModelCallLog)
    {
        return toAjax(aiModelCallLogService.insertAiModelCallLog(aiModelCallLog));
    }

    /**
     * 修改AI模型调用日志
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:edit')")
    @Log(title = "AI模型调用日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelCallLog aiModelCallLog)
    {
        return toAjax(aiModelCallLogService.updateAiModelCallLog(aiModelCallLog));
    }

    /**
     * 删除AI模型调用日志
     */
    @PreAuthorize("@ss.hasPermi('system:ailog:remove')")
    @Log(title = "AI模型调用日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(aiModelCallLogService.deleteAiModelCallLogByLogIds(logIds));
    }
}
