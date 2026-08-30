package com.ruoyi.project.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队任务分配对象 ai_team_task
 * 
 * @author devpivot
 * @date 2026-08-29
 */
public class AiTeamTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务主键 */
    private Long id;

    /** 团队ID */
    private Long teamId;

    /** 关联项目ID(可空) */
    private Long projectId;

    /** 关联流水线阶段: REQ/CLARIFY/PRD/PROTO/TECH/DB */
    private String stage;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 负责人用户ID */
    private Long assigneeId;

    /** 优先级: LOW/MEDIUM/HIGH */
    private String priority;

    /** 任务状态: TODO/DOING/REVIEW/DONE */
    private String status;

    /** 截止时间 */
    private Date dueAt;

    /** 负责人昵称(非数据库字段,列表展示用) */
    private String assigneeName;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setStage(String stage) { this.stage = stage; }
    public String getStage() { return stage; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getAssigneeId() { return assigneeId; }

    public void setPriority(String priority) { this.priority = priority; }
    public String getPriority() { return priority; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    public void setDueAt(Date dueAt) { this.dueAt = dueAt; }
    public Date getDueAt() { return dueAt; }

    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getAssigneeName() { return assigneeName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("teamId", getTeamId())
            .append("projectId", getProjectId())
            .append("stage", getStage())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("assigneeId", getAssigneeId())
            .append("priority", getPriority())
            .append("status", getStatus())
            .append("dueAt", getDueAt())
            .append("assigneeName", getAssigneeName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
