package com.ruoyi.project.domain;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队讨论消息对象 ai_team_message
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public class AiTeamMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long msgId;

    /** 团队ID */
    private Long teamId;

    /** 发送者用户ID */
    private Long userId;

    /** 消息内容 */
    private String content;

    /** 发送者昵称(非数据库字段,来自sys_user) */
    private String nickName;

    /** 发送者头像地址(非数据库字段,来自sys_user) */
    private String avatar;

    /** 格式化发送时间(非数据库字段,例如 HH:mm 或 MM-dd HH:mm) */
    private String time;

    /** 已读人列表(非数据库字段,排除当前用户) */
    private List<Map<String, Object>> readUsers;

    public void setMsgId(Long msgId) { this.msgId = msgId; }
    public Long getMsgId() { return msgId; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }

    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }

    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getNickName() { return nickName; }

    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getAvatar() { return avatar; }

    public void setTime(String time) { this.time = time; }
    public String getTime() { return time; }

    public void setReadUsers(List<Map<String, Object>> readUsers) { this.readUsers = readUsers; }
    public List<Map<String, Object>> getReadUsers() { return readUsers; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("msgId", getMsgId())
            .append("teamId", getTeamId())
            .append("userId", getUserId())
            .append("content", getContent())
            .append("nickName", getNickName())
            .append("time", getTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
