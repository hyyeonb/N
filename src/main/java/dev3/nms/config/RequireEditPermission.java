package dev3.nms.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드에 EDIT 권한 체크를 적용하는 어노테이션.
 * AOP(PermissionAspect)에서 HttpSession의 USER_ID를 읽어
 * permissionService.canEditPage(userId, pageCode) 검증.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEditPermission {
    String value(); // pageCode
}
