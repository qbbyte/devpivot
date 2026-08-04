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
import com.ruoyi.ai.domain.AiParallelTask;
import com.ruoyi.ai.service.IAiParallelTaskService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 多模型并行任务Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/task")
public class AiParallelTaskController extends BaseController
{
    @Autowired
    private IAiParallelTaskService aiParallelTaskService;

    /**
     * 查询多模型并行任务列表
     */
    @PreAuthorize("@ss.hasPermi('system:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiParallelTask aiParallelTask)
    {
        startPage();
        List<AiParallelTask> list = aiParallelTaskService.selectAiParallelTaskList(aiParallelTask);
        return getDataTable(list);
    }

    /**
     * 导出多模型并行任务列表
     */
    @PreAuthorize("@ss.hasPermi('system:task:export')")
    @Log(title = "多模型并行任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiParallelTask aiParallelTask)
    {
        List<AiParallelTask> list = aiParallelTaskService.selectAiParallelTaskList(aiParallelTask);
        ExcelUtil<AiParallelTask> util = new ExcelUtil<AiParallelTask>(AiParallelTask.class);
        util.exportExcel(response, list, "多模型并行任务数据");
    }

    /**
     * 获取多模型并行任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:task:query')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        return success(aiParallelTaskService.selectAiParallelTaskByTaskId(taskId));
    }

    /**
     * 新增多模型并行任务
     */
    @PreAuthorize("@ss.hasPermi('system:task:add')")
    @Log(title = "多模型并行任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiParallelTask aiParallelTask)
    {
        return toAjax(aiParallelTaskService.insertAiParallelTask(aiParallelTask));
    }

    /**
     * 修改多模型并行任务
     */
    @PreAuthorize("@ss.hasPermi('system:task:edit')")
    @Log(title = "多模型并行任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiParallelTask aiParallelTask)
    {
        return toAjax(aiParallelTaskService.updateAiParallelTask(aiParallelTask));
    }

    /**
     * 删除多模型并行任务
     */
    @PreAuthorize("@ss.hasPermi('system:task:remove')")
    @Log(title = "多模型并行任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(aiParallelTaskService.deleteAiParallelTaskByTaskIds(taskIds));
    }
}
