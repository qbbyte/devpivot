package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队成员关系对象 ai_team_member
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public class AiTeamMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 团队ID */
    private Long teamId;

    /** 用户ID */
    private Long userId;

    /** 角色(OWNER创建者/ADMIN管理员/MEMBER成员) */
    private String role;

    /** 团队内职务(可空,回退sys_user) */
    private String title;

    /** 用户昵称(非数据库字段,来自sys_user) */
    private String nickName;

    /** 用户账号(非数据库字段,来自sys_user) */
    private String userName;

    /** 用户邮箱(非数据库字段,来自sys_user) */
    private String email;

    /** 用户头像地址(非数据库字段,来自sys_user) */
    private String avatar;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }

    public void setRole(String role) { this.role = role; }
    public String getRole() { return role; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getNickName() { return nickName; }

    public void setUserName(String userName) { this.userName = userName; }
    public String getUserName() { return userName; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getAvatar() { return avatar; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("teamId", getTeamId())
            .append("userId", getUserId())
            .append("role", getRole())
            .append("title", getTitle())
            .append("nickName", getNickName())
            .append("userName", getUserName())
            .append("email", getEmail())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
