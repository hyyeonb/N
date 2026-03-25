package dev3.nms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP 기반 API Rate Limiting
 * - 일반 API: 분당 120회
 * - 인증 API: 분당 20회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int GENERAL_LIMIT = 120;  // 분당
    private static final int AUTH_LIMIT = 20;       // 분당

    private final ObjectMapper objectMapper;

    private final Cache<String, AtomicInteger> generalCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(50_000)
            .build();

    private final Cache<String, AtomicInteger> authCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(50_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        String uri = request.getRequestURI();

        boolean isAuthEndpoint = uri.startsWith("/api/auth/");

        if (isAuthEndpoint) {
            AtomicInteger count = authCache.get(ip, k -> new AtomicInteger(0));
            if (count.incrementAndGet() > AUTH_LIMIT) {
                log.warn("[Rate Limit] 인증 API 초과 - IP: {}, 요청: {}", ip, uri);
                sendTooManyRequests(response);
                return;
            }
        } else {
            AtomicInteger count = generalCache.get(ip, k -> new AtomicInteger(0));
            if (count.incrementAndGet() > GENERAL_LIMIT) {
                log.warn("[Rate Limit] 일반 API 초과 - IP: {}, 요청: {}", ip, uri);
                sendTooManyRequests(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        ResVO<Void> resVO = new ResVO<>(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", null);
        response.getWriter().write(objectMapper.writeValueAsString(resVO));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) return "127.0.0.1";
        return ip;
    }
}
