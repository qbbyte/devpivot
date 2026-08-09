package com.ruoyi.project.domain;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI团队对象 ai_team
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public class AiTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 团队ID */
    private Long teamId;

    /** 团队名称 */
    private String teamName;

    /** 团队简介 */
    private String description;

    /** 创建者用户ID */
    private Long ownerId;

    /** 状态(0正常 1解散) */
    private String status;

    /** 删除标志(0存在 2删除) */
    private String delFlag;

    /** 当前用户在团队中的角色(非数据库字段) */
    private String myRole;

    /** 成员数(非数据库字段) */
    private Integer memberCount;

    /** 当前用户未读消息数(非数据库字段) */
    private Integer unreadCount;

    /** 成员列表(非数据库字段,详情返回) */
    private List<AiTeamMember> members;

    /** 关联项目列表(非数据库字段,详情返回) */
    private List<AiTeamProject> projects;

    /** 讨论消息列表(非数据库字段,详情返回) */
    private List<AiTeamMessage> messages;

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamName() { return teamName; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getOwnerId() { return ownerId; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getDelFlag() { return delFlag; }

    public void setMyRole(String myRole) { this.myRole = myRole; }
    public String getMyRole() { return myRole; }

    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public Integer getMemberCount() { return memberCount; }

    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
    public Integer getUnreadCount() { return unreadCount; }

    public void setMembers(List<AiTeamMember> members) { this.members = members; }
    public List<AiTeamMember> getMembers() { return members; }

    public void setProjects(List<AiTeamProject> projects) { this.projects = projects; }
    public List<AiTeamProject> getProjects() { return projects; }

    public void setMessages(List<AiTeamMessage> messages) { this.messages = messages; }
    public List<AiTeamMessage> getMessages() { return messages; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("teamId", getTeamId())
            .append("teamName", getTeamName())
            .append("description", getDescription())
            .append("ownerId", getOwnerId())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("myRole", getMyRole())
            .append("memberCount", getMemberCount())
            .append("unreadCount", getUnreadCount())
            .append("members", getMembers())
            .append("projects", getProjects())
            .append("messages", getMessages())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
