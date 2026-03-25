package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.service.AccountService;
import dev3.nms.service.PermissionService;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final PermissionService permissionService;

    /**
     * 계정 정보 조회 (프로필 + 최근 로그인 이력)
     */
    @GetMapping
    public ResponseEntity<ResVO<Map<String, Object>>> getAccountInfo(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        Map<String, Object> accountInfo = accountService.getAccountInfo(userId);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", accountInfo));
    }

    /**
     * 프로필 수정 (이름, 이메일, 전화번호)
     */
    @AuditLog(actionType = "UPDATE", targetType = "ACCOUNT", pageCode = "account_settings")
    @PutMapping("/profile")
    public ResponseEntity<ResVO<Void>> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        try {
            accountService.updateProfile(userId, body.get("NAME"), body.get("EMAIL"), body.get("PHONE"));
            return ResponseEntity.ok(new ResVO<>(200, "프로필이 수정되었습니다.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ResVO<>(400, e.getMessage(), null));
        }
    }

    /**
     * 비밀번호 변경 (LOCAL 유저만)
     */
    @AuditLog(actionType = "UPDATE", targetType = "PASSWORD", pageCode = "account_settings")
    @PutMapping("/password")
    public ResponseEntity<ResVO<Void>> changePassword(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        try {
            accountService.changePassword(userId, body.get("CURRENT_PASSWORD"), body.get("NEW_PASSWORD"));
            return ResponseEntity.ok(new ResVO<>(200, "비밀번호가 변경되었습니다.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ResVO<>(400, e.getMessage(), null));
        }
    }

    /**
     * 프로필 이미지 업로드 (Base64로 DB 저장)
     */
    @PostMapping("/profile-image")
    public ResponseEntity<ResVO<String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ResVO<>(400, "파일을 선택해주세요.", null));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(new ResVO<>(400, "이미지 파일만 업로드 가능합니다.", null));
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new ResVO<>(400, "파일 크기는 2MB 이하여야 합니다.", null));
            }

            // Base64 data URI로 변환
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + contentType + ";base64," + base64;

            accountService.updateProfileImage(userId, dataUri);

            return ResponseEntity.ok(new ResVO<>(200, "프로필 이미지가 변경되었습니다.", dataUri));
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "이미지 업로드에 실패했습니다.", null));
        }
    }

    /**
     * 접근 가능 장비 ID 목록 조회 (알림 필터링용)
     * null 반환 = 전체 허용 (admin/ALL_GROUP_VIEW)
     */
    @GetMapping("/accessible-devices")
    public ResponseEntity<ResVO<List<Long>>> getAccessibleDevices(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        List<Long> deviceIds = permissionService.getAccessibleDeviceIds(userId);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", deviceIds));
    }

    private Long getUserIdFromSession(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }
}
