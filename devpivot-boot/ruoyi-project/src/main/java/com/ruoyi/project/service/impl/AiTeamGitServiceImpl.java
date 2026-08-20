package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.ruoyi.project.git.GitProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.AesGcmCrypto;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamProjectRepo;
import com.ruoyi.project.git.GitProviderFactory;
import com.ruoyi.project.git.dto.GitBranchInfo;
import com.ruoyi.project.git.dto.GitCommit;
import com.ruoyi.project.git.dto.GitContributor;
import com.ruoyi.project.git.dto.GitHeatmap;
import com.ruoyi.project.git.dto.GitHeatmapDay;
import com.ruoyi.project.git.dto.GitRepoConfig;
import com.ruoyi.project.git.dto.GitRepoSaveReq;
import com.ruoyi.project.mapper.AiTeamMapper;
import com.ruoyi.project.service.IAiTeamGitService;

/**
 * 团队项目 Git 仓库统计业务层实现(多仓库: 以 repoId 定位, 缓存 key 含 repoId)
 *
 * 鉴权沿用团队模块约定: 在 service 层 assertMember / assertManager(基于 ai_team_member 判定)。
 */
@Service
public class AiTeamGitServiceImpl implements IAiTeamGitService
{
    private static final Set<String> VALID_PLATFORMS = Set.of("github", "gitlab", "gitee", "gitea", "self-hosted");
    private static final int PER_PAGE = 20;
    private static final long CACHE_TTL = 300; // 秒, 5 分钟, 规避公共 API 限流
    /** 热力图按上海时区聚合(与 Gitee/GitHub 国内观感一致) */
    private static final ZoneId HEAT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter UTC_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Autowired
    private AiTeamMapper teamMapper;
    @Autowired
    private AesGcmCrypto aesGcmCrypto;
    @Autowired
    private RedisCache redisCache;
    private final GitProviderFactory factory = new GitProviderFactory();

    @Override
    public Long addRepo(Long teamId, Long projectId, Long operatorId, GitRepoSaveReq req)
    {
        assertManager(teamId, operatorId);
        validateReq(req, false);
        AiTeamProjectRepo repo = new AiTeamProjectRepo();
        repo.setTeamId(teamId);
        repo.setProjectId(projectId);
        repo.setName(trimToNull(req.getName()));
        repo.setPlatform(req.getPlatform());
        repo.setRepoFullName(req.getRepoFullName().trim());
        repo.setRepoBranch(trimToNull(req.getRepoBranch()));
        repo.setRepoApiBase(trimToNull(req.getRepoApiBase()));
        if (req.getAccessToken() != null && !req.getAccessToken().isEmpty())
        {
            repo.setAccessToken(aesGcmCrypto.encrypt(req.getAccessToken().trim()));
        }
        repo.setCreateBy(SecurityUtils.getUsername());
        repo.setCreateTime(DateUtils.getNowDate());
        teamMapper.insertProjectRepo(repo);
        return repo.getId();
    }

    @Override
    public void updateRepo(Long teamId, Long repoId, Long operatorId, GitRepoSaveReq req)
    {
        assertManager(teamId, operatorId);
        validateRepoBelongs(teamId, repoId);
        validateReq(req, true);
        AiTeamProjectRepo repo = new AiTeamProjectRepo();
        repo.setId(repoId);
        repo.setName(trimToNull(req.getName()));
        repo.setPlatform(req.getPlatform());
        repo.setRepoFullName(req.getRepoFullName().trim());
        repo.setRepoBranch(trimToNull(req.getRepoBranch()));
        repo.setRepoApiBase(trimToNull(req.getRepoApiBase()));
        repo.setUpdateBy(SecurityUtils.getUsername());
        repo.setUpdateTime(DateUtils.getNowDate());
        teamMapper.updateProjectRepoById(repo);
        // 令牌单独处理: 留空表示不修改
        if (req.getAccessToken() != null && !req.getAccessToken().isEmpty())
        {
            teamMapper.updateRepoToken(repoId, aesGcmCrypto.encrypt(req.getAccessToken().trim()),
                    SecurityUtils.getUsername(), DateUtils.getNowDate());
        }
        clearRepoCache(repoId);
    }

    @Override
    public void deleteRepo(Long teamId, Long repoId, Long operatorId)
    {
        assertManager(teamId, operatorId);
        validateRepoBelongs(teamId, repoId);
        teamMapper.deleteProjectRepo(repoId);
        clearRepoCache(repoId);
    }

    @Override
    public List<AiTeamProjectRepo> listRepos(Long teamId, Long projectId, Long userId)
    {
        assertMember(teamId, userId);
        return teamMapper.selectReposByProject(teamId, projectId);
    }

    @Override
    public GitRepoConfig getRepoConfig(Long teamId, Long repoId, Long userId)
    {
        assertMember(teamId, userId);
        validateRepoBelongs(teamId, repoId);
        AiTeamProjectRepo repo = teamMapper.selectRepoById(repoId);
        GitRepoConfig cfg = new GitRepoConfig();
        cfg.setConfigured(true);
        cfg.setRepoId(repoId);
        cfg.setName(repo.getName());
        cfg.setPlatform(repo.getPlatform());
        cfg.setRepoFullName(repo.getRepoFullName());
        cfg.setRepoBranch(repo.getRepoBranch());
        cfg.setRepoApiBase(repo.getRepoApiBase());
        String enc = teamMapper.selectRepoTokenById(repoId);
        if (enc != null && !enc.isEmpty())
        {
            cfg.setMaskedToken(AesGcmCrypto.maskKey(aesGcmCrypto.decrypt(enc)));
        }
        else
        {
            cfg.setMaskedToken("");
        }
        return cfg;
    }

    @Override
    public List<GitContributor> getRepoContributors(Long teamId, Long repoId, Long userId)
    {
        assertMember(teamId, userId);
        RepoCtx ctx = resolveCtx(teamId, repoId);
        String cacheKey = "git:contributors:" + repoId;
        Object cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return (List<GitContributor>) cached;
        }
        List<GitContributor> list = factory.getProvider(ctx.platform, ctx.apiBase)
                .listContributors(ctx.apiBase, ctx.fullName, ctx.token);
        Map<String, AiTeamMember> emailMap = new HashMap<>();
        Map<String, AiTeamMember> userMap = new HashMap<>();
        buildMemberMaps(teamMapper.selectMembersByTeamId(teamId), emailMap, userMap);
        for (GitContributor c : list)
        {
            AiTeamMember m = c.getEmail() != null ? emailMap.get(c.getEmail().toLowerCase()) : null;
            if (m == null && c.getLogin() != null)
            {
                m = userMap.get(c.getLogin());
            }
            if (m != null)
            {
                c.setMemberId(m.getUserId());
                c.setMemberName(m.getNickName());
            }
        }
        redisCache.setCacheObject(cacheKey, list, (int) CACHE_TTL, TimeUnit.SECONDS);
        return list;
    }

    @Override
    public List<GitCommit> getRepoCommits(Long teamId, Long repoId, Long userId, int page, String branch)
    {
        assertMember(teamId, userId);
        if (page < 1)
        {
            page = 1;
        }
        RepoCtx ctx = resolveCtx(teamId, repoId);
        String branchKey = branch == null ? "" : branch;
        String cacheKey = "git:commits:" + repoId + ":" + branchKey + ":" + page;
        Object cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return (List<GitCommit>) cached;
        }
        List<GitCommit> list = factory.getProvider(ctx.platform, ctx.apiBase)
                .listCommits(ctx.apiBase, ctx.fullName, branch, ctx.token, page, PER_PAGE);
        Map<String, AiTeamMember> emailMap = new HashMap<>();
        Map<String, AiTeamMember> userMap = new HashMap<>();
        buildMemberMaps(teamMapper.selectMembersByTeamId(teamId), emailMap, userMap);
        for (GitCommit c : list)
        {
            AiTeamMember m = c.getAuthorEmail() != null ? emailMap.get(c.getAuthorEmail().toLowerCase()) : null;
            if (m == null && c.getAuthorLogin() != null)
            {
                m = userMap.get(c.getAuthorLogin());
            }
            if (m != null)
            {
                c.setMemberId(m.getUserId());
                c.setMemberName(m.getNickName());
            }
        }
        redisCache.setCacheObject(cacheKey, list, (int) CACHE_TTL, TimeUnit.SECONDS);
        return list;
    }

    @Override
    public GitBranchInfo getRepoBranches(Long teamId, Long repoId, Long userId)
    {
        assertMember(teamId, userId);
        RepoCtx ctx = resolveCtx(teamId, repoId);
        String cacheKey = "git:branches:" + repoId;
        Object cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return (GitBranchInfo) cached;
        }
        GitBranchInfo info = new GitBranchInfo();
        GitProvider provider = factory.getProvider(ctx.platform, ctx.apiBase);
        info.setDefaultBranch(provider.getDefaultBranch(ctx.apiBase, ctx.fullName, ctx.token));
        info.setBranches(provider.listBranches(ctx.apiBase, ctx.fullName, ctx.token));
        redisCache.setCacheObject(cacheKey, info, (int) CACHE_TTL, TimeUnit.SECONDS);
        return info;
    }

    @Override
    public GitHeatmap getRepoHeatmap(Long teamId, Long repoId, Long userId, String branch)
    {
        assertMember(teamId, userId);
        RepoCtx ctx = resolveCtx(teamId, repoId);
        String branchKey = branch == null ? "" : branch;
        // v2: counts 由 Map 改为 List<GitHeatmapDay>, 换 key 避开已写入的坏缓存
        String cacheKey = "git:heatmap:v2:" + repoId + ":" + branchKey;
        Object cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return (GitHeatmap) cached;
        }
        LocalDate end = LocalDate.now(HEAT_ZONE);
        LocalDate start = end.minusDays(364); // 含结束日共 365 天
        String sinceIso = UTC_ISO.format(start.atStartOfDay(HEAT_ZONE).toInstant());
        List<GitCommit> list = factory.getProvider(ctx.platform, ctx.apiBase)
                .listCommitsSince(ctx.apiBase, ctx.fullName, branch, ctx.token, sinceIso);
        GitHeatmap hm = new GitHeatmap();
        hm.setStartDate(start.toString());
        hm.setEndDate(end.toString());
        Map<String, Integer> counts = new LinkedHashMap<>();
        int total = 0;
        for (GitCommit c : list)
        {
            if (c.getDate() == null)
            {
                continue;
            }
            try
            {
                LocalDate d = OffsetDateTime.parse(c.getDate()).atZoneSameInstant(HEAT_ZONE).toLocalDate();
                if (d.isBefore(start) || d.isAfter(end))
                {
                    continue;
                }
                counts.merge(d.toString(), 1, Integer::sum);
                total++;
            }
            catch (Exception ignore)
            {
                // 日期格式异常的单条忽略, 不影响整体
            }
        }
        List<GitHeatmapDay> dayList = new ArrayList<>(counts.size());
        counts.forEach((date, cnt) -> dayList.add(new GitHeatmapDay(date, cnt)));
        hm.setTotal(total);
        hm.setList(dayList);
        redisCache.setCacheObject(cacheKey, hm, (int) CACHE_TTL, TimeUnit.SECONDS);
        return hm;
    }

    /** 校验新增/更新请求字段(update 时 allowNullName=true: 别名可为空) */
    private void validateReq(GitRepoSaveReq req, boolean update)
    {
        if (req.getPlatform() == null || !VALID_PLATFORMS.contains(req.getPlatform()))
        {
            throw new ServiceException("不支持的仓库平台：" + (req.getPlatform() == null ? "null" : req.getPlatform()));
        }
        if (req.getRepoFullName() == null || req.getRepoFullName().trim().isEmpty())
        {
            throw new ServiceException("仓库全名不能为空");
        }
        if (("self-hosted".equals(req.getPlatform()) || "gitea".equals(req.getPlatform()))
                && (req.getRepoApiBase() == null || req.getRepoApiBase().trim().isEmpty()))
        {
            throw new ServiceException("自托管 / Gitea 仓库需填写 API 地址");
        }
        if (req.getName() != null && req.getName().length() > 64)
        {
            throw new ServiceException("仓库别名不能超过 64 字符");
        }
    }

    /** 校验仓库属于该团队(防跨团队越权操作) */
    private void validateRepoBelongs(Long teamId, Long repoId)
    {
        if (repoId == null || teamMapper.countRepoByTeam(repoId, teamId) == 0)
        {
            throw new ServiceException("仓库不存在或不属于该团队");
        }
    }

    /** 读取仓库上下文(平台/全名/apiBase/解密令牌),令牌缺失明确报错 */
    private RepoCtx resolveCtx(Long teamId, Long repoId)
    {
        validateRepoBelongs(teamId, repoId);
        AiTeamProjectRepo repo = teamMapper.selectRepoById(repoId);
        String enc = teamMapper.selectRepoTokenById(repoId);
        String token = null;
        if (enc != null && !enc.isEmpty())
        {
            token = aesGcmCrypto.decrypt(enc);
        }
        if (token == null || token.isEmpty())
        {
            throw new ServiceException("仓库未配置访问令牌，请在仓库配置中填写只读令牌");
        }
        RepoCtx ctx = new RepoCtx();
        ctx.platform = repo.getPlatform();
        ctx.fullName = repo.getRepoFullName();
        ctx.apiBase = repo.getRepoApiBase();
        ctx.token = token;
        return ctx;
    }

    private void buildMemberMaps(List<AiTeamMember> members, Map<String, AiTeamMember> emailMap, Map<String, AiTeamMember> userMap)
    {
        if (members == null)
        {
            return;
        }
        for (AiTeamMember m : members)
        {
            if (m.getEmail() != null)
            {
                emailMap.put(m.getEmail().toLowerCase(), m);
            }
            if (m.getUserName() != null)
            {
                userMap.put(m.getUserName(), m);
            }
        }
    }

    private void clearRepoCache(Long repoId)
    {
        try
        {
            Collection<String> keys = redisCache.keys("git:*");
            if (keys == null || keys.isEmpty())
            {
                return;
            }
            List<String> toDel = new ArrayList<>();
            String token = ":" + repoId;
            for (String k : keys)
            {
                // key 形如 git:contributors:123 / git:commits:123:main:1 / git:branches:123
                if (k.startsWith("git:") && k.contains(token))
                {
                    toDel.add(k);
                }
            }
            if (!toDel.isEmpty())
            {
                redisCache.deleteObject(toDel);
            }
        }
        catch (Exception e)
        {
            // 缓存清理失败不阻塞主流程
        }
    }

    private String trimToNull(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static class RepoCtx
    {
        String platform;
        String fullName;
        String apiBase;
        String token;
    }

    private AiTeamMember assertMember(Long teamId, Long userId)
    {
        AiTeamMember me = teamMapper.selectMember(teamId, userId);
        if (me == null)
        {
            throw new ServiceException("您不是该团队成员");
        }
        return me;
    }

    private void assertManager(Long teamId, Long userId)
    {
        AiTeamMember me = assertMember(teamId, userId);
        if (!"OWNER".equals(me.getRole()) && !"ADMIN".equals(me.getRole()))
        {
            throw new ServiceException("无操作权限(仅管理员/创建者可操作)");
        }
    }
}
