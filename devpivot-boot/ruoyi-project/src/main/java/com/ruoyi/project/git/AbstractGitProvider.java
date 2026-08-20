package com.ruoyi.project.git;

import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.exception.ServiceException;

/**
 * Git 平台适配器基类: 统一持有超时 RestTemplate, 提供带令牌头 + SSRF 防护 + 友好错误的 GET 助手。
 */
public abstract class AbstractGitProvider implements GitProvider
{
    /** 热力图 since 拉取的最大翻页数(100 条/页 = 3000 条, 防一年提交过多导致超时) */
    protected static final int MAX_HEAT_PAGES = 30;

    protected final RestTemplate restTemplate;

    protected AbstractGitProvider()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 发起 GET 并返回响应体字符串。
     * @param authHeaderName 鉴权头名(如 Authorization / PRIVATE-TOKEN),可为 null
     * @param authScheme 鉴权方案前缀(如 Bearer / token),可为 null
     */
    protected String get(String url, String token, String authHeaderName, String authScheme)
    {
        GitSsrfGuard.ensureSafe(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "devpivot");
        if (token != null && !token.isEmpty() && authHeaderName != null && !authHeaderName.isEmpty())
        {
            String value = (authScheme == null || authScheme.isEmpty()) ? token
                    : authScheme + " " + token;
            headers.set(authHeaderName, value);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try
        {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        }
        catch (HttpStatusCodeException e)
        {
            int code = e.getStatusCode().value();
            if (code == 401 || code == 403)
            {
                throw new ServiceException("令牌无效或权限不足（需仓库读取权限），请检查令牌");
            }
            if (code == 404)
            {
                throw new ServiceException("仓库不存在或无权限（私有库常见：令牌缺 repo 权限，或仓库名/分支拼错）");
            }
            throw new ServiceException("Git 平台请求失败：HTTP " + code);
        }
        catch (RestClientException e)
        {
            throw new ServiceException("无法连接 Git 平台：" + e.getMessage());
        }
    }
}
