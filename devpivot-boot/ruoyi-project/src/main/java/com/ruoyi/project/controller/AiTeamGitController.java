package com.ruoyi.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.git.dto.GitRepoSaveReq;
import com.ruoyi.project.service.IAiTeamGitService;

/**
 * 团队项目 Git 仓库统计控制器(门户侧,仅登录即可访问,鉴权在 service 层)
 * 多仓库: 一个项目可关联多个仓库, 仓库级接口以 repoId 定位。
 */
@RestController
@Validated
@RequestMapping("/team/git")
public class AiTeamGitController extends BaseController
{
    @Autowired
    private IAiTeamGitService gitService;

    /** 新增仓库配置(仅 OWNER/ADMIN 可写;令牌加密存储,不回传明文) */
    @PostMapping("/{teamId}/project/{projectId}/repo")
    public AjaxResult addRepo(@PathVariable("teamId") Long teamId,
                              @PathVariable("projectId") Long projectId,
                              @RequestBody GitRepoSaveReq req)
    {
        return success(gitService.addRepo(teamId, projectId, SecurityUtils.getUserId(), req));
    }

    /** 更新仓库配置(仅 OWNER/ADMIN;令牌留空表示不修改) */
    @PutMapping("/{teamId}/repo/{repoId}")
    public AjaxResult updateRepo(@PathVariable("teamId") Long teamId,
                                 @PathVariable("repoId") Long repoId,
                                 @RequestBody GitRepoSaveReq req)
    {
        gitService.updateRepo(teamId, repoId, SecurityUtils.getUserId(), req);
        return success();
    }

    /** 删除仓库配置(仅 OWNER/ADMIN) */
    @DeleteMapping("/{teamId}/repo/{repoId}")
    public AjaxResult deleteRepo(@PathVariable("teamId") Long teamId,
                                 @PathVariable("repoId") Long repoId)
    {
        gitService.deleteRepo(teamId, repoId, SecurityUtils.getUserId());
        return success();
    }

    /** 项目下的仓库列表(不含令牌) */
    @GetMapping("/{teamId}/project/{projectId}/repos")
    public AjaxResult repos(@PathVariable("teamId") Long teamId,
                            @PathVariable("projectId") Long projectId)
    {
        return success(gitService.listRepos(teamId, projectId, SecurityUtils.getUserId()));
    }

    /** 单个仓库配置(令牌脱敏) */
    @GetMapping("/{teamId}/repo/{repoId}")
    public AjaxResult getRepo(@PathVariable("teamId") Long teamId,
                              @PathVariable("repoId") Long repoId)
    {
        return success(gitService.getRepoConfig(teamId, repoId, SecurityUtils.getUserId()));
    }

    /** 贡献者统计(每人提交数) */
    @GetMapping("/{teamId}/repo/{repoId}/contributors")
    public AjaxResult contributors(@PathVariable("teamId") Long teamId,
                                   @PathVariable("repoId") Long repoId)
    {
        return success(gitService.getRepoContributors(teamId, repoId, SecurityUtils.getUserId()));
    }

    /** 提交历史(分页) */
    @GetMapping("/{teamId}/repo/{repoId}/commits")
    public AjaxResult commits(@PathVariable("teamId") Long teamId,
                              @PathVariable("repoId") Long repoId,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "branch", required = false) String branch)
    {
        return success(gitService.getRepoCommits(teamId, repoId, SecurityUtils.getUserId(), page, branch));
    }

    /** 分支列表 + 默认分支(用于前端下拉切换) */
    @GetMapping("/{teamId}/repo/{repoId}/branches")
    public AjaxResult branches(@PathVariable("teamId") Long teamId,
                               @PathVariable("repoId") Long repoId)
    {
        return success(gitService.getRepoBranches(teamId, repoId, SecurityUtils.getUserId()));
    }

    /** 提交热力图(过去 365 天按日聚合, 用于 Gitee/GitHub 风格贡献热力图) */
    @GetMapping("/{teamId}/repo/{repoId}/heatmap")
    public AjaxResult heatmap(@PathVariable("teamId") Long teamId,
                              @PathVariable("repoId") Long repoId,
                              @RequestParam(value = "branch", required = false) String branch)
    {
        return success(gitService.getRepoHeatmap(teamId, repoId, SecurityUtils.getUserId(), branch));
    }
}
