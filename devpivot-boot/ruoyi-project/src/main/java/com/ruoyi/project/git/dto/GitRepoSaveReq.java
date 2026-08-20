package com.ruoyi.project.git.dto;

/**
 * 保存仓库配置请求(令牌为明文,服务端加密存储)
 */
public class GitRepoSaveReq
{
    /** 仓库别名(可选,便于区分同一项目的多个仓库) */
    private String name;
    /** github / gitlab / gitee / gitea / self-hosted */
    private String platform;
    /** owner/repo 或自托管完整路径 */
    private String repoFullName;
    /** 分支,默认 main/master */
    private String repoBranch;
    /** 自托管 API base(如 https://gitlab.xxx.com/api/v4),公有云填空 */
    private String repoApiBase;
    /** 访问令牌(明文,服务端加密);为空表示不修改现有令牌 */
    private String accessToken;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getRepoBranch() { return repoBranch; }
    public void setRepoBranch(String repoBranch) { this.repoBranch = repoBranch; }
    public String getRepoApiBase() { return repoApiBase; }
    public void setRepoApiBase(String repoApiBase) { this.repoApiBase = repoApiBase; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
