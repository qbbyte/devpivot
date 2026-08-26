package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamProject;
import com.ruoyi.project.domain.AiTeamProjectRepo;
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

    /** 按邀请码查团队(用于邀请链接加入) */
    AiTeam selectByInviteCode(@Param("inviteCode") String inviteCode);

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

    /** 仅更新邀请码(重新生成用) */
    int updateInviteCode(@Param("teamId") Long teamId, @Param("inviteCode") String inviteCode,
                         @Param("updateBy") String updateBy, @Param("updateTime") java.util.Date updateTime);
    int updateMemberRole(@Param("teamId") Long teamId, @Param("userId") Long userId,
                         @Param("role") String role, @Param("updateBy") String updateBy,
                         @Param("updateTime") java.util.Date updateTime);
    int dissolveTeam(Long teamId);

    int deleteMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
    int deleteProject(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    AiTeamMember selectMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
    int countMembersByRole(@Param("teamId") Long teamId, @Param("role") String role);
    int existsProject(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    /** 按项目反查已绑定团队ID列表(供项目级访问控制 ProjectAccessService 使用) */
    List<Long> selectTeamIdsByProjectId(Long projectId);

    /** 更新项目关联的 Git 仓库配置(平台/全名/分支/API base/加密令牌) */
    int updateProjectRepo(AiTeamProject project);

    /** 取项目关联的 Git 仓库配置(不含令牌,列表/展示用) */
    AiTeamProject selectProjectRepo(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    /** 取项目关联仓库的加密令牌(内部解密用,仅 service 调用) */
    String selectProjectAccessToken(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    /** 取当前用户在某团队内尚未读的消息ID(排除自己发送 + 已读记录) */
    List<Long> selectUnreadMessageIds(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("msgIds") List<Long> msgIds);

    /* ==================== 多仓库: ai_team_project_repo ==================== */

    /** 新增仓库配置(返回自增 id) */
    int insertProjectRepo(AiTeamProjectRepo repo);

    /** 更新仓库配置(不含令牌,令牌单独处理) */
    int updateProjectRepoById(AiTeamProjectRepo repo);

    /** 更新仓库令牌(独立方法,避免覆盖其他字段) */
    int updateRepoToken(@Param("repoId") Long repoId, @Param("accessToken") String accessToken,
                        @Param("updateBy") String updateBy, @Param("updateTime") java.util.Date updateTime);

    /** 删除仓库配置 */
    int deleteProjectRepo(@Param("repoId") Long repoId);

    /** 项目下的仓库列表(不含令牌) */
    List<AiTeamProjectRepo> selectReposByProject(@Param("teamId") Long teamId, @Param("projectId") Long projectId);

    /** 按 id 取仓库(不含令牌) */
    AiTeamProjectRepo selectRepoById(@Param("repoId") Long repoId);

    /** 按 id 取加密令牌(内部解密用) */
    String selectRepoTokenById(@Param("repoId") Long repoId);

    /** 仓库是否属于该团队(鉴权用) */
    int countRepoByTeam(@Param("repoId") Long repoId, @Param("teamId") Long teamId);
}
