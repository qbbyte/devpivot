package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI澄清会话对象 ai_clarify_session
 * 
 * 说明：一个项目对应一次澄清会话，集中存储对话记录、采纳结论、保留要点与最终结论快照。
 * 与 ai_clarify_record（单条问题记录，后台管理用）相互独立，互不影响。
 * 
 * @author devpivot
 * @date 2026-08-05
 */
public class AiClarifySession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long sessionId;

    /** 项目ID（唯一） */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 对话记录(JSON数组，含 user/ai_question/model 等消息) */
    private String conversation;

    /** 采纳结论(JSON数组) */
    private String adopted;

    /** 保留要点(JSON数组) */
    private String retained;

    /** 最终澄清结论(JSON) */
    private String conclusion;

    /** 状态(0进行中 1已提交) */
    @Excel(name = "状态(0进行中 1已提交)")
    private String status;

    /** 删除标志(0存在 2删除) */
    private String delFlag;

    /** 提交时间 */
    private java.util.Date submitTime;

    public void setSessionId(Long sessionId) 
    {
        this.sessionId = sessionId;
    }

    public Long getSessionId() 
    {
        return sessionId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setConversation(String conversation) 
    {
        this.conversation = conversation;
    }

    public String getConversation() 
    {
        return conversation;
    }

    public void setAdopted(String adopted) 
    {
        this.adopted = adopted;
    }

    public String getAdopted() 
    {
        return adopted;
    }

    public void setRetained(String retained) 
    {
        this.retained = retained;
    }

    public String getRetained() 
    {
        return retained;
    }

    public void setConclusion(String conclusion) 
    {
        this.conclusion = conclusion;
    }

    public String getConclusion() 
    {
        return conclusion;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    public void setSubmitTime(java.util.Date submitTime) 
    {
        this.submitTime = submitTime;
    }

    public java.util.Date getSubmitTime() 
    {
        return submitTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("sessionId", getSessionId())
            .append("projectId", getProjectId())
            .append("conversation", getConversation())
            .append("adopted", getAdopted())
            .append("retained", getRetained())
            .append("conclusion", getConclusion())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("submitTime", getSubmitTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
