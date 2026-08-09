package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMessage;

/**
 * 团队模块业务层
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public interface IAiTeamService
{
    /** 我的团队列表 */
    List<AiTeam> listMyTeams(Long userId);

    /** 团队详情(聚合成员/项目/消息/已读) */
    AiTeam getTeamDetail(Long teamId, Long userId);

    /** 创建团队(自动添加创建者为 OWNER) */
    Long createTeam(AiTeam team, Long userId, String username);

    /** 编辑团队(仅名称/简介) */
    void updateTeam(AiTeam team, Long userId);

    /** 解散团队(仅 OWNER) */
    void dissolveTeam(Long teamId, Long userId);

    /** 添加成员 */
    void addMember(Long teamId, Long targetUserId, String role, Long operatorId);

    /** 移除成员 */
    void removeMember(Long teamId, Long targetUserId, Long operatorId);

    /** 修改成员角色 */
    void changeMemberRole(Long teamId, Long targetUserId, String role, Long operatorId);

    /** 关联项目 */
    void bindProject(Long teamId, Long projectId, Long operatorId);

    /** 解绑项目 */
    void unbindProject(Long teamId, Long projectId, Long operatorId);

    /** 发送讨论消息 */
    AiTeamMessage sendMessage(Long teamId, Long userId, String content);

    /** 标记已读(不传 msgIds 则标记全部未读) */
    void markRead(Long teamId, Long userId, List<Long> msgIds);

    /** 检索平台用户目录 */
    List<Map<String, Object>> searchUsers(String keyword);

    /** 退出团队(非创建者主动退出;创建者需先转移所有权或解散) */
    void leaveTeam(Long teamId, Long userId);

    /** 项目下拉选项(供团队关联项目选择器) */
    List<Map<String, Object>> listProjectOptions();

    /** 轻量拉取团队消息(含已读人聚合,用于讨论区轮询刷新) */
    List<AiTeamMessage> listMessages(Long teamId, Long userId);
}
