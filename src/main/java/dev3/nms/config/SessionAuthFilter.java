package dev3.nms.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev3.nms.service.AuthService;
import dev3.nms.vo.auth.UserVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * HttpSession에서 USER_ID를 읽어 SecurityContext에 인증 정보를 설정하는 필터
 * - Caffeine 캐시로 DB 조회 최소화 (30초 캐시)
 */
@Slf4j
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;

    // 사용자 정보 캐시 (30초 TTL) - 매 요청마다 DB 조회 방지
    private final Cache<Long, UserVO> userCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(500)
            .build();

    public SessionAuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object userIdObj = session.getAttribute("USER_ID");
            if (userIdObj != null) {
                Long userId = toLong(userIdObj);

                if (userId != null) {
                    // 캐시에서 먼저 조회, 없으면 DB 조회
                    UserVO user = userCache.get(userId, id -> authService.getUserById(id));

                    if (user != null && !"SUSPENDED".equals(user.getSTATUS())) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 사용자 상태 변경 시 캐시 무효화 (AdminService에서 호출)
     */
    public void evictUser(Long userId) {
        userCache.invalidate(userId);
    }

    private Long toLong(Object obj) {
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
    }
}
