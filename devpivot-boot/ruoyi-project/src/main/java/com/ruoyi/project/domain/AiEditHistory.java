package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员修改记录对象 ai_edit_history
 * 腾讯文档式操作轨迹（append-only）：记录每位用户对版本及内容的操作（创建/编辑/删除/还原/发布…）与时间戳。
 *
 * @author devpivot
 * @date 2026-08-26
 */
public class AiEditHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long historyId;

    /** 项目ID（归属分区键） */
    private Long projectId;

    /** 阶段：CLARIFY/REQ/PRD/PROTO/TECH/DB */
    private String stage;

    /** 结果物类型（默认同 stage） */
    private String artifactType;

    /** 结果物当前主键（可为空） */
    private Long artifactId;

    /** 关联版本ID（版本级操作必填） */
    private Long versionId;

    /** 关联版本号 */
    private String versionNo;

    /** 动作：CREATE创建/UPDATE编辑/DELETE删除/RESTORE还原/RELEASE发布/EXPORT导出/ROLLBACK回退 */
    private String action;

    /** 人类可读动作描述，如"创建版本 V2" */
    private String actionDesc;

    /** 操作用户ID（sys_user.user_id） */
    private Long operatorId;

    /** 操作用户名（冗余快照，防改名/删除） */
    private String operatorName;

    /** 目标对象标签，如"页面：登录页" */
    private String targetLabel;

    /** 变更摘要JSON：{"added":n,"removed":n,"modified":n} */
    private String changeSummary;

    /** 结构化diff JSON（字段级 old/new，可选） */
    private String changeDetail;

    /** 操作IP（可选） */
    private String ip;

    /** 客户端标识（可选） */
    private String client;

    public void setHistoryId(Long historyId)
    {
        this.historyId = historyId;
    }

    public Long getHistoryId()
    {
        return historyId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setStage(String stage)
    {
        this.stage = stage;
    }

    public String getStage()
    {
        return stage;
    }

    public void setArtifactType(String artifactType)
    {
        this.artifactType = artifactType;
    }

    public String getArtifactType()
    {
        return artifactType;
    }

    public void setArtifactId(Long artifactId)
    {
        this.artifactId = artifactId;
    }

    public Long getArtifactId()
    {
        return artifactId;
    }

    public void setVersionId(Long versionId)
    {
        this.versionId = versionId;
    }

    public Long getVersionId()
    {
        return versionId;
    }

    public void setVersionNo(String versionNo)
    {
        this.versionNo = versionNo;
    }

    public String getVersionNo()
    {
        return versionNo;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    public void setActionDesc(String actionDesc)
    {
        this.actionDesc = actionDesc;
    }

    public String getActionDesc()
    {
        return actionDesc;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setTargetLabel(String targetLabel)
    {
        this.targetLabel = targetLabel;
    }

    public String getTargetLabel()
    {
        return targetLabel;
    }

    public void setChangeSummary(String changeSummary)
    {
        this.changeSummary = changeSummary;
    }

    public String getChangeSummary()
    {
        return changeSummary;
    }

    public void setChangeDetail(String changeDetail)
    {
        this.changeDetail = changeDetail;
    }

    public String getChangeDetail()
    {
        return changeDetail;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getIp()
    {
        return ip;
    }

    public void setClient(String client)
    {
        this.client = client;
    }

    public String getClient()
    {
        return client;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("historyId", getHistoryId())
            .append("projectId", getProjectId())
            .append("stage", getStage())
            .append("artifactType", getArtifactType())
            .append("artifactId", getArtifactId())
            .append("versionId", getVersionId())
            .append("versionNo", getVersionNo())
            .append("action", getAction())
            .append("actionDesc", getActionDesc())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("targetLabel", getTargetLabel())
            .append("changeSummary", getChangeSummary())
            .append("changeDetail", getChangeDetail())
            .append("ip", getIp())
            .append("client", getClient())
            .append("createTime", getCreateTime())
            .toString();
    }
}
