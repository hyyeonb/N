package dev3.nms.config;

import dev3.nms.service.PermissionService;
import dev3.nms.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

/**
 * 권한 체크 AOP
 * - @RequireEditPermission: 페이지 EDIT 권한
 * - @RequireGroupAccess: 그룹 VIEW/EDIT 권한
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    /**
     * 페이지 EDIT 권한 체크
     */
    @Around("@annotation(requireEditPermission)")
    public Object checkPagePermission(ProceedingJoinPoint joinPoint, RequireEditPermission requireEditPermission)
            throws Throwable {

        Long userId = getCurrentUserId();
        String pageCode = requireEditPermission.value();

        if (!permissionService.canEditPage(userId, pageCode)) {
            log.warn("[권한 거부] USER_ID: {}, pageCode: {}", userId, pageCode);
            throw new AccessDeniedException("권한이 없습니다");
        }

        return joinPoint.proceed();
    }

    /**
     * 그룹 접근 권한 체크
     * 메서드 파라미터에서 groupId를 자동 추출 (파라미터명: groupId, watchGroupId)
     */
    @Around("@annotation(requireGroupAccess)")
    public Object checkGroupPermission(ProceedingJoinPoint joinPoint, RequireGroupAccess requireGroupAccess)
            throws Throwable {

        Long userId = getCurrentUserId();
        String groupType = requireGroupAccess.groupType();
        String action = requireGroupAccess.action();

        // 메서드 파라미터에서 groupId 추출
        Long groupId = extractGroupId(joinPoint);

        if (groupId != null) {
            boolean allowed;
            if ("EDIT".equalsIgnoreCase(action)) {
                allowed = permissionService.canEditGroup(userId, groupType, groupId);
            } else {
                allowed = permissionService.canViewGroup(userId, groupType, groupId);
            }

            if (!allowed) {
                log.warn("[그룹 권한 거부] USER_ID: {}, groupType: {}, groupId: {}, action: {}",
                        userId, groupType, groupId, action);
                throw new AccessDeniedException("해당 그룹에 대한 권한이 없습니다");
            }
        }

        return joinPoint.proceed();
    }

    private Long getCurrentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new AccessDeniedException("권한을 확인할 수 없습니다");

        Long userId = SessionUtil.getUserId(attrs.getRequest().getSession(false));
        if (userId == null) throw new AccessDeniedException("로그인이 필요합니다");

        return userId;
    }

    /**
     * 메서드 파라미터에서 groupId 또는 watchGroupId 추출
     */
    private Long extractGroupId(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < params.length; i++) {
            String name = params[i].getName();
            if ("groupId".equals(name) || "watchGroupId".equals(name)) {
                Object val = args[i];
                if (val instanceof Long) return (Long) val;
                if (val instanceof Integer) return ((Integer) val).longValue();
                if (val instanceof Number) return ((Number) val).longValue();
            }
        }
        return null;
    }
}
