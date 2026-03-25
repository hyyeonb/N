package dev3.nms.controller;

import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.service.ActivityLogService;
import dev3.nms.service.LoginHistoryService;
import dev3.nms.service.PermissionService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.auth.ActivityLogVO;
import dev3.nms.vo.auth.LoginHistoryVO;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final LoginHistoryService loginHistoryService;
    private final ActivityLogService activityLogService;
    private final LoginHistoryMapper loginHistoryMapper;
    private final PermissionService permissionService;

    /**
     * 로그인 이력 조회 (검색, 타입필터, 날짜범위, 페이지네이션)
     */
    @GetMapping("/login")
    public ResponseEntity<ResVO<PageVO<LoginHistoryVO>>> getLoginHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LOGIN_AT") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search,
            HttpSession session
    ) {
        // 비관리자는 자신의 로그인 이력만 조회 가능
        Long userId = SessionUtil.getUserId(session);
        Long filterUserId = null;
        if (userId != null && !permissionService.isAdmin(userId)) {
            filterUserId = userId;
        }

        PageVO<LoginHistoryVO> result = loginHistoryService.getLoginHistory(
                page, size, sort, order, loginType, startDate, endDate, search, filterUserId);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", result));
    }

    /**
     * 특정 로그인 세션의 활동 로그 조회
     */
    @GetMapping("/login/{historyId}/activities")
    public ResponseEntity<ResVO<PageVO<ActivityLogVO>>> getLoginActivities(
            @PathVariable Long historyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PageVO<ActivityLogVO> result = activityLogService.getActivitiesByHistoryId(historyId, page, size);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", result));
    }

    /**
     * 페이지 뷰 기록 (fire-and-forget)
     */
    @PostMapping("/page-view")
    public ResponseEntity<ResVO<Void>> recordPageView(
            @RequestBody Map<String, String> body,
            HttpSession session,
            HttpServletRequest request
    ) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ResponseEntity.ok(new ResVO<>(200, "OK", null));
        }

        Long historyId = getHistoryId(session);
        String pageCode = body.get("pageCode");
        String pagePath = body.get("pagePath");
        String targetType = body.get("targetType");
        String targetName = body.get("targetName");
        String detail = body.get("detail");
        String ipAddress = getClientIp(request);

        activityLogService.logPageView(historyId, userId, pageCode, pagePath, ipAddress,
                targetType, targetName, detail);
        return ResponseEntity.ok(new ResVO<>(200, "OK", null));
    }

    /**
     * session-end: 제거됨
     * LOGOUT_AT은 아래 두 경우에만 기록:
     *   1. 명시적 로그아웃 (AuthController.logout → session.invalidate())
     *   2. Redis 세션 만료 (SessionExpiredListener → SessionExpiredEvent/SessionDestroyedEvent)
     * beforeunload beacon은 새로고침/탭 이동에서도 발생하여 세션을 잘못 종료시킴
     */

    private Long getHistoryId(HttpSession session) {
        Object historyId = session.getAttribute("HISTORY_ID");
        if (historyId instanceof Long) return (Long) historyId;
        if (historyId instanceof Number) return ((Number) historyId).longValue();
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
