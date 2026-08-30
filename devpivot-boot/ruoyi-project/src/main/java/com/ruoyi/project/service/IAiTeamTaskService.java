package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiTeamTask;

/**
 * 团队任务分配业务层
 * 
 * @author devpivot
 * @date 2026-08-29
 */
public interface IAiTeamTaskService
{
    /** 团队任务列表(可按 stage/status/assigneeId 过滤) */
    List<AiTeamTask> listTasks(Long teamId, String stage, String status, Long assigneeId, Long userId);

    /** 创建任务(仅 OWNER/ADMIN) */
    Long createTask(AiTeamTask task, Long operatorId);

    /** 编辑任务(标题/描述/阶段/优先级/截止时间); 改派需 manager */
    void updateTask(AiTeamTask task, Long operatorId);

    /** 认领待办(仅本人, 且任务未分配) */
    void claimTask(Long teamId, Long taskId, Long userId);

    /** 提交复核(仅负责人本人) */
    void submitTask(Long teamId, Long taskId, Long userId);

    /** 复核通过/打回(仅 OWNER/ADMIN) */
    void reviewTask(Long teamId, Long taskId, boolean approved, Long operatorId);

    /** 删除任务(仅 OWNER/ADMIN) */
    void deleteTask(Long teamId, Long taskId, Long operatorId);
}
