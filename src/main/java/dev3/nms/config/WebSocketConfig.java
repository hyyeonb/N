package dev3.nms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독 경로 - 카테고리별 분리
        config.enableSimpleBroker(
            "/topic/alerts",            // 전체 알림
            "/topic/alerts/connection", // 연결 장애
            "/topic/alerts/performance",// 성능 장애
            "/topic/alerts/port",       // 포트 장애
            "/topic/alerts/system",     // 시스템 장애
            "/user/queue/alerts"        // 개인 알림
        );
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/alerts")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
