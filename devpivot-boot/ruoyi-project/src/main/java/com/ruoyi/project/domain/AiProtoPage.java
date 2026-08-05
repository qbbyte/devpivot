package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 原型页面对象 ai_proto_page
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiProtoPage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 页面ID */
    private Long pageId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 页面名称 */
    @Excel(name = "页面名称")
    private String pageName;

    /** 页面说明 */
    @Excel(name = "页面说明")
    private String pageDesc;

    /** 画布布局数据(JSON: 组件树/栅格/坐标) */
    @Excel(name = "画布布局数据(JSON: 组件树/栅格/坐标)")
    private String layout;

    /** 状态(0草稿 1已确认) */
    @Excel(name = "状态(0草稿 1已确认)")
    private String status;

    /** 生成来源(人工/AI生成) */
    @Excel(name = "生成来源(人工/AI生成)")
    private String sourceModel;

    public void setPageId(Long pageId) 
    {
        this.pageId = pageId;
    }

    public Long getPageId() 
    {
        return pageId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setPageName(String pageName) 
    {
        this.pageName = pageName;
    }

    public String getPageName() 
    {
        return pageName;
    }

    public void setPageDesc(String pageDesc) 
    {
        this.pageDesc = pageDesc;
    }

    public String getPageDesc() 
    {
        return pageDesc;
    }

    public void setLayout(String layout) 
    {
        this.layout = layout;
    }

    public String getLayout() 
    {
        return layout;
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
            .append("pageId", getPageId())
            .append("projectId", getProjectId())
            .append("pageName", getPageName())
            .append("pageDesc", getPageDesc())
            .append("layout", getLayout())
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
