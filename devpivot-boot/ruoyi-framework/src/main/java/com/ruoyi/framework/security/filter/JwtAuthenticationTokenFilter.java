package com.ruoyi.framework.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * token过滤器 验证token有效性
 * 
 * @author ruoyi
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    static
    {
        // 根因修复：SSE/异步分派场景下，Spring MVC 会在独立线程上继续写入响应。
        // 默认 MODE_THREADLOCAL 不会把主请求线程的安全上下文传播到异步线程，
        // 导致异步线程 SecurityContext 为空 → AuthorizationFilter 抛 Access Denied
        // （且 SSE 响应已提交，连带抛出 "response is already committed"）。
        // 改用 INHERITABLETHREADLOCAL，使主请求线程的登录身份被子线程继承，
        // 从根上保证异步/SSE 请求始终携带登录身份。
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch()
    {
        // 默认 true（异步分派跳过本过滤器）。但本项目无状态(STATELESS) + SseEmitter 流式接口，
        // 异步分派线程上 SecurityContext 为空，会导致 AuthorizationFilter 抛 Access Denied。
        // 改为 false：异步分派阶段也重新从请求头校验 token，恢复安全上下文。
        return false;
    }
}
