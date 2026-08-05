package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 版本全链路记录对象 ai_version_record
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiVersionRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库) */
    @Excel(name = "产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)")
    private String bizType;

    /** 产物ID */
    @Excel(name = "产物ID")
    private Long bizId;

    /** 版本号(V1.0) */
    @Excel(name = "版本号(V1.0)")
    private String versionNo;

    /** 确认时完整内容快照(JSON,用于回退) */
    @Excel(name = "确认时完整内容快照(JSON,用于回退)")
    private String contentSnapshot;

    /** 修改备注 */
    @Excel(name = "修改备注")
    private String changeRemark;

    /** 生成所用模型 */
    @Excel(name = "生成所用模型")
    private String sourceModel;

    /** 生成参数(JSON) */
    @Excel(name = "生成参数(JSON)")
    private String modelParams;

    /** 版本状态(0草稿 1正式版本) */
    @Excel(name = "版本状态(0草稿 1正式版本)")
    private String status;

    public void setRecordId(Long recordId) 
    {
        this.recordId = recordId;
    }

    public Long getRecordId() 
    {
        return recordId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setBizType(String bizType) 
    {
        this.bizType = bizType;
    }

    public String getBizType() 
    {
        return bizType;
    }

    public void setBizId(Long bizId) 
    {
        this.bizId = bizId;
    }

    public Long getBizId() 
    {
        return bizId;
    }

    public void setVersionNo(String versionNo) 
    {
        this.versionNo = versionNo;
    }

    public String getVersionNo() 
    {
        return versionNo;
    }

    public void setContentSnapshot(String contentSnapshot) 
    {
        this.contentSnapshot = contentSnapshot;
    }

    public String getContentSnapshot() 
    {
        return contentSnapshot;
    }

    public void setChangeRemark(String changeRemark) 
    {
        this.changeRemark = changeRemark;
    }

    public String getChangeRemark() 
    {
        return changeRemark;
    }

    public void setSourceModel(String sourceModel) 
    {
        this.sourceModel = sourceModel;
    }

    public String getSourceModel() 
    {
        return sourceModel;
    }

    public void setModelParams(String modelParams) 
    {
        this.modelParams = modelParams;
    }

    public String getModelParams() 
    {
        return modelParams;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("projectId", getProjectId())
            .append("bizType", getBizType())
            .append("bizId", getBizId())
            .append("versionNo", getVersionNo())
            .append("contentSnapshot", getContentSnapshot())
            .append("changeRemark", getChangeRemark())
            .append("sourceModel", getSourceModel())
            .append("modelParams", getModelParams())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
