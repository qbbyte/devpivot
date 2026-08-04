package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI澄清问题记录对象 ai_clarify_record
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiClarifyRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景) */
    @Excel(name = "类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)")
    private String category;

    /** 问题内容 */
    @Excel(name = "问题内容")
    private String question;

    /** 用户回答 */
    @Excel(name = "用户回答")
    private String answer;

    /** 状态(0待回答 1已回答 2已跳过) */
    @Excel(name = "状态(0待回答 1已回答 2已跳过)")
    private String status;

    /** 提出该问题的模型 */
    @Excel(name = "提出该问题的模型")
    private String sourceModel;

    /** 多模型对比标记(0共识 1独有 2观点差异) */
    @Excel(name = "多模型对比标记(0共识 1独有 2观点差异)")
    private String highlightType;

    /** 语义一致命中该问题的模型列表 */
    @Excel(name = "语义一致命中该问题的模型列表")
    private String modelList;

    /** 是否勾选合并进最终清单(Y/N) */
    @Excel(name = "是否勾选合并进最终清单(Y/N)")
    private String isMerged;

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

    public void setCategory(String category) 
    {
        this.category = category;
    }

    public String getCategory() 
    {
        return category;
    }

    public void setQuestion(String question) 
    {
        this.question = question;
    }

    public String getQuestion() 
    {
        return question;
    }

    public void setAnswer(String answer) 
    {
        this.answer = answer;
    }

    public String getAnswer() 
    {
        return answer;
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

    public void setHighlightType(String highlightType) 
    {
        this.highlightType = highlightType;
    }

    public String getHighlightType() 
    {
        return highlightType;
    }

    public void setModelList(String modelList) 
    {
        this.modelList = modelList;
    }

    public String getModelList() 
    {
        return modelList;
    }

    public void setIsMerged(String isMerged) 
    {
        this.isMerged = isMerged;
    }

    public String getIsMerged() 
    {
        return isMerged;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("projectId", getProjectId())
            .append("category", getCategory())
            .append("question", getQuestion())
            .append("answer", getAnswer())
            .append("status", getStatus())
            .append("sourceModel", getSourceModel())
            .append("highlightType", getHighlightType())
            .append("modelList", getModelList())
            .append("isMerged", getIsMerged())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
