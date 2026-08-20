package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 团队项目 Git 仓库对象 ai_team_project_repo
 * 一个项目可关联多个 Git 仓库，仓库配置独立成表。
 * 
 * @author devpivot
 * @date 2026-08-20
 */
public class AiTeamProjectRepo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 团队ID */
    private Long teamId;

    /** 项目ID */
    private Long projectId;

    /** 仓库别名(可选,便于区分同一项目的多个仓库) */
    private String name;

    /** 仓库平台: github / gitlab / gitee / gitea / self-hosted */
    private String platform;

    /** 仓库全名 owner/repo 或自托管完整路径 */
    private String repoFullName;

    /** 分支(默认 main/master) */
    private String repoBranch;

    /** 自托管平台自定义 API base URL(公有云平台填空) */
    private String repoApiBase;

    /** 访问令牌(加密存储 ENC:...，不回传前端，不进 toString) */
    private String accessToken;

    /** 项目名称(非数据库字段,来自 ai_project,列表展示用) */
    private String projectName;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getTeamId() { return teamId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setPlatform(String platform) { this.platform = platform; }
    public String getPlatform() { return platform; }

    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getRepoFullName() { return repoFullName; }

    public void setRepoBranch(String repoBranch) { this.repoBranch = repoBranch; }
    public String getRepoBranch() { return repoBranch; }

    public void setRepoApiBase(String repoApiBase) { this.repoApiBase = repoApiBase; }
    public String getRepoApiBase() { return repoApiBase; }

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getAccessToken() { return accessToken; }

    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectName() { return projectName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("teamId", getTeamId())
            .append("projectId", getProjectId())
            .append("name", getName())
            .append("platform", getPlatform())
            .append("repoFullName", getRepoFullName())
            .append("repoBranch", getRepoBranch())
            .append("repoApiBase", getRepoApiBase())
            .append("projectName", getProjectName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
