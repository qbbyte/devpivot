package com.ruoyi.project.websocket;

import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.project.mapper.AiTeamMapper;

/**
 * STOMP 入站鉴权拦截器
 * 
 * <p>CONNECT：从 STOMP 帧的 Authorization 头(Bearer token)或 token 头取出 JWT，
 * 复用 {@link TokenService} 校验并重建登录身份，写入消息头供后续 SUBSCRIBE 使用。</p>
 * 
 * <p>SUBSCRIBE：校验订阅者确为该团队成员，防止越权订阅他人团队频道。</p>
 * 
 * @author devpivot
 * @date 2026-08-10
 */
@Component
public class TeamWsAuthInterceptor implements ChannelInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(TeamWsAuthInterceptor.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AiTeamMapper teamMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel)
    {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command))
        {
            LoginUser loginUser = authenticate(accessor);
            if (loginUser == null)
            {
                // 无有效 token：抛错会让 Spring 向该连接回送 ERROR 帧并断开
                throw new IllegalArgumentException("WebSocket 鉴权失败：缺少或无效的 token");
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    loginUser, null, Collections.emptyList());
            accessor.setUser(auth);
            accessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }
        else if (StompCommand.SUBSCRIBE.equals(command))
        {
            Principal principal = accessor.getUser();
            if (principal == null)
            {
                throw new IllegalArgumentException("未鉴权的订阅请求");
            }
            LoginUser loginUser = (LoginUser) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            Long teamId = parseTeamId(accessor.getDestination());
            if (teamId != null && teamMapper.selectMember(teamId, loginUser.getUserId()) == null)
            {
                throw new IllegalArgumentException("无权订阅该团队频道");
            }
        }
        return message;
    }

    private LoginUser authenticate(StompHeaderAccessor accessor)
    {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(auth))
        {
            auth = accessor.getFirstNativeHeader("token");
        }
        if (!StringUtils.hasText(auth))
        {
            return null;
        }
        if (auth.startsWith("Bearer "))
        {
            auth = auth.substring("Bearer ".length());
        }
        else if (auth.startsWith("Bearer"))
        {
            auth = auth.substring("Bearer".length()).trim();
        }
        return tokenService.getLoginUser(auth);
    }

    /** 从 /topic/team/{teamId} 或 /topic/team/{teamId}/read 解析团队ID */
    private Long parseTeamId(String destination)
    {
        if (!StringUtils.hasText(destination))
        {
            return null;
        }
        String prefix = "/topic/team/";
        if (!destination.startsWith(prefix))
        {
            return null;
        }
        String rest = destination.substring(prefix.length());
        int slash = rest.indexOf('/');
        String idStr = slash >= 0 ? rest.substring(0, slash) : rest;
        try
        {
            return Long.parseLong(idStr);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
