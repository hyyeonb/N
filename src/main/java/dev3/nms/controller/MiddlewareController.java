package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.service.MiddlewareService;
import dev3.nms.service.PermissionService;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.MiddlewareVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/middleware")
@RequiredArgsConstructor
public class MiddlewareController {

    private final MiddlewareService middlewareService;
    private final PermissionService permissionService;

    /**
     * 미들웨어 목록 조회
     */
    @GetMapping
    public ResponseEntity<ResVO<List<MiddlewareVO>>> list() {
        List<MiddlewareVO> list = middlewareService.getAll();
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", list));
    }

    /**
     * 미들웨어 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResVO<MiddlewareVO>> detail(@PathVariable Integer id) {
        MiddlewareVO mw = middlewareService.getById(id);
        if (mw == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, "미들웨어를 찾을 수 없습니다", null));
        }
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", mw));
    }

    /**
     * 미들웨어 등록 (관리자 전용)
     */
    @AuditLog(actionType = "CREATE", targetType = "MIDDLEWARE", pageCode = "system_admin")
    @PostMapping
    public ResponseEntity<ResVO<Void>> create(@RequestBody MiddlewareVO mw, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        middlewareService.create(mw);
        return ResponseEntity.ok(new ResVO<>(200, "등록 성공", null));
    }

    /**
     * 미들웨어 수정 (관리자 전용)
     */
    @AuditLog(actionType = "UPDATE", targetType = "MIDDLEWARE", pageCode = "system_admin")
    @PutMapping("/{id}")
    public ResponseEntity<ResVO<Void>> update(@PathVariable Integer id,
                                               @RequestBody MiddlewareVO mw,
                                               HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        middlewareService.update(id, mw);
        return ResponseEntity.ok(new ResVO<>(200, "수정 성공", null));
    }

    /**
     * 미들웨어 삭제 (관리자 전용)
     */
    @AuditLog(actionType = "DELETE", targetType = "MIDDLEWARE", pageCode = "system_admin")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResVO<Void>> delete(@PathVariable Integer id, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        middlewareService.delete(id);
        return ResponseEntity.ok(new ResVO<>(200, "삭제 성공 (할당 장비 자동 재분배)", null));
    }

    /**
     * 미들웨어 헬스체크
     */
    @PostMapping("/{id}/health")
    public ResponseEntity<ResVO<MiddlewareVO>> healthCheck(@PathVariable Integer id) {
        try {
            MiddlewareVO result = middlewareService.healthCheck(id);
            return ResponseEntity.ok(new ResVO<>(200, "헬스체크 완료", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        }
    }

    /**
     * 미들웨어 API Key 자동 등록 (Go 시작 시 호출, 인증 불필요)
     */
    @PostMapping("/register-key")
    public ResponseEntity<ResVO<Integer>> registerKey(@RequestBody java.util.Map<String, Object> body) {
        try {
            int middlewareId = body.get("middlewareId") != null ? ((Number) body.get("middlewareId")).intValue() : 0;
            String apiKey = (String) body.get("apiKey");
            String url = (String) body.get("url");

            if (apiKey == null || apiKey.isBlank()) {
                return ResponseEntity.badRequest().body(new ResVO<>(400, "apiKey 필수", null));
            }

            Integer assignedId = middlewareService.registerKey(middlewareId, apiKey, url);
            return ResponseEntity.ok(new ResVO<>(200, "API Key 등록 완료", assignedId));
        } catch (Exception e) {
            log.error("API Key 등록 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, e.getMessage(), null));
        }
    }

    private Long getCurrentUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }
}
