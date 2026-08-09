package com.ruoyi.project.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 团队消息已读记录对象 ai_team_message_read
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public class AiTeamMessageRead
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 消息ID */
    private Long msgId;

    /** 团队ID */
    private Long teamId;

    /** 已读用户ID */
    private Long userId;

    /** 已读时间 */
    private Date createTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setMsgId(Long msgId) { this.msgId = msgId; }
    public Long getMsgId() { return msgId; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getCreateTime() { return createTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("msgId", getMsgId())
            .append("teamId", getTeamId())
            .append("userId", getUserId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
