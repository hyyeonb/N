package dev3.nms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.service.AuthService;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 로그인 체크 인터셉터 - API 전용 (JSON 응답)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.debug("LoginCheckInterceptor: Intercepting request for {}", requestURI);

        // CORS preflight 요청은 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("SESSION_ID") == null) {
            log.info("Non-authenticated user access attempt to {}.", requestURI);
            sendUnauthorizedResponse(response, "로그인이 필요합니다");
            return false;
        }

        String sessionId = (String) session.getAttribute("SESSION_ID");
        if (authService.getUserBySession(sessionId) == null) {
            log.info("Session {} is invalid.", sessionId);
            session.invalidate();
            sendUnauthorizedResponse(response, "세션이 만료되었습니다");
            return false;
        }

        log.debug("Authenticated user access to {}. Proceeding.", requestURI);
        return true;
    }

    /**
     * 401 Unauthorized JSON 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ResVO<Void> resVO = new ResVO<>(401, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(resVO));
    }
}
