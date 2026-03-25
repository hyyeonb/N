package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.service.AdminService;
import dev3.nms.service.PermissionService;
import dev3.nms.service.ThresholdService;
import dev3.nms.vo.auth.*;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.ThresholdVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PermissionService permissionService;
    private final ThresholdService thresholdService;

    /**
     * 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<ResVO<List<UserVO>>> getUsers(HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        List<UserVO> users = adminService.getAllUsers();
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", users));
    }

    /**
     * 사용자 상세 + 권한 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ResVO<Map<String, Object>>> getUserDetail(
            @PathVariable Long userId, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        try {
            Map<String, Object> detail = adminService.getUserDetail(userId);
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        }
    }

    /**
     * 페이지 권한 일괄 수정
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_PERMISSION", pageCode = "system_admin")
    @PutMapping("/users/{userId}/page-access")
    public ResponseEntity<ResVO<Void>> updatePageAccess(
            @PathVariable Long userId,
            @RequestBody List<PageAccessVO> accessList,
            HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.updatePageAccess(userId, accessList, currentUserId);
        return ResponseEntity.ok(new ResVO<>(200, "페이지 권한 수정 성공", null));
    }

    /**
     * 그룹 권한 수정
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_PERMISSION", pageCode = "system_admin")
    @PutMapping("/users/{userId}/group-access")
    public ResponseEntity<ResVO<Void>> updateGroupAccess(
            @PathVariable Long userId,
            @RequestBody List<GroupAccessVO> accessList,
            HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.updateGroupAccess(userId, accessList, currentUserId);
        return ResponseEntity.ok(new ResVO<>(200, "그룹 권한 수정 성공", null));
    }

    /**
     * 계정 상태 변경
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_STATUS", pageCode = "system_admin")
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ResVO<Void>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.updateUserStatus(userId, body.get("STATUS"));
        return ResponseEntity.ok(new ResVO<>(200, "상태 변경 성공", null));
    }

    /**
     * 전체 그룹 조회 토글
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_SETTING", pageCode = "system_admin")
    @PutMapping("/users/{userId}/all-group-view")
    public ResponseEntity<ResVO<Void>> updateAllGroupView(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body,
            HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.updateAllGroupView(userId, body.get("ALL_GROUP_VIEW"));
        return ResponseEntity.ok(new ResVO<>(200, "전체 그룹 조회 설정 변경 성공", null));
    }

    /**
     * 사용자 확인 처리
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_REVIEW", pageCode = "system_admin")
    @PostMapping("/users/{userId}/review")
    public ResponseEntity<ResVO<Void>> reviewUser(
            @PathVariable Long userId, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.reviewUser(userId);
        return ResponseEntity.ok(new ResVO<>(200, "사용자 확인 처리 완료", null));
    }

    /**
     * 페이지 마스터 목록 조회
     */
    @GetMapping("/pages")
    public ResponseEntity<ResVO<List<PageMasterVO>>> getPages(HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        List<PageMasterVO> pages = adminService.getAllPages();
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", pages));
    }

    /**
     * 권한 복사
     */
    @AuditLog(actionType = "UPDATE", targetType = "USER_PERMISSION", pageCode = "system_admin")
    @PostMapping("/users/{userId}/copy-from/{sourceUserId}")
    public ResponseEntity<ResVO<Void>> copyPermissions(
            @PathVariable Long userId,
            @PathVariable Long sourceUserId,
            HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        adminService.copyPermissions(userId, sourceUserId, currentUserId);
        return ResponseEntity.ok(new ResVO<>(200, "권한 복사 성공", null));
    }

    // ==================== 임계치 관리 ====================

    @GetMapping("/thresholds")
    public ResponseEntity<ResVO<List<ThresholdVO>>> getThresholds(HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", thresholdService.getBaseThresholds()));
    }

    @AuditLog(actionType = "UPDATE", targetType = "THRESHOLD", pageCode = "system_admin")
    @PutMapping("/thresholds")
    public ResponseEntity<ResVO<Void>> updateThresholds(@RequestBody List<ThresholdVO> thresholds, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        try {
            thresholdService.updateBaseThresholds(thresholds);
            return ResponseEntity.ok(new ResVO<>(200, "임계치가 저장되었습니다.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResVO<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/thresholds/devices")
    public ResponseEntity<ResVO<List<ThresholdVO>>> getDeviceThresholds(HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", thresholdService.getDeviceThresholds()));
    }

    @GetMapping("/thresholds/devices/{deviceId}")
    public ResponseEntity<ResVO<List<ThresholdVO>>> getDeviceThreshold(@PathVariable String deviceId, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", thresholdService.getDeviceThreshold(deviceId)));
    }

    @AuditLog(actionType = "UPDATE", targetType = "DEVICE_THRESHOLD", pageCode = "system_admin")
    @PutMapping("/thresholds/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> upsertDeviceThresholds(@PathVariable String deviceId, @RequestBody List<ThresholdVO> thresholds, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        try {
            thresholdService.upsertDeviceThresholds(deviceId, thresholds);
            return ResponseEntity.ok(new ResVO<>(200, "장비별 임계치가 저장되었습니다.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResVO<>(400, e.getMessage(), null));
        }
    }

    @AuditLog(actionType = "DELETE", targetType = "DEVICE_THRESHOLD", pageCode = "system_admin")
    @DeleteMapping("/thresholds/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> deleteDeviceThresholds(@PathVariable String deviceId, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        thresholdService.deleteDeviceThresholds(deviceId);
        return ResponseEntity.ok(new ResVO<>(200, "장비별 임계치가 삭제되었습니다.", null));
    }

    private Long getCurrentUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }
}
