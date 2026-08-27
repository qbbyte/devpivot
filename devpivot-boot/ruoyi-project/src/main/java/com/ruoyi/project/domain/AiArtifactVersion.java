package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 结果物版本对象 ai_artifact_version
 * 各阶段（CLARIFY/REQ/PRD/PROTO/TECH/DB）结果物多版本快照统一管理，支持回退/对比/发布。
 *
 * @author devpivot
 * @date 2026-08-26
 */
public class AiArtifactVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 版本ID */
    private Long versionId;

    /** 项目ID（归属分区键） */
    private Long projectId;

    /** 阶段：CLARIFY/REQ/PRD/PROTO/TECH/DB */
    private String stage;

    /** 结果物类型：默认同 stage；PROTO 细分 WEB/H5/MINI */
    private String artifactType;

    /** 结果物当前主键（对应业务表主键，可为空） */
    private Long artifactId;

    /** 版本号：阶段内自增 V1/V2/... */
    private String versionNo;

    /** 版本名称（用户命名，默认"版本 yyyy-MM-dd HH:mm"） */
    private String versionName;

    /** 完整内容快照（JSON，用于回退与对比） */
    private String snapshot;

    /** 快照 MD5（判重/快速对比） */
    private String snapshotHash;

    /** 来源版本ID（还原/分支时记录，形成版本链） */
    private Long parentVersionId;

    /** 来源：MANUAL人工/AI_GEN AI生成/PATCH AI改稿/RESTORE历史还原/TEMPLATE模板 */
    private String sourceType;

    /** 生成所用模型 code */
    private String sourceModel;

    /** 状态：DRAFT草稿/RELEASED正式/ARCHIVED归档 */
    private String status;

    /** 修改备注 */
    private String changeRemark;

    public void setVersionId(Long versionId)
    {
        this.versionId = versionId;
    }

    public Long getVersionId()
    {
        return versionId;
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

    public void setVersionNo(String versionNo)
    {
        this.versionNo = versionNo;
    }

    public String getVersionNo()
    {
        return versionNo;
    }

    public void setVersionName(String versionName)
    {
        this.versionName = versionName;
    }

    public String getVersionName()
    {
        return versionName;
    }

    public void setSnapshot(String snapshot)
    {
        this.snapshot = snapshot;
    }

    public String getSnapshot()
    {
        return snapshot;
    }

    public void setSnapshotHash(String snapshotHash)
    {
        this.snapshotHash = snapshotHash;
    }

    public String getSnapshotHash()
    {
        return snapshotHash;
    }

    public void setParentVersionId(Long parentVersionId)
    {
        this.parentVersionId = parentVersionId;
    }

    public Long getParentVersionId()
    {
        return parentVersionId;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceModel(String sourceModel)
    {
        this.sourceModel = sourceModel;
    }

    public String getSourceModel()
    {
        return sourceModel;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setChangeRemark(String changeRemark)
    {
        this.changeRemark = changeRemark;
    }

    public String getChangeRemark()
    {
        return changeRemark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("versionId", getVersionId())
            .append("projectId", getProjectId())
            .append("stage", getStage())
            .append("artifactType", getArtifactType())
            .append("artifactId", getArtifactId())
            .append("versionNo", getVersionNo())
            .append("versionName", getVersionName())
            .append("snapshotHash", getSnapshotHash())
            .append("parentVersionId", getParentVersionId())
            .append("sourceType", getSourceType())
            .append("sourceModel", getSourceModel())
            .append("status", getStatus())
            .append("changeRemark", getChangeRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
