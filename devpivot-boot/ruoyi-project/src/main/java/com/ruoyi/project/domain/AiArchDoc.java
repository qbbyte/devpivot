package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统架构设计文档对象 ai_arch_doc
 * ARCH 阶段产物：AI 依据 PRD + 原型产出系统架构设计（模块划分/核心流程/接口契约/部署架构/非功能约束），
 * 内容为含 Mermaid 图的 Markdown。
 *
 * @author devpivot
 * @date 2026-08-26
 */
public class AiArchDoc extends BaseEntity
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

    /** 文档内容(Markdown，含 Mermaid 架构图) */
    @Excel(name = "文档内容(Markdown，含Mermaid架构图)")
    private String content;

    /** 多模型生成结果及融合来源(JSON，预留) */
    @Excel(name = "多模型生成结果及融合来源(JSON)")
    private String multiSource;

    /** 状态(0草稿 1已确认) */
    @Excel(name = "状态(0草稿 1已确认)")
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

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
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
            .append("content", getContent())
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
