package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队项目关联关系对象 ai_team_project
 * 
 * @author devpivot
 * @date 2026-08-09
 */
public class AiTeamProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 团队ID */
    private Long teamId;

    /** 项目ID */
    private Long projectId;

    /** 项目名称(非数据库字段,来自ai_project) */
    private String projectName;

    /** 项目阶段(非数据库字段,来自ai_project) */
    private String step;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectName() { return projectName; }

    public void setStep(String step) { this.step = step; }
    public String getStep() { return step; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("teamId", getTeamId())
            .append("projectId", getProjectId())
            .append("projectName", getProjectName())
            .append("step", getStep())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
