package com.ruoyi.ai.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI模型调用日志对象 ai_model_call_log
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiModelCallLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 并行任务ID(单模型为空) */
    @Excel(name = "并行任务ID(单模型为空)")
    private Long taskId;

    /** 场景类型 */
    @Excel(name = "场景类型")
    private String taskType;

    /** 模型ID */
    @Excel(name = "模型ID")
    private Long modelId;

    /** 输入token */
    @Excel(name = "输入token")
    private Long reqTokens;

    /** 输出token */
    @Excel(name = "输出token")
    private Long respTokens;

    /** 费用 */
    @Excel(name = "费用")
    private BigDecimal cost;

    /** 是否命中缓存(Y/N) */
    @Excel(name = "是否命中缓存(Y/N)")
    private String cacheHit;

    /** 状态(0成功 1失败 2超时 3降级) */
    @Excel(name = "状态(0成功 1失败 2超时 3降级)")
    private String status;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMsg;

    /** 耗时(毫秒) */
    @Excel(name = "耗时(毫秒)")
    private Long consumeMs;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    public void setLogId(Long logId) 
    {
        this.logId = logId;
    }

    public Long getLogId() 
    {
        return logId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setTaskId(Long taskId) 
    {
        this.taskId = taskId;
    }

    public Long getTaskId() 
    {
        return taskId;
    }

    public void setTaskType(String taskType) 
    {
        this.taskType = taskType;
    }

    public String getTaskType() 
    {
        return taskType;
    }

    public void setModelId(Long modelId) 
    {
        this.modelId = modelId;
    }

    public Long getModelId() 
    {
        return modelId;
    }

    public void setReqTokens(Long reqTokens) 
    {
        this.reqTokens = reqTokens;
    }

    public Long getReqTokens() 
    {
        return reqTokens;
    }

    public void setRespTokens(Long respTokens) 
    {
        this.respTokens = respTokens;
    }

    public Long getRespTokens() 
    {
        return respTokens;
    }

    public void setCost(BigDecimal cost) 
    {
        this.cost = cost;
    }

    public BigDecimal getCost() 
    {
        return cost;
    }

    public void setCacheHit(String cacheHit) 
    {
        this.cacheHit = cacheHit;
    }

    public String getCacheHit() 
    {
        return cacheHit;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setErrorMsg(String errorMsg) 
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() 
    {
        return errorMsg;
    }

    public void setConsumeMs(Long consumeMs) 
    {
        this.consumeMs = consumeMs;
    }

    public Long getConsumeMs() 
    {
        return consumeMs;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("projectId", getProjectId())
            .append("taskId", getTaskId())
            .append("taskType", getTaskType())
            .append("modelId", getModelId())
            .append("reqTokens", getReqTokens())
            .append("respTokens", getRespTokens())
            .append("cost", getCost())
            .append("cacheHit", getCacheHit())
            .append("status", getStatus())
            .append("errorMsg", getErrorMsg())
            .append("consumeMs", getConsumeMs())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
