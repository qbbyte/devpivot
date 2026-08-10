package com.ruoyi.project.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket / STOMP 实时推送配置(团队讨论区)
 * 
 * <p>端点：/ws/team（原生 WebSocket，不走 SockJS，避免 Spring Boot 4 下 SockJS 已废弃的兼容层）。
 * 频道：/topic/team/{teamId} 推送新消息；/topic/team/{teamId}/read 推送已读事件；
 * /user 用于后续可能的点对点推送（如在线状态）。</p>
 * 
 * <p>鉴权不在 HTTP 层做（SecurityConfig 已将 /ws/team/** permitAll），
 * 而是在 {@link TeamWsAuthInterceptor} 的 CONNECT/SUBSCRIBE 阶段用 STOMP 头里的 JWT 校验。</p>
 * 
 * @author devpivot
 * @date 2026-08-10
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
{
    private final TeamWsAuthInterceptor authInterceptor;

    public WebSocketConfig(TeamWsAuthInterceptor authInterceptor)
    {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        // 原生 WebSocket 端点；setAllowedOriginPatterns 允许同源/代理域接入
        registry.addEndpoint("/ws/team")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        // 简单内存 broker（无外部 MQ），按 team 分 topic 广播即可满足团队讨论规模
        registry.enableSimpleBroker("/topic", "/user");
        // 客户端发往服务端的消息前缀（目前发送走 REST，预留）
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration)
    {
        registration.interceptors(authInterceptor);
    }
}
