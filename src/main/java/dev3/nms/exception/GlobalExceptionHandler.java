package dev3.nms.exception;

import dev3.nms.vo.common.ResVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 중복 이메일 예외 처리 (기존)
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ResVO<Object>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.error("중복 이메일 오류 (EmailAlreadyExistsException): {}", ex.getMessage());
        ResVO<Object> response = new ResVO<>(HttpStatus.CONFLICT.value(), ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 중복 이메일 예외 처리 (신규 - 소셜 로그인용)
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ResVO<Object>> handleDuplicateEmailException(DuplicateEmailException ex) {
        log.warn("중복 이메일: {} (기존: {}, 시도: {})",
                ex.getEmail(), ex.getExistingSocialType(), ex.getAttemptedSocialType());

        ResVO<Object> response = new ResVO<>(HttpStatus.CONFLICT.value(), ex.getMessage(),
                Map.of("socialType", ex.getExistingSocialType()));
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * DB 중복 키 예외 처리 (DuplicateKeyException - MyBatis/JDBC)
     * EMAIL, LOGIN_ID 등 UNIQUE 제약 조건 위반 시 발생
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResVO<Object>> handleDuplicateKeyException(DuplicateKeyException ex) {
        String message = ex.getMessage();
        String userMessage = "중복된 데이터가 존재합니다.";

        // 에러 메시지에서 어떤 필드가 중복인지 파악
        if (message != null) {
            if (message.contains("EMAIL")) {
                userMessage = "이미 사용 중인 이메일입니다.";
            } else if (message.contains("LOGIN_ID")) {
                userMessage = "이미 사용 중인 아이디입니다.";
            }
        }

        log.warn("DB 중복 키 오류: {}", userMessage);
        ResVO<Object> response = new ResVO<>(HttpStatus.CONFLICT.value(), userMessage, null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResVO<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("잘못된 요청: {}", ex.getMessage());
        ResVO<Object> response = new ResVO<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * NoResourceFoundException 처리 (favicon.ico 등 정적 리소스 없을 때)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResVO<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        // favicon.ico 요청은 로그 출력하지 않음
        String resourcePath = ex.getResourcePath();
        if (resourcePath != null && resourcePath.contains("favicon.ico")) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 그 외 리소스는 로그 출력
        log.warn("리소스를 찾을 수 없음: {}", resourcePath);
        ResVO<Object> response = new ResVO<>(HttpStatus.NOT_FOUND.value(),
                "요청한 리소스를 찾을 수 없습니다", null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResVO<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("런타임 오류 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "처리 중 오류가 발생했습니다: " + ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResVO<Object>> handleGeneralException(Exception ex) {
        log.error("예상치 못한 오류 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 오류가 발생했습니다: " + ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
