package com.ruoyi.project.websocket;

import java.util.List;

/**
 * 团队消息已读事件(经 /topic/team/{teamId}/read 广播)
 * 
 * @author devpivot
 * @date 2026-08-10
 */
public class TeamReadEvent
{
    /** 团队ID */
    private Long teamId;

    /** 被标记已读的消息ID列表 */
    private List<Long> msgIds;

    /** 阅读者用户ID */
    private Long readerUserId;

    /** 阅读者昵称 */
    private String readerNickName;

    public TeamReadEvent()
    {
    }

    public TeamReadEvent(Long teamId, List<Long> msgIds, Long readerUserId, String readerNickName)
    {
        this.teamId = teamId;
        this.msgIds = msgIds;
        this.readerUserId = readerUserId;
        this.readerNickName = readerNickName;
    }

    public Long getTeamId()
    {
        return teamId;
    }

    public void setTeamId(Long teamId)
    {
        this.teamId = teamId;
    }

    public List<Long> getMsgIds()
    {
        return msgIds;
    }

    public void setMsgIds(List<Long> msgIds)
    {
        this.msgIds = msgIds;
    }

    public Long getReaderUserId()
    {
        return readerUserId;
    }

    public void setReaderUserId(Long readerUserId)
    {
        this.readerUserId = readerUserId;
    }

    public String getReaderNickName()
    {
        return readerNickName;
    }

    public void setReaderNickName(String readerNickName)
    {
        this.readerNickName = readerNickName;
    }
}
