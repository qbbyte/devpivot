package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 原型历史版本对象 ai_proto_version
 * 存储某一时刻原型全部页面+组件的 JSON 快照，用于回退/对比。
 *
 * @author devpivot
 * @date 2026-08-07
 */
public class AiProtoVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 版本ID */
    private Long versionId;

    /** 项目ID */
    private Long projectId;

    /** 版本名称 */
    private String versionName;

    /** 设备类型(WEB网页端/H5移动端/MINI小程序) */
    private String deviceType;

    /** 生成来源(人工/AI生成) */
    private String sourceModel;

    /** 原型快照(JSON: 完整 pages 数组) */
    private String snapshot;

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

    public void setVersionName(String versionName)
    {
        this.versionName = versionName;
    }

    public String getVersionName()
    {
        return versionName;
    }

    public void setDeviceType(String deviceType)
    {
        this.deviceType = deviceType;
    }

    public String getDeviceType()
    {
        return deviceType;
    }

    public void setSourceModel(String sourceModel)
    {
        this.sourceModel = sourceModel;
    }

    public String getSourceModel()
    {
        return sourceModel;
    }

    public void setSnapshot(String snapshot)
    {
        this.snapshot = snapshot;
    }

    public String getSnapshot()
    {
        return snapshot;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("versionId", getVersionId())
            .append("projectId", getProjectId())
            .append("versionName", getVersionName())
            .append("deviceType", getDeviceType())
            .append("sourceModel", getSourceModel())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
