package dev3.nms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.vo.common.ResVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthFilter sessionAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:3000",
        "http://localhost:5173",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:5173",
        "http://192.168.3.114",
        "http://192.168.3.114.nip.io",
        "https://192.168.3.114",
        "https://nms.stninfo.local"
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: SPA + SameSite 쿠키로 대체 (REST API는 CSRF 토큰 대신 SameSite 쿠키로 방어)
            .csrf(csrf -> csrf.disable())

            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 인가 규칙
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/oauth/**").permitAll()
                .requestMatchers("/api/alerts/**").permitAll()
                .requestMatchers("/api/middleware/register-key").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/mgmt/vendors/test-oid").permitAll()
                .requestMatchers("/api/history/page-view").permitAll()
                .anyRequest().authenticated()
            )

            // 필터 체인: RateLimit → SessionAuth → Spring Security
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // 인증/인가 실패 JSON 응답
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResVO<Void> resVO = new ResVO<>(401, "로그인이 필요합니다", null);
                    response.getWriter().write(objectMapper.writeValueAsString(resVO));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResVO<Void> resVO = new ResVO<>(403, "권한이 없습니다", null);
                    response.getWriter().write(objectMapper.writeValueAsString(resVO));
                })
            )

            // 세션 관리
            // sessionFixation: none — 커스텀 세션 인증(SessionAuthFilter)과 충돌 방지
            // 세션 고정 공격은 SameSite=Lax 쿠키 + HTTPS(운영)로 방어
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sf -> sf.none())
            )

            // Security Headers
            .headers(headers -> headers
                // X-Content-Type-Options: nosniff (MIME 스니핑 방지)
                .contentTypeOptions(cto -> {})
                // X-Frame-Options: DENY (클릭재킹 방지)
                .frameOptions(fo -> fo.deny())
                // X-XSS-Protection: 0 (최신 브라우저는 자체 XSS 필터 사용)
                .xssProtection(xss -> xss.disable())
                // Referrer-Policy
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Permissions-Policy (카메라, 마이크 등 차단)
                .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=()"))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
        return source;
    }

}
