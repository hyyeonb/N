package dev3.nms.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 그룹 접근 권한 체크 어노테이션.
 * AOP(PermissionAspect)에서 사용자의 그룹 VIEW/EDIT 권한을 검증.
 * 메서드 파라미터에서 groupId를 자동 추출하여 체크.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireGroupAccess {
    String groupType();       // "ASSET" or "WATCH"
    String action() default "VIEW"; // "VIEW" or "EDIT"
}
