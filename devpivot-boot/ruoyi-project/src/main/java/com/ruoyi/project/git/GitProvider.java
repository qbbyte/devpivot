package com.ruoyi.project.git;

import java.util.List;
import com.ruoyi.project.git.dto.GitCommit;
import com.ruoyi.project.git.dto.GitContributor;

/**
 * Git 平台适配器统一接口(平台无关)
 */
public interface GitProvider
{
    /**
     * 提交历史(时间倒序)
     * @param apiBase 平台 API base(公有云由实现固定,自托管由调用方传入)
     * @param fullName 仓库全名 owner/repo 或自托管路径
     * @param branch 分支(空=默认分支)
     * @param token 访问令牌(明文,仅内存使用)
     * @param page 页码(从1开始)
     * @param perPage 每页条数
     */
    List<GitCommit> listCommits(String apiBase, String fullName, String branch, String token, int page, int perPage);

    /**
     * 拉取 since(ISO-8601, UTC)之后的所有提交(内部翻页, 单页 100 条, 上限 30 页防失控)。
     * 供提交热力图聚合使用。
     */
    List<GitCommit> listCommitsSince(String apiBase, String fullName, String branch, String token, String sinceIso);

    /**
     * 贡献者统计(每人提交数 / 增删行)
     */
    List<GitContributor> listContributors(String apiBase, String fullName, String token);

    /**
     * 分支列表(用于前端下拉切换)
     */
    List<String> listBranches(String apiBase, String fullName, String token);

    /**
     * 默认分支名(用于自动选择),无则返回 null
     */
    String getDefaultBranch(String apiBase, String fullName, String token);
}
