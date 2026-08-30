package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiTeamTask;
import com.ruoyi.project.service.IAiTeamTaskService;

/**
 * 团队任务分配 Controller(门户侧,仅登录即可访问)
 * 
 * @author devpivot
 * @date 2026-08-29
 */
@RestController
@RequestMapping("/team")
public class AiTeamTaskController extends BaseController
{
    @Autowired
    private IAiTeamTaskService taskService;

    /** 团队任务列表(支持 stage/status/assigneeId 过滤) */
    @GetMapping("/{teamId}/task")
    public AjaxResult list(@PathVariable("teamId") Long teamId,
                           @RequestParam(value = "stage", required = false) String stage,
                           @RequestParam(value = "status", required = false) String status,
                           @RequestParam(value = "assigneeId", required = false) Long assigneeId)
    {
        Long userId = SecurityUtils.getUserId();
        return success(taskService.listTasks(teamId, stage, status, assigneeId, userId));
    }

    /** 创建任务(仅 OWNER/ADMIN) */
    @PostMapping("/{teamId}/task")
    public AjaxResult create(@PathVariable("teamId") Long teamId,
                             @RequestBody AiTeamTask task)
    {
        Long operatorId = SecurityUtils.getUserId();
        task.setTeamId(teamId);
        Long id = taskService.createTask(task, operatorId);
        return success(id);
    }

    /** 编辑任务 / 改派 */
    @PutMapping("/{teamId}/task/{id}")
    public AjaxResult edit(@PathVariable("teamId") Long teamId,
                           @PathVariable("id") Long id,
                           @RequestBody AiTeamTask task)
    {
        Long operatorId = SecurityUtils.getUserId();
        task.setId(id);
        taskService.updateTask(task, operatorId);
        return success();
    }

    /** 认领任务 */
    @PostMapping("/{teamId}/task/{id}/claim")
    public AjaxResult claim(@PathVariable("teamId") Long teamId,
                           @PathVariable("id") Long id)
    {
        Long userId = SecurityUtils.getUserId();
        taskService.claimTask(teamId, id, userId);
        return success();
    }

    /** 提交复核 */
    @PostMapping("/{teamId}/task/{id}/submit")
    public AjaxResult submit(@PathVariable("teamId") Long teamId,
                             @PathVariable("id") Long id)
    {
        Long userId = SecurityUtils.getUserId();
        taskService.submitTask(teamId, id, userId);
        return success();
    }

    /** 复核通过/打回(approved=true 通过, false 打回) */
    @PostMapping("/{teamId}/task/{id}/review")
    public AjaxResult review(@PathVariable("teamId") Long teamId,
                             @PathVariable("id") Long id,
                             @RequestParam("approved") boolean approved)
    {
        Long operatorId = SecurityUtils.getUserId();
        taskService.reviewTask(teamId, id, approved, operatorId);
        return success();
    }

    /** 删除任务(仅 OWNER/ADMIN) */
    @DeleteMapping("/{teamId}/task/{id}")
    public AjaxResult delete(@PathVariable("teamId") Long teamId,
                             @PathVariable("id") Long id)
    {
        Long operatorId = SecurityUtils.getUserId();
        taskService.deleteTask(teamId, id, operatorId);
        return success();
    }
}
