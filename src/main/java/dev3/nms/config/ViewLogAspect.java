package dev3.nms.config;

import dev3.nms.service.ActivityLogService;
import dev3.nms.service.AuditDetailService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.auth.ActivityLogVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

/**
 * @ViewLog 어노테이션이 붙은 메서드 호출 시 리소스 상세 조회 로그를 기록.
 *
 * 동작:
 * 1. @PathVariable에서 targetId 추출
 * 2. AuditDetailService.getDisplayName(targetType, targetId)로 DB에서 실제 이름 조회
 * 3. ACTION_TYPE="VIEW", TARGET_TYPE, TARGET_ID, TARGET_NAME, DETAIL 기록
 *
 * 로그 실패 시 API 호출에 영향 없음 (try/catch 격리).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ViewLogAspect {

    private final ActivityLogService activityLogService;
    private final AuditDetailService auditDetailService;

    @Around("@annotation(viewLog)")
    public Object logView(ProceedingJoinPoint joinPoint, ViewLog viewLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return result;

            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            Long userId = SessionUtil.getUserId(session);
            if (userId == null) return result;

            Long historyId = getHistoryId(session);
            String targetType = viewLog.targetType();
            String pageCode = viewLog.pageCode().isEmpty() ? null : viewLog.pageCode();

            String targetId = extractTargetId(joinPoint, viewLog.idParam());
            if (targetId == null) return result;

            // DB에서 실제 이름 조회 (가장 신뢰할 수 있는 소스)
            String targetName = auditDetailService.getDisplayName(targetType, targetId);
            String label = auditDetailService.getTargetLabel(targetType);
            String detail = label + " 조회 - " + (targetName != null ? targetName : ("ID=" + targetId));

            ActivityLogVO logVO = new ActivityLogVO();
            logVO.setUSER_ID(userId);
            logVO.setHISTORY_ID(historyId);
            logVO.setACTION_TYPE("VIEW");
            logVO.setTARGET_TYPE(targetType);
            logVO.setTARGET_ID(targetId);
            logVO.setTARGET_NAME(targetName);
            logVO.setDETAIL(detail);
            logVO.setPAGE_CODE(pageCode);
            logVO.setIP_ADDRESS(getClientIp(request));

            activityLogService.logActivity(logVO);
        } catch (Exception e) {
            log.debug("[ViewLog] 기록 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * @PathVariable에서 targetId 추출.
     * idParam이 지정되어 있으면 해당 이름의 파라미터, 없으면 첫 번째 *Id 파라미터 사용.
     */
    private String extractTargetId(ProceedingJoinPoint joinPoint, String idParam) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        // 1. idParam 지정 시 우선 사용
        if (idParam != null && !idParam.isEmpty()) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(PathVariable.class)
                        && idParam.equals(params[i].getName())
                        && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }

        // 2. 첫 번째 *Id PathVariable
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

    private Long getHistoryId(HttpSession session) {
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
