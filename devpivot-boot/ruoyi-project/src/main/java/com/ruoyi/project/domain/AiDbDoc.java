package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 数据库设计文档对象 ai_db_doc
 * 
 * @author ruoyi
 * @date 2026-08-08
 */
public class AiDbDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 文档标题 */
    @Excel(name = "文档标题")
    private String docName;

    /** 数据库类型(MySQL/PostgreSQL/Redis等) */
    @Excel(name = "数据库类型")
    private String dbType;

    /** 文档内容(Markdown) */
    @Excel(name = "文档内容")
    private String content;

    /** 多模型对比差异结果(JSON) */
    @Excel(name = "多模型对比差异结果")
    private String diffResult;

    /** 各模型生成结果及融合来源(JSON) */
    @Excel(name = "各模型生成结果及融合来源")
    private String multiSource;

    /** 状态(0草稿 1已确认) */
    @Excel(name = "状态")
    private String status;

    /** 生成模型 */
    @Excel(name = "生成模型")
    private String sourceModel;

    public void setDocId(Long docId) 
    {
        this.docId = docId;
    }

    public Long getDocId() 
    {
        return docId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setDocName(String docName) 
    {
        this.docName = docName;
    }

    public String getDocName() 
    {
        return docName;
    }

    public void setDbType(String dbType) 
    {
        this.dbType = dbType;
    }

    public String getDbType() 
    {
        return dbType;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setDiffResult(String diffResult) 
    {
        this.diffResult = diffResult;
    }

    public String getDiffResult() 
    {
        return diffResult;
    }

    public void setMultiSource(String multiSource) 
    {
        this.multiSource = multiSource;
    }

    public String getMultiSource() 
    {
        return multiSource;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("projectId", getProjectId())
            .append("docName", getDocName())
            .append("dbType", getDbType())
            .append("content", getContent())
            .append("diffResult", getDiffResult())
            .append("multiSource", getMultiSource())
            .append("status", getStatus())
            .append("sourceModel", getSourceModel())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
