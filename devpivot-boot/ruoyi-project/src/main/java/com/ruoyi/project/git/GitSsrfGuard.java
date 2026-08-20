package com.ruoyi.project.git;

import java.net.InetAddress;
import java.net.URL;
import com.ruoyi.common.exception.ServiceException;

/**
 * SSRF 防护: 仅允许 HTTPS, 禁止访问内网 / 回环 / 链路本地地址。
 * 防止有人把仓库地址填成 http://169.254.169.254 之类打元数据或内网探测。
 */
public class GitSsrfGuard
{
    public static void ensureSafe(String urlStr)
    {
        if (urlStr == null || urlStr.isEmpty())
        {
            throw new ServiceException("仓库地址不能为空");
        }
        URL url;
        try
        {
            url = new URL(urlStr);
        }
        catch (Exception e)
        {
            throw new ServiceException("仓库地址格式非法");
        }
        if (!"https".equalsIgnoreCase(url.getProtocol()))
        {
            throw new ServiceException("仅允许 HTTPS 仓库地址(安全策略)");
        }
        String host = url.getHost();
        if (host == null || host.isEmpty())
        {
            throw new ServiceException("仓库地址主机非法");
        }
        try
        {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isAnyLocalAddress() || addr.isLinkLocalAddress())
            {
                throw new ServiceException("禁止访问内网 / 回环地址");
            }
        }
        catch (ServiceException se)
        {
            throw se;
        }
        catch (Exception e)
        {
            throw new ServiceException("无法解析仓库主机");
        }
    }
}
