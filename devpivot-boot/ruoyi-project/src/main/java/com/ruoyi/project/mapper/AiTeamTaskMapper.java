package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiTeamTask;
import org.apache.ibatis.annotations.Param;

/**
 * 团队任务分配数据访问层
 * 
 * @author devpivot
 * @date 2026-08-29
 */
public interface AiTeamTaskMapper
{
    /** 按团队查询任务列表(可选 stage/status/assigneeId 过滤), 关联 sys_user 取负责人昵称 */
    List<AiTeamTask> selectTaskList(@Param("teamId") Long teamId,
                                    @Param("stage") String stage,
                                    @Param("status") String status,
                                    @Param("assigneeId") Long assigneeId);

    /** 按主键查询 */
    AiTeamTask selectTaskById(@Param("id") Long id);

    int insertTask(AiTeamTask task);

    int updateTask(AiTeamTask task);

    int deleteTask(@Param("id") Long id);
}
