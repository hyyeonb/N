package dev3.nms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 - API 전용 백엔드
 * 인증은 Spring Security (SecurityConfig)에서 처리
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // LoginCheckInterceptor 제거됨 - Spring Security로 대체
}
