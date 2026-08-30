package com.ruoyi.project.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamTask;
import com.ruoyi.project.mapper.AiTeamMapper;
import com.ruoyi.project.mapper.AiTeamTaskMapper;
import com.ruoyi.project.service.IAiTeamTaskService;

/**
 * 团队任务分配业务层实现
 * 
 * @author devpivot
 * @date 2026-08-29
 */
@Service
public class AiTeamTaskServiceImpl implements IAiTeamTaskService
{
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String STATUS_TODO = "TODO";
    private static final String STATUS_DOING = "DOING";
    private static final String STATUS_REVIEW = "REVIEW";
    private static final String STATUS_DONE = "DONE";

    @Autowired
    private AiTeamTaskMapper taskMapper;

    @Autowired
    private AiTeamMapper teamMapper;

    @Override
    public List<AiTeamTask> listTasks(Long teamId, String stage, String status, Long assigneeId, Long userId)
    {
        assertMember(teamId, userId);
        return taskMapper.selectTaskList(teamId, stage, status, assigneeId);
    }

    @Override
    public Long createTask(AiTeamTask task, Long operatorId)
    {
        assertManager(task.getTeamId(), operatorId);
        if (task.getStatus() == null || task.getStatus().isEmpty())
        {
            task.setStatus(STATUS_TODO);
        }
        if (task.getPriority() == null || task.getPriority().isEmpty())
        {
            task.setPriority("MEDIUM");
        }
        Date now = DateUtils.getNowDate();
        task.setCreateBy(SecurityUtils.getUsername());
        task.setCreateTime(now);
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(now);
        taskMapper.insertTask(task);
        return task.getId();
    }

    @Override
    public void updateTask(AiTeamTask task, Long operatorId)
    {
        AiTeamTask existing = taskMapper.selectTaskById(task.getId());
        if (existing == null)
        {
            throw new ServiceException("任务不存在");
        }
        // 负责人变更: 改派他人需管理员; 自行认领(从空到本人)成员即可
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(existing.getAssigneeId()))
        {
            boolean selfClaim = existing.getAssigneeId() == null && task.getAssigneeId().equals(operatorId);
            if (!selfClaim)
            {
                assertManager(existing.getTeamId(), operatorId);
            }
        }
        else
        {
            // 普通字段编辑: 管理员或负责人本人
            AiTeamMember me = assertMember(existing.getTeamId(), operatorId);
            boolean isManager = ROLE_OWNER.equals(me.getRole()) || ROLE_ADMIN.equals(me.getRole());
            boolean isOwner = existing.getAssigneeId() != null && existing.getAssigneeId().equals(operatorId);
            if (!isManager && !isOwner)
            {
                throw new ServiceException("无操作权限");
            }
        }
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        taskMapper.updateTask(task);
    }

    @Override
    public void claimTask(Long teamId, Long taskId, Long userId)
    {
        AiTeamTask task = taskMapper.selectTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        if (!teamId.equals(task.getTeamId()))
        {
            throw new ServiceException("任务不属于该团队");
        }
        assertMember(teamId, userId);
        if (task.getAssigneeId() != null)
        {
            throw new ServiceException("该任务已被认领");
        }
        if (!STATUS_TODO.equals(task.getStatus()))
        {
            throw new ServiceException("仅待办状态可认领");
        }
        task.setAssigneeId(userId);
        task.setStatus(STATUS_DOING);
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        taskMapper.updateTask(task);
    }

    @Override
    public void submitTask(Long teamId, Long taskId, Long userId)
    {
        AiTeamTask task = taskMapper.selectTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        if (!teamId.equals(task.getTeamId()))
        {
            throw new ServiceException("任务不属于该团队");
        }
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(userId))
        {
            throw new ServiceException("仅负责人本人可提交复核");
        }
        if (!STATUS_DOING.equals(task.getStatus()))
        {
            throw new ServiceException("仅进行中状态可提交");
        }
        task.setStatus(STATUS_REVIEW);
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        taskMapper.updateTask(task);
    }

    @Override
    public void reviewTask(Long teamId, Long taskId, boolean approved, Long operatorId)
    {
        AiTeamTask task = taskMapper.selectTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        if (!teamId.equals(task.getTeamId()))
        {
            throw new ServiceException("任务不属于该团队");
        }
        assertManager(teamId, operatorId);
        if (!STATUS_REVIEW.equals(task.getStatus()))
        {
            throw new ServiceException("当前状态不可复核");
        }
        task.setStatus(approved ? STATUS_DONE : STATUS_DOING);
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        taskMapper.updateTask(task);
    }

    @Override
    public void deleteTask(Long teamId, Long taskId, Long operatorId)
    {
        AiTeamTask task = taskMapper.selectTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在");
        }
        if (!teamId.equals(task.getTeamId()))
        {
            throw new ServiceException("任务不属于该团队");
        }
        assertManager(teamId, operatorId);
        taskMapper.deleteTask(taskId);
    }

    private AiTeamMember assertMember(Long teamId, Long userId)
    {
        AiTeamMember me = teamMapper.selectMember(teamId, userId);
        if (me == null)
        {
            throw new ServiceException("您不是该团队成员");
        }
        return me;
    }

    private void assertManager(Long teamId, Long userId)
    {
        AiTeamMember me = assertMember(teamId, userId);
        if (!ROLE_OWNER.equals(me.getRole()) && !ROLE_ADMIN.equals(me.getRole()))
        {
            throw new ServiceException("无操作权限(仅管理员/创建者可操作)");
        }
    }
}
