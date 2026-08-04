package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 多模型并行任务对象 ai_parallel_task
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiParallelTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 任务类型(CLARIFY/PRD/TECH/DB_CHECK) */
    @Excel(name = "任务类型(CLARIFY/PRD/TECH/DB_CHECK)")
    private String taskType;

    /** 参与模型列表(JSON) */
    @Excel(name = "参与模型列表(JSON)")
    private String modelIds;

    /** 请求参数(JSON) */
    @Excel(name = "请求参数(JSON)")
    private String requestParams;

    /** 融合汇总结果(JSON) */
    @Excel(name = "融合汇总结果(JSON)")
    private String resultSummary;

    /** 差异比对结果(JSON) */
    @Excel(name = "差异比对结果(JSON)")
    private String compareResult;

    /** 任务状态(0运行中 1完成 2部分失败 3失败) */
    @Excel(name = "任务状态(0运行中 1完成 2部分失败 3失败)")
    private String status;

    /** 预估token */
    @Excel(name = "预估token")
    private Long estTokens;

    /** 实际消耗token */
    @Excel(name = "实际消耗token")
    private Long totalTokens;

    public void setTaskId(Long taskId) 
    {
        this.taskId = taskId;
    }

    public Long getTaskId() 
    {
        return taskId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setTaskType(String taskType) 
    {
        this.taskType = taskType;
    }

    public String getTaskType() 
    {
        return taskType;
    }

    public void setModelIds(String modelIds) 
    {
        this.modelIds = modelIds;
    }

    public String getModelIds() 
    {
        return modelIds;
    }

    public void setRequestParams(String requestParams) 
    {
        this.requestParams = requestParams;
    }

    public String getRequestParams() 
    {
        return requestParams;
    }

    public void setResultSummary(String resultSummary) 
    {
        this.resultSummary = resultSummary;
    }

    public String getResultSummary() 
    {
        return resultSummary;
    }

    public void setCompareResult(String compareResult) 
    {
        this.compareResult = compareResult;
    }

    public String getCompareResult() 
    {
        return compareResult;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setEstTokens(Long estTokens) 
    {
        this.estTokens = estTokens;
    }

    public Long getEstTokens() 
    {
        return estTokens;
    }

    public void setTotalTokens(Long totalTokens) 
    {
        this.totalTokens = totalTokens;
    }

    public Long getTotalTokens() 
    {
        return totalTokens;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("projectId", getProjectId())
            .append("taskType", getTaskType())
            .append("modelIds", getModelIds())
            .append("requestParams", getRequestParams())
            .append("resultSummary", getResultSummary())
            .append("compareResult", getCompareResult())
            .append("status", getStatus())
            .append("estTokens", getEstTokens())
            .append("totalTokens", getTotalTokens())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
