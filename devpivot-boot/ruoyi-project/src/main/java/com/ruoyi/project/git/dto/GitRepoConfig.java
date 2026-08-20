package com.ruoyi.project.git.dto;

/**
 * 仓库配置(返回前端,令牌脱敏,不暴露明文)
 */
public class GitRepoConfig
{
    /** 仓库 id(多仓库下前端据此定位) */
    private Long repoId;
    /** 仓库别名(可选) */
    private String name;
    /** 是否已配置仓库 */
    private boolean configured;
    private String platform;
    private String repoFullName;
    private String repoBranch;
    private String repoApiBase;
    /** 脱敏令牌(后4位),未配置为空 */
    private String maskedToken;

    public Long getRepoId() { return repoId; }
    public void setRepoId(Long repoId) { this.repoId = repoId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getRepoBranch() { return repoBranch; }
    public void setRepoBranch(String repoBranch) { this.repoBranch = repoBranch; }
    public String getRepoApiBase() { return repoApiBase; }
    public void setRepoApiBase(String repoApiBase) { this.repoApiBase = repoApiBase; }
    public String getMaskedToken() { return maskedToken; }
    public void setMaskedToken(String maskedToken) { this.maskedToken = maskedToken; }
}
