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
        config.enableSimpleBroker(
            "/topic/alerts",
            "/topic/alerts/connection",
            "/topic/alerts/performance",
            "/topic/alerts/port",
            "/topic/alerts/system",
            "/topic/notice/urgent",
            "/user/queue/alerts"
        );
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket CORS도 명시적으로 제한 (기존 * → 허용 Origin만)
        registry.addEndpoint("/ws/alerts")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:5173",
                    "http://192.168.3.114",
                    "http://192.168.3.114.nip.io"
                )
                .withSockJS();
    }
}
