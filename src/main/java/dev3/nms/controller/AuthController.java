package dev3.nms.controller;

import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.service.AuthService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.auth.*;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginHistoryMapper loginHistoryMapper;

    /**
     * 소셜 로그인 API
     */
    @PostMapping("/social/login")
    public ResVO<LoginResponseVO> socialLogin(
            @RequestBody SocialLoginRequestVO request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("========== 소셜 로그인 요청 시작 ==========");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponseVO loginResponse = authService.socialLogin(request, ipAddress, userAgent);

        // HttpSession에 사용자 정보 저장
        session.setAttribute("USER_ID", loginResponse.getUSER_ID());
        session.setAttribute("HISTORY_ID", loginResponse.getHISTORY_ID());

        log.info("========== 소셜 로그인 요청 완료 ==========");
        return new ResVO<>(200, "로그인 성공", loginResponse);
    }

    /**
     * 소셜 로그인 API - Authorization Code 방식
     */
    @PostMapping("/social/code")
    public ResVO<LoginResponseVO> socialLoginWithCode(
            @RequestBody SocialCodeRequestVO request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("========== 소셜 로그인 (Code) 요청 시작 ==========");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponseVO loginResponse = authService.socialLoginWithCode(request, ipAddress, userAgent);

        // HttpSession에 사용자 정보 저장
        session.setAttribute("USER_ID", loginResponse.getUSER_ID());
        session.setAttribute("HISTORY_ID", loginResponse.getHISTORY_ID());

        log.info("========== 소셜 로그인 (Code) 요청 완료 ==========");
        return new ResVO<>(200, "로그인 성공", loginResponse);
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResVO<Void> logout(HttpSession session) {
        // 로그아웃 시 세션 종료 시각 기록
        Long historyId = getHistoryIdFromSession(session);
        if (historyId != null) {
            loginHistoryMapper.updateLogoutAt(historyId);
        }
        session.invalidate();
        return new ResVO<>(200, "로그아웃 성공", null);
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public ResVO<Map<String, Object>> getCurrentUser(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return new ResVO<>(401, "로그인이 필요합니다", null);
        }

        UserVO user = authService.getUserById(userId);
        if (user == null) {
            return new ResVO<>(401, "세션이 만료되었습니다", null);
        }

        // 사용자 정보 + 권한 정보 함께 반환
        UserPermissionVO permissions = authService.buildPermissions(user);

        Map<String, Object> result = new HashMap<>();
        result.put("USER_ID", user.getUSER_ID());
        result.put("LOGIN_ID", user.getLOGIN_ID());
        result.put("EMAIL", user.getEMAIL());
        result.put("NAME", user.getNAME());
        result.put("PHONE", user.getPHONE());
        result.put("PROFILE_IMAGE", user.getPROFILE_IMAGE());
        result.put("SOCIAL_TYPE", user.getSOCIAL_TYPE());
        result.put("STATUS", user.getSTATUS());
        result.put("permissions", permissions);

        return new ResVO<>(200, "조회 성공", result);
    }

    /**
     * 세션 유효성 검증 API
     */
    @GetMapping("/validate")
    public ResVO<Boolean> validateSession(HttpSession session, HttpServletRequest request) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return new ResVO<>(401, "세션이 없습니다", false);
        }

        UserVO user = authService.getUserById(userId);
        if (user == null) {
            return new ResVO<>(200, "검증 완료", false);
        }

        // 세션에 HISTORY_ID가 없으면 새 로그인 이력 생성 (서버 재시작 등으로 유실된 경우)
        if (getHistoryIdFromSession(session) == null) {
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String loginType = user.getSOCIAL_TYPE() != null ? user.getSOCIAL_TYPE() : "LOCAL";
            Long historyId = authService.createLoginHistory(userId, loginType, ipAddress, userAgent);
            session.setAttribute("HISTORY_ID", historyId);
            log.info("[Validate] 세션에 HISTORY_ID 복원 - USER_ID: {}, HISTORY_ID: {}", userId, historyId);
        }

        return new ResVO<>(200, "검증 완료", true);
    }

    // ==================== 로컬 회원가입/로그인 ====================

    @PostMapping("/signup")
    public ResponseEntity<ResVO<UserVO>> signup(
            @RequestBody UserVO user,
            HttpServletRequest httpRequest) {

        log.info("========== 로컬 회원가입 요청 ==========");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        UserVO savedUser = authService.signup(user, ipAddress, userAgent);
        savedUser.setPASSWORD(null);

        log.info("========== 로컬 회원가입 완료 ==========");
        return ResponseEntity.ok(new ResVO<>(200, "회원가입 성공", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ResVO<LoginResponseVO>> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        log.info("========== 로컬 로그인 요청 ==========");
        String loginId = request.get("LOGIN_ID") != null ? request.get("LOGIN_ID") : request.get("loginId");
        String password = request.get("PASSWORD") != null ? request.get("PASSWORD") : request.get("password");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponseVO loginResponse = authService.localLogin(loginId, password, ipAddress, userAgent);

            // HttpSession에 사용자 정보 저장
            session.setAttribute("USER_ID", loginResponse.getUSER_ID());
            session.setAttribute("HISTORY_ID", loginResponse.getHISTORY_ID());

            log.info("========== 로컬 로그인 완료 ==========");
            return ResponseEntity.ok(new ResVO<>(200, "로그인 성공", loginResponse));
        } catch (IllegalArgumentException e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, e.getMessage(), null));
        }
    }

    @GetMapping("/check-id")
    public ResVO<Boolean> checkLoginId(@RequestParam("loginId") String loginId) {
        boolean available = authService.isLoginIdAvailable(loginId);
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
        return new ResVO<>(200, message, available);
    }

    @GetMapping("/check-email")
    public ResVO<Boolean> checkEmail(@RequestParam("email") String email) {
        boolean available = authService.isEmailAvailable(email);
        String message = available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.";
        return new ResVO<>(200, message, available);
    }

    @GetMapping("/check-phone")
    public ResVO<Boolean> checkPhone(@RequestParam("phone") String phone) {
        boolean available = authService.isPhoneAvailable(phone);
        String message = available ? "사용 가능한 전화번호입니다." : "이미 사용 중인 전화번호입니다.";
        return new ResVO<>(200, message, available);
    }

    @PostMapping("/find-id")
    public ResponseEntity<ResVO<String>> findId(@RequestBody Map<String, String> request) {
        String name = request.get("NAME");
        String phone = request.get("PHONE");

        String loginId = authService.findLoginId(name, phone);
        if (loginId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, "일치하는 계정을 찾을 수 없습니다.", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "아이디 조회 성공", loginId));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResVO<Void>> resetPassword(@RequestBody Map<String, String> request) {
        String loginId = request.get("LOGIN_ID");
        String name = request.get("NAME");
        String phone = request.get("PHONE");
        String newPassword = request.get("NEW_PASSWORD");

        boolean success = authService.resetPassword(loginId, name, phone, newPassword);
        if (!success) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, "일치하는 계정을 찾을 수 없습니다.", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "비밀번호가 변경되었습니다.", null));
    }

    private Long getUserIdFromSession(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }

    private Long getHistoryIdFromSession(HttpSession session) {
        Object historyId = session.getAttribute("HISTORY_ID");
        if (historyId instanceof Long) return (Long) historyId;
        if (historyId instanceof Number) return ((Number) historyId).longValue();
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        String ip = null;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) break;
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        if ("0:0:0:0:0:0:0:1".equals(ip)) ip = "127.0.0.1";
        return ip;
    }
}
