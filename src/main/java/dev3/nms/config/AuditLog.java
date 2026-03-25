package dev3.nms.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 활동 로그 기록용 어노테이션.
 * AuditLogAspect에서 @AfterReturning으로 성공한 요청만 기록.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String actionType();    // CREATE, UPDATE, DELETE
    String targetType();    // DEVICE, GROUP, MODEL, NOTICE 등
    String pageCode() default "";
}
