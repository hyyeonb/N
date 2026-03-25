package dev3.nms.config;

import dev3.nms.service.ActivityLogService;
import dev3.nms.service.AuditDetailService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.auth.ActivityLogVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * @AuditLog 어노테이션이 붙은 메서드의 성공 결과를 비동기 로깅.
 * UPDATE/DELETE 시 변경 전/후 스냅샷 비교 → DETAIL 필드에 사람이 읽기 좋은 변경 내역 생성.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ActivityLogService activityLogService;
    private final AuditDetailService auditDetailService;

    @Around("@annotation(auditLog)")
    public Object logActivity(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String targetId = extractTargetId(joinPoint);
        String actionType = auditLog.actionType();
        String targetType = auditLog.targetType();

        // 1. Before: UPDATE/DELETE 시 변경 전 스냅샷
        Map<String, String> beforeSnapshot = null;
        if (("UPDATE".equals(actionType) || "DELETE".equals(actionType)) && targetId != null) {
            try {
                beforeSnapshot = auditDetailService.getSnapshot(targetType, targetId, joinPoint.getArgs());
            } catch (Exception e) {
                log.debug("[AuditLog] 스냅샷 조회 실패: {}", e.getMessage());
            }
        }

        // 2. Execute
        Object result = joinPoint.proceed();

        // 3. After: 로그 기록
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return result;

            HttpServletRequest request = attrs.getRequest();
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            Long userId = SessionUtil.getUserId(session);
            if (userId == null) return result;

            Long historyId = getHistoryId(session);
            Object requestBody = extractRequestBody(joinPoint);

            // TARGET_NAME
            String targetName = null;
            if (beforeSnapshot != null) {
                targetName = beforeSnapshot.get("_displayName");
            }

            // DETAIL 빌드
            String detail = null;
            switch (actionType) {
                case "UPDATE" -> {
                    if (beforeSnapshot != null) {
                        Map<String, String> afterValues = auditDetailService.extractNewValues(targetType, requestBody);
                        if ("WATCH_GROUP".equals(targetType)) {
                            detail = auditDetailService.buildWatchGroupUpdateDetail(beforeSnapshot, afterValues);
                        } else {
                            detail = auditDetailService.buildUpdateDetail(targetType, beforeSnapshot, afterValues);
                        }
                    }
                    // WATCH_CONTROL: 관제 시작/중지 - URI에서 액션 추출
                    if ("WATCH_CONTROL".equals(targetType) && beforeSnapshot != null) {
                        String uri = request.getRequestURI();
                        String displayName = beforeSnapshot.get("_displayName");
                        if (uri.contains("/start/")) {
                            detail = "관제 수집 시작 - " + (displayName != null ? displayName : "");
                        } else if (uri.contains("/stop/")) {
                            detail = "관제 수집 중지 - " + (displayName != null ? displayName : "");
                        }
                    }
                }
                case "DELETE" -> {
                    detail = auditDetailService.buildDeleteDetail(targetType, beforeSnapshot);
                }
                case "CREATE" -> {
                    detail = auditDetailService.buildCreateDetail(targetType, requestBody);
                    // CREATE 시 targetName 추출
                    if (detail != null && detail.contains(" - ") && targetName == null) {
                        targetName = detail.substring(detail.indexOf(" - ") + 3);
                    }
                }
            }

            ActivityLogVO logVO = ActivityLogVO.builder()
                    .HISTORY_ID(historyId)
                    .USER_ID(userId)
                    .ACTION_TYPE(actionType)
                    .TARGET_TYPE(targetType)
                    .TARGET_ID(targetId)
                    .TARGET_NAME(targetName)
                    .PAGE_CODE(auditLog.pageCode())
                    .REQUEST_METHOD(request.getMethod())
                    .REQUEST_URI(request.getRequestURI())
                    .IP_ADDRESS(getClientIp(request))
                    .DETAIL(detail)
                    .build();

            activityLogService.logActivity(logVO);
        } catch (Exception e) {
            log.warn("[AuditLog] 활동 로그 기록 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * @RequestBody 파라미터 추출
     */
    private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(RequestBody.class)) {
                return args[i];
            }
        }
        return null;
    }

    /**
     * @PathVariable에서 첫 번째 *Id 추출
     */
    private String extractTargetId(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(PathVariable.class)) {
                String name = params[i].getName();
                if (name.toLowerCase().contains("id") && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }
        return null;
    }

    private Long getHistoryId(jakarta.servlet.http.HttpSession session) {
        if (session == null) return null;
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
