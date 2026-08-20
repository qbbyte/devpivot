package com.ruoyi.project.git.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.project.git.AbstractGitProvider;
import com.ruoyi.project.git.dto.GitCommit;
import com.ruoyi.project.git.dto.GitContributor;

/**
 * GitHub 适配器(公有云 api.github.com)。
 * commits 含 stats(增删行); contributors 仅返回 login + 提交数(无 email)。
 */
public class GitHubProvider extends AbstractGitProvider
{
    private final String apiBase;

    public GitHubProvider(String apiBase)
    {
        this.apiBase = apiBase;
    }

    private static String enc(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public List<GitCommit> listCommits(String apiBase, String fullName, String branch, String token, int page, int perPage)
    {
        StringBuilder url = new StringBuilder(this.apiBase + "/repos/" + fullName + "/commits?page=" + page + "&per_page=" + perPage);
        if (branch != null && !branch.isEmpty())
        {
            url.append("&sha=").append(enc(branch));
        }
        String body = get(url.toString(), token, "Authorization", "Bearer");
        return parseCommits(body);
    }

    @Override
    public List<GitCommit> listCommitsSince(String apiBase, String fullName, String branch, String token, String sinceIso)
    {
        List<GitCommit> all = new ArrayList<>();
        for (int page = 1; page <= MAX_HEAT_PAGES; page++)
        {
            StringBuilder url = new StringBuilder(this.apiBase + "/repos/" + fullName
                    + "/commits?page=" + page + "&per_page=100&since=" + enc(sinceIso));
            if (branch != null && !branch.isEmpty())
            {
                url.append("&sha=").append(enc(branch));
            }
            List<GitCommit> pageList = parseCommits(get(url.toString(), token, "Authorization", "Bearer"));
            all.addAll(pageList);
            if (pageList.size() < 100)
            {
                break;
            }
        }
        return all;
    }

    private List<GitCommit> parseCommits(String body)
    {
        JSONArray arr = JSON.parseArray(body);
        List<GitCommit> list = new ArrayList<>();
        if (arr == null)
        {
            return list;
        }
        for (int i = 0; i < arr.size(); i++)
        {
            JSONObject c = arr.getJSONObject(i);
            GitCommit g = new GitCommit();
            g.setSha(c.getString("sha"));
            JSONObject cm = c.getJSONObject("commit");
            if (cm != null)
            {
                g.setMessage(cm.getString("message"));
                JSONObject a = cm.getJSONObject("author");
                if (a != null)
                {
                    g.setAuthorName(a.getString("name"));
                    g.setAuthorEmail(a.getString("email"));
                    g.setDate(a.getString("date"));
                }
            }
            JSONObject au = c.getJSONObject("author");
            if (au != null)
            {
                g.setAuthorLogin(au.getString("login"));
            }
            JSONObject s = c.getJSONObject("stats");
            if (s != null)
            {
                g.setAdditions(s.getIntValue("additions"));
                g.setDeletions(s.getIntValue("deletions"));
                g.setTotal(s.getIntValue("total"));
            }
            list.add(g);
        }
        return list;
    }

    @Override
    public List<GitContributor> listContributors(String apiBase, String fullName, String token)
    {
        String url = this.apiBase + "/repos/" + fullName + "/contributors?per_page=100";
        String body = get(url, token, "Authorization", "Bearer");
        JSONArray arr = JSON.parseArray(body);
        List<GitContributor> list = new ArrayList<>();
        if (arr == null)
        {
            return list;
        }
        for (int i = 0; i < arr.size(); i++)
        {
            JSONObject o = arr.getJSONObject(i);
            GitContributor g = new GitContributor();
            g.setLogin(o.getString("login"));
            g.setName(o.getString("login"));
            g.setEmail(null);
            g.setContributions(o.getIntValue("contributions"));
            list.add(g);
        }
        return list;
    }

    @Override
    public List<String> listBranches(String apiBase, String fullName, String token)
    {
        List<String> branches = new ArrayList<>();
        for (int page = 1; page <= 3; page++)
        {
            String url = this.apiBase + "/repos/" + fullName + "/branches?per_page=100&page=" + page;
            String body = get(url, token, "Authorization", "Bearer");
            JSONArray arr = JSON.parseArray(body);
            if (arr == null || arr.isEmpty())
            {
                break;
            }
            for (int i = 0; i < arr.size(); i++)
            {
                String name = arr.getJSONObject(i).getString("name");
                if (name != null && !name.isEmpty())
                {
                    branches.add(name);
                }
            }
            if (arr.size() < 100)
            {
                break;
            }
        }
        return branches;
    }

    @Override
    public String getDefaultBranch(String apiBase, String fullName, String token)
    {
        String body = get(this.apiBase + "/repos/" + fullName, token, "Authorization", "Bearer");
        JSONObject o = JSON.parseObject(body);
        return o == null ? null : o.getString("default_branch");
    }
}
