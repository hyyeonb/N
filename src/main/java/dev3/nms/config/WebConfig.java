package dev3.nms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 - API 전용 백엔드
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .order(1)
                .addPathPatterns("/api/**")      // API 경로만 인터셉터 적용
                .excludePathPatterns(
                        "/api/auth/**",          // 인증 관련 API 제외
                        "/api/oauth/**",         // OAuth 소셜 로그인 API 제외
                        "/api/alerts/**",        // Alert API 제외 (Kafka Consumer 내부 호출용)
                        "/api/mgmt/vendors/test-oid",  // 벤더 OID 테스트 API (디버그용)
                        "/error"
                );
    }
}
