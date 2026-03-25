package dev3.nms.controller;

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
    @DeleteMapping("/{id}")
    public ResponseEntity<ResVO<Void>> delete(@PathVariable Integer id, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!permissionService.isAdmin(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResVO<>(403, "관리자 권한이 필요합니다", null));
        }
        try {
            middlewareService.delete(id);
            return ResponseEntity.ok(new ResVO<>(200, "삭제 성공", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ResVO<>(409, e.getMessage(), null));
        }
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

    private Long getCurrentUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }
}
