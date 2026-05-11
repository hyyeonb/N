package dev3.nms.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 리소스 상세 조회 로그 어노테이션.
 * GET /{entity}/{id} 엔드포인트에 부착하면 ViewLogAspect가 자동으로 활동 로그를 기록한다.
 *
 * ACTION_TYPE = "VIEW"
 * TARGET_TYPE = 지정된 값
 * TARGET_NAME = AuditDetailService.getDisplayName(targetType, targetId)로 DB에서 자동 조회
 *
 * 사용 예:
 *   @ViewLog(targetType = "USER_PERMISSION", pageCode = "system_admin")
 *   @GetMapping("/users/{userId}/detail")
 *   public UserDetailVO getUserDetail(@PathVariable Long userId) { ... }
 *
 * NOTE: 리소스 상세 조회 엔드포인트에는 반드시 적용할 것 (docs/RULES.md 참고).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewLog {
    String targetType();            // USER_PERMISSION, DEVICE, GROUP 등
    String pageCode() default "";   // 페이지 식별자 (선택)
    String idParam() default "";    // @PathVariable 파라미터명 (기본: 첫 번째 *Id)
}
