package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiTeamProjectRepo;
import com.ruoyi.project.git.dto.GitBranchInfo;
import com.ruoyi.project.git.dto.GitCommit;
import com.ruoyi.project.git.dto.GitContributor;
import com.ruoyi.project.git.dto.GitHeatmap;
import com.ruoyi.project.git.dto.GitRepoConfig;
import com.ruoyi.project.git.dto.GitRepoSaveReq;

/**
 * 团队项目 Git 仓库统计业务层(多仓库: 一个项目可关联多个仓库, 以 repoId 定位)
 */
public interface IAiTeamGitService
{
    /** 新增仓库配置(仅 OWNER/ADMIN;令牌加密存储;返回新仓库 id) */
    Long addRepo(Long teamId, Long projectId, Long operatorId, GitRepoSaveReq req);

    /** 更新仓库配置(仅 OWNER/ADMIN;令牌留空表示不修改) */
    void updateRepo(Long teamId, Long repoId, Long operatorId, GitRepoSaveReq req);

    /** 删除仓库配置(仅 OWNER/ADMIN;同时清缓存) */
    void deleteRepo(Long teamId, Long repoId, Long operatorId);

    /** 项目下的仓库列表(不含令牌,含 projectName) */
    List<AiTeamProjectRepo> listRepos(Long teamId, Long projectId, Long userId);

    /** 单个仓库配置(令牌脱敏,不暴露明文) */
    GitRepoConfig getRepoConfig(Long teamId, Long repoId, Long userId);

    /** 贡献者统计(每人提交数,映射团队成员) */
    List<GitContributor> getRepoContributors(Long teamId, Long repoId, Long userId);

    /** 提交历史(分页,映射团队成员) */
    List<GitCommit> getRepoCommits(Long teamId, Long repoId, Long userId, int page, String branch);

    /** 分支列表 + 默认分支(用于前端下拉切换) */
    GitBranchInfo getRepoBranches(Long teamId, Long repoId, Long userId);

    /** 提交热力图(过去 365 天按日聚合, Asia/Shanghai) */
    GitHeatmap getRepoHeatmap(Long teamId, Long repoId, Long userId, String branch);
}
