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
 * GitLab 适配器(公有云 gitlab.com/api/v4, 或自托管 GitLab 传自定义 apiBase)。
 * 鉴权头 PRIVATE-TOKEN。commits 含 stats; contributors 返回 name/email/commits。
 */
public class GitLabProvider extends AbstractGitProvider
{
    private final String apiBase;

    public GitLabProvider(String apiBase)
    {
        this.apiBase = apiBase;
    }

    private static String encPath(String fullName)
    {
        return fullName.replace("/", "%2F");
    }

    private static String enc(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public List<GitCommit> listCommits(String apiBase, String fullName, String branch, String token, int page, int perPage)
    {
        StringBuilder url = new StringBuilder(this.apiBase + "/projects/" + encPath(fullName)
                + "/repository/commits?page=" + page + "&per_page=" + perPage);
        if (branch != null && !branch.isEmpty())
        {
            url.append("&ref_name=").append(enc(branch));
        }
        String body = get(url.toString(), token, "PRIVATE-TOKEN", null);
        return parseCommits(body);
    }

    @Override
    public List<GitCommit> listCommitsSince(String apiBase, String fullName, String branch, String token, String sinceIso)
    {
        List<GitCommit> all = new ArrayList<>();
        for (int page = 1; page <= MAX_HEAT_PAGES; page++)
        {
            StringBuilder url = new StringBuilder(this.apiBase + "/projects/" + encPath(fullName)
                    + "/repository/commits?page=" + page + "&per_page=100&since=" + enc(sinceIso));
            if (branch != null && !branch.isEmpty())
            {
                url.append("&ref_name=").append(enc(branch));
            }
            List<GitCommit> pageList = parseCommits(get(url.toString(), token, "PRIVATE-TOKEN", null));
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
            g.setSha(c.getString("id"));
            g.setMessage(c.getString("title"));
            JSONObject a = c.getJSONObject("author");
            if (a != null)
            {
                g.setAuthorName(a.getString("name"));
                g.setAuthorEmail(a.getString("email"));
            }
            g.setAuthorLogin(c.getString("author_name"));
            g.setDate(c.getString("committed_date") != null ? c.getString("committed_date") : c.getString("authored_date"));
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
        String url = this.apiBase + "/projects/" + encPath(fullName) + "/repository/contributors?per_page=100";
        String body = get(url, token, "PRIVATE-TOKEN", null);
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
            g.setName(o.getString("name"));
            g.setEmail(o.getString("email"));
            g.setLogin(o.getString("name"));
            g.setContributions(o.getIntValue("commits"));
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
            String url = this.apiBase + "/projects/" + encPath(fullName) + "/repository/branches?per_page=100&page=" + page;
            String body = get(url, token, "PRIVATE-TOKEN", null);
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
        String body = get(this.apiBase + "/projects/" + encPath(fullName), token, "PRIVATE-TOKEN", null);
        JSONObject o = JSON.parseObject(body);
        return o == null ? null : o.getString("default_branch");
    }
}
