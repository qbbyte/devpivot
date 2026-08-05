package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI项目对象 ai_project
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 行业分类 */
    @Excel(name = "行业分类")
    private String industryType;

    /** 项目简介 */
    @Excel(name = "项目简介")
    private String projectIntro;

    /** 目标用户群体 */
    @Excel(name = "目标用户群体")
    private String targetUser;

    /** 目标数据库类型(MySQL/PostgreSQL) */
    @Excel(name = "目标数据库类型(MySQL/PostgreSQL)")
    private String dbType;

    /** 默认模型策略(JSON: 默认模型/是否多模型/并行数量与名单) */
    @Excel(name = "默认模型策略(JSON: 默认模型/是否多模型/并行数量与名单)")
    private String modelStrategy;

    /** 项目进度阶段(REQ需求/CLARIFY澄清/PRD原型/TECH技术/DB库表/DONE完成) */
    @Excel(name = "项目进度阶段(REQ需求/CLARIFY澄清/PRD原型/TECH技术/DB库表/DONE完成)")
    private String step;

    /** 当前负责人ID */
    private Long assigneeId;

    /** 负责人昵称（非数据库字段） */
    private String assigneeName;

    /** 是否置顶(Y/N) */
    @Excel(name = "是否置顶(Y/N)")
    private String isTop;

    /** 项目状态(0正常 1归档) */
    @Excel(name = "项目状态(0正常 1归档)")
    private String status;

    /** 删除标志(0存在 2删除) */
    private String delFlag;

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setProjectName(String projectName) 
    {
        this.projectName = projectName;
    }

    public String getProjectName() 
    {
        return projectName;
    }

    public void setIndustryType(String industryType) 
    {
        this.industryType = industryType;
    }

    public String getIndustryType() 
    {
        return industryType;
    }

    public void setProjectIntro(String projectIntro) 
    {
        this.projectIntro = projectIntro;
    }

    public String getProjectIntro() 
    {
        return projectIntro;
    }

    public void setTargetUser(String targetUser) 
    {
        this.targetUser = targetUser;
    }

    public String getTargetUser() 
    {
        return targetUser;
    }

    public void setDbType(String dbType) 
    {
        this.dbType = dbType;
    }

    public String getDbType() 
    {
        return dbType;
    }

    public void setModelStrategy(String modelStrategy) 
    {
        this.modelStrategy = modelStrategy;
    }

    public String getModelStrategy() 
    {
        return modelStrategy;
    }

    public void setStep(String step) 
    {
        this.step = step;
    }

    public String getStep() 
    {
        return step;
    }

    public void setAssigneeId(Long assigneeId) 
    {
        this.assigneeId = assigneeId;
    }

    public Long getAssigneeId() 
    {
        return assigneeId;
    }

    public void setAssigneeName(String assigneeName) 
    {
        this.assigneeName = assigneeName;
    }

    public String getAssigneeName() 
    {
        return assigneeName;
    }

    public void setIsTop(String isTop) 
    {
        this.isTop = isTop;
    }

    public String getIsTop() 
    {
        return isTop;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("projectName", getProjectName())
            .append("industryType", getIndustryType())
            .append("projectIntro", getProjectIntro())
            .append("targetUser", getTargetUser())
            .append("dbType", getDbType())
            .append("modelStrategy", getModelStrategy())
            .append("step", getStep())
            .append("isTop", getIsTop())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
