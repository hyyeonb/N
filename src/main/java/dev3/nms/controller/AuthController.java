package dev3.nms.controller;

import dev3.nms.service.AuthService;
import dev3.nms.vo.auth.LoginResponseVO;
import dev3.nms.vo.auth.SocialCodeRequestVO;
import dev3.nms.vo.auth.SocialLoginRequestVO;
import dev3.nms.vo.auth.UserVO;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 소셜 로그인 API (Kakao, Google, Naver)
     *
     * @param request 소셜 로그인 요청 정보 (SOCIAL_TYPE, ACCESS_TOKEN)
     * @param httpRequest HTTP 요청 정보
     * @param session HTTP 세션
     * @return 로그인 응답 (사용자 정보 + 세션 ID)
     */
    @PostMapping("/social/login")
    public ResVO<LoginResponseVO> socialLogin(
            @RequestBody SocialLoginRequestVO request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("========== 소셜 로그인 요청 시작 ==========");
        log.info("요청 정보 - SOCIAL_TYPE: {}, ACCESS_TOKEN: {}",
                request.getSOCIAL_TYPE(),
                request.getACCESS_TOKEN() != null ? "***존재***" : "null");

        // IP 주소 및 User Agent 추출
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.info("클라이언트 정보 - IP: {}, User-Agent: {}", ipAddress, userAgent);

        // 소셜 로그인 처리
        log.info("AuthService.socialLogin() 호출");
        LoginResponseVO loginResponse = authService.socialLogin(request, ipAddress, userAgent);
        log.info("로그인 성공 - USER_ID: {}, EMAIL: {}", loginResponse.getUSER_ID(), loginResponse.getEMAIL());

        // 세션에 사용자 정보 저장
        session.setAttribute("USER_ID", loginResponse.getUSER_ID());
        session.setAttribute("SESSION_ID", loginResponse.getSESSION_ID());
        log.info("세션 저장 완료 - SESSION_ID: {}", loginResponse.getSESSION_ID());

        log.info("========== 소셜 로그인 요청 완료 ==========");
        return new ResVO<>(200, "로그인 성공", loginResponse);
    }

    /**
     * 소셜 로그인 API - Authorization Code 방식 (Kakao용)
     *
     * @param request 소셜 코드 요청 정보 (SOCIAL_TYPE, CODE, REDIRECT_URI)
     * @param httpRequest HTTP 요청 정보
     * @param session HTTP 세션
     * @return 로그인 응답 (사용자 정보 + 세션 ID)
     */
    @PostMapping("/social/code")
    public ResVO<LoginResponseVO> socialLoginWithCode(
            @RequestBody SocialCodeRequestVO request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("========== 소셜 로그인 (Code) 요청 시작 ==========");
        log.info("요청 정보 - SOCIAL_TYPE: {}, CODE: {}, REDIRECT_URI: {}, STATE: {}",
                request.getSOCIAL_TYPE(),
                request.getCODE() != null ? "***존재***" : "null",
                request.getREDIRECT_URI(),
                request.getSTATE());

        // IP 주소 및 User Agent 추출
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.info("클라이언트 정보 - IP: {}, User-Agent: {}", ipAddress, userAgent);

        // 소셜 로그인 처리
        log.info("AuthService.socialLoginWithCode() 호출");
        LoginResponseVO loginResponse = authService.socialLoginWithCode(request, ipAddress, userAgent);
        log.info("로그인 성공 - USER_ID: {}, EMAIL: {}", loginResponse.getUSER_ID(), loginResponse.getEMAIL());

        // 세션에 사용자 정보 저장
        session.setAttribute("USER_ID", loginResponse.getUSER_ID());
        session.setAttribute("SESSION_ID", loginResponse.getSESSION_ID());
        log.info("세션 저장 완료 - SESSION_ID: {}", loginResponse.getSESSION_ID());

        log.info("========== 소셜 로그인 (Code) 요청 완료 ==========");
        return new ResVO<>(200, "로그인 성공", loginResponse);
    }

    /**
     * 로그아웃 API
     *
     * @param session HTTP 세션
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResVO<Void> logout(HttpSession session) {
        String sessionId = (String) session.getAttribute("SESSION_ID");

        if (sessionId != null) {
            authService.logout(sessionId);
        }

        session.invalidate();

        return new ResVO<>(200, "로그아웃 성공", null);
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     *
     * @param session HTTP 세션
     * @return 사용자 정보
     */
    @GetMapping("/me")
    public ResVO<UserVO> getCurrentUser(HttpSession session) {
        String sessionId = (String) session.getAttribute("SESSION_ID");

        if (sessionId == null) {
            return new ResVO<>(401, "로그인이 필요합니다", null);
        }

        UserVO user = authService.getUserBySession(sessionId);

        if (user == null) {
            return new ResVO<>(401, "세션이 만료되었습니다", null);
        }

        return new ResVO<>(200, "조회 성공", user);
    }

    /**
     * 세션 유효성 검증 API
     *
     * @param session HTTP 세션
     * @return 세션 유효 여부
     */
    @GetMapping("/validate")
    public ResVO<Boolean> validateSession(HttpSession session) {
        String sessionId = (String) session.getAttribute("SESSION_ID");

        if (sessionId == null) {
            return new ResVO<>(401, "세션이 없습니다", false);
        }

        UserVO user = authService.getUserBySession(sessionId);
        boolean isValid = user != null;

        return new ResVO<>(200, "검증 완료", isValid);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        String ip = null;

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 헤더에 여러 IP가 포함된 경우, 첫 번째 IP를 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // IPv6 loopback address를 IPv4 형식으로 변환
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
