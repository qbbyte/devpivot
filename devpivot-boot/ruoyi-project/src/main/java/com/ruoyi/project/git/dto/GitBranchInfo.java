package com.ruoyi.project.git.dto;

import java.util.List;

/**
 * 分支信息(用于前端下拉切换): 默认分支 + 分支列表
 */
public class GitBranchInfo
{
    /** 默认分支名, 无则为空串 */
    private String defaultBranch = "";
    /** 分支名列表 */
    private List<String> branches;

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch == null ? "" : defaultBranch; }
    public List<String> getBranches() { return branches; }
    public void setBranches(List<String> branches) { this.branches = branches; }
}
