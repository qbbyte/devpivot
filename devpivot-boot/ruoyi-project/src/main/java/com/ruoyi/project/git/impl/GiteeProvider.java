package com.ruoyi.project.git.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.project.git.AbstractGitProvider;
import com.ruoyi.project.git.dto.GitCommit;
import com.ruoyi.project.git.dto.GitContributor;

/**
 * Gitee 适配器(公有云 gitee.com/api/v5)。
 * 令牌走 query 参数 access_token; commits 不含 stats(增删行置 0);
 * Gitee 无标准 contributors 接口, 故从提交聚合统计每人提交数。
 */
public class GiteeProvider extends AbstractGitProvider
{
    private final String apiBase;

    public GiteeProvider(String apiBase)
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
        if (token != null && !token.isEmpty())
        {
            url.append("&access_token=").append(enc(token));
        }
        String body = get(url.toString(), null, null, null);
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
            if (token != null && !token.isEmpty())
            {
                url.append("&access_token=").append(enc(token));
            }
            List<GitCommit> pageList = parseCommits(get(url.toString(), null, null, null));
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
            list.add(g);
        }
        return list;
    }

    @Override
    public List<GitContributor> listContributors(String apiBase, String fullName, String token)
    {
        // Gitee 无标准 contributors 接口, 从前 5 页提交(每页100)聚合
        Map<String, GitContributor> map = new LinkedHashMap<>();
        for (int p = 1; p <= 5; p++)
        {
            List<GitCommit> cs = listCommits(apiBase, fullName, null, token, p, 100);
            if (cs.isEmpty())
            {
                break;
            }
            for (GitCommit c : cs)
            {
                String key = c.getAuthorEmail() != null ? c.getAuthorEmail()
                        : (c.getAuthorLogin() != null ? c.getAuthorLogin() : c.getAuthorName());
                if (key == null)
                {
                    continue;
                }
                GitContributor g = map.computeIfAbsent(key, k -> {
                    GitContributor nc = new GitContributor();
                    nc.setEmail(c.getAuthorEmail());
                    nc.setLogin(c.getAuthorLogin());
                    nc.setName(c.getAuthorName());
                    nc.setContributions(0);
                    return nc;
                });
                g.setContributions(g.getContributions() + 1);
            }
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<String> listBranches(String apiBase, String fullName, String token)
    {
        List<String> branches = new ArrayList<>();
        for (int page = 1; page <= 3; page++)
        {
            StringBuilder url = new StringBuilder(this.apiBase + "/repos/" + fullName + "/branches?per_page=100&page=" + page);
            if (token != null && !token.isEmpty())
            {
                url.append("&access_token=").append(enc(token));
            }
            String body = get(url.toString(), null, null, null);
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
        StringBuilder url = new StringBuilder(this.apiBase + "/repos/" + fullName);
        if (token != null && !token.isEmpty())
        {
            url.append("?access_token=").append(enc(token));
        }
        String body = get(url.toString(), null, null, null);
        JSONObject o = JSON.parseObject(body);
        return o == null ? null : o.getString("default_branch");
    }
}
