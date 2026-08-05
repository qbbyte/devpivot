package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 需求基线对象 ai_req_baseline
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiReqBaseline extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 基线ID */
    private Long baselineId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 结构化需求内容(JSON: 功能点/业务规则/角色权限/字段信息) */
    @Excel(name = "结构化需求内容(JSON: 功能点/业务规则/角色权限/字段信息)")
    private String content;

    /** 状态(0草稿 1已确认) */
    @Excel(name = "状态(0草稿 1已确认)")
    private String status;

    /** 生成模型 */
    @Excel(name = "生成模型")
    private String sourceModel;

    /** 生成参数(JSON) */
    @Excel(name = "生成参数(JSON)")
    private String modelParams;

    public void setBaselineId(Long baselineId) 
    {
        this.baselineId = baselineId;
    }

    public Long getBaselineId() 
    {
        return baselineId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("baselineId", getBaselineId())
            .append("projectId", getProjectId())
            .append("content", getContent())
            .append("status", getStatus())
            .append("sourceModel", getSourceModel())
            .append("modelParams", getModelParams())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
