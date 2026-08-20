package com.ruoyi.project.git;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.git.impl.GitHubProvider;
import com.ruoyi.project.git.impl.GiteeProvider;
import com.ruoyi.project.git.impl.GitLabProvider;

/**
 * 按 platform 选择平台适配器。
 * self-hosted 复用 GitLab 兼容协议(企业内网最常见), 需传入自定义 apiBase(强制 HTTPS + SSRF 校验)。
 */
public class GitProviderFactory
{
    public GitProvider getProvider(String platform, String apiBase)
    {
        if (platform == null || platform.isEmpty())
        {
            throw new ServiceException("仓库平台不能为空");
        }
        switch (platform)
        {
            case "github":
                return new GitHubProvider("https://api.github.com");
            case "gitlab":
                return new GitLabProvider("https://gitlab.com/api/v4");
            case "gitee":
                return new GiteeProvider("https://gitee.com/api/v5");
            case "gitea":
                // Gitea API 与 GitHub 兼容, 复用 GitHubProvider, 需传入自定义 apiBase(https://host/api/v1)
                if (apiBase == null || apiBase.trim().isEmpty())
                {
                    throw new ServiceException("Gitea 需填写 API 地址（如 https://gitea.xxx.com/api/v1）");
                }
                return new GitHubProvider(apiBase.trim());
            case "self-hosted":
                if (apiBase == null || apiBase.trim().isEmpty())
                {
                    throw new ServiceException("自托管仓库需填写 API 地址");
                }
                return new GitLabProvider(apiBase.trim());
            default:
                throw new ServiceException("不支持的仓库平台：" + platform);
        }
    }
}
