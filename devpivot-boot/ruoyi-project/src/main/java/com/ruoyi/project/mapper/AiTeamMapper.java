package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamProject;
import com.ruoyi.project.domain.AiTeamMessage;
import com.ruoyi.project.domain.AiTeamMessageRead;
import org.apache.ibatis.annotations.Param;

/**
 * 团队模块数据访问层
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public interface AiTeamMapper
{
    /** 我的团队列表(带当前用户角色 myRole 与成员数 memberCount) */
    List<AiTeam> selectMyTeams(@Param("userId") Long userId);

    /** 团队详情基础(带 myRole 与 memberCount) */
    AiTeam selectTeamDetail(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /** 团队成员列表(关联 sys_user 取昵称/账号/邮箱) */
    List<AiTeamMember> selectMembersByTeamId(Long teamId);

    /** 团队关联项目列表(关联 ai_project 取名称/阶段) */
    List<AiTeamProject> selectProjectsByTeamId(Long teamId);

    /** 团队消息列表(关联 sys_user 取发送者昵称) */
    List<AiTeamMessage> selectMessagesByTeamId(Long teamId);

    /** 团队内已读记录(排除当前用户,用于"发送人看已读人") */
    List<Map<String, Object>> selectReadersByTeamId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /** 平台用户目录检索(查 sys_user) */
    List<Map<String, Object>> searchSysUser(@Param("keyword") String keyword);

    /** 项目下拉选项(供团队关联项目选择器,返回 projectId/projectName) */
    List<Map<String, Object>> selectProjectOptions();

    int insertTeam(AiTeam team);
    int insertMember(AiTeamMember member);
    int insertProject(AiTeamProject project);
    int insertMessage(AiTeamMessage message);
    int insertReadIgnore(@Param("list") List<AiTeamMessageRead> list);

    int updateTeam(AiTeam team);
    int updateMemberRole(@Param("teamId") Long teamId, @Param("userId") Long userId,
                         @Param("role") String role, @Param("updateBy") String updateBy,
                         @Param("updateTime") java.util.Date updateTime);
    int dissolveTeam(Long teamId);

    int deleteMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
    int deleteProject(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    AiTeamMember selectMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
    int countMembersByRole(@Param("teamId") Long teamId, @Param("role") String role);
    int existsProject(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    /** 取当前用户在某团队内尚未读的消息ID(排除自己发送 + 已读记录) */
    List<Long> selectUnreadMessageIds(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("msgIds") List<Long> msgIds);
}
