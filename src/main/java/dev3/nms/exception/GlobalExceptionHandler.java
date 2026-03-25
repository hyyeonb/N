package dev3.nms.exception;

import dev3.nms.vo.common.ResVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * - 사용자에게는 안전한 메시지만 반환
 * - 상세 Exception 원문은 서버 로그에만 기록
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 권한 부족 예외 처리 (Spring Security)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResVO<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("접근 거부: {}", ex.getMessage());
        ResVO<Object> response = new ResVO<>(HttpStatus.FORBIDDEN.value(), "권한이 없습니다", null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

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
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResVO<Object>> handleDuplicateKeyException(DuplicateKeyException ex) {
        String message = ex.getMessage();
        String userMessage = "중복된 데이터가 존재합니다.";

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
     * IllegalArgumentException 처리 — 비즈니스 로직에서 의도적으로 던진 검증 오류
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResVO<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("잘못된 요청: {}", ex.getMessage());
        ResVO<Object> response = new ResVO<>(HttpStatus.BAD_REQUEST.value(),
                sanitizeMessage(ex.getMessage(), "잘못된 요청입니다."), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 요청 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResVO<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("필수 파라미터 누락: {}", ex.getParameterName());
        ResVO<Object> response = new ResVO<>(HttpStatus.BAD_REQUEST.value(),
                "필수 파라미터가 누락되었습니다: " + ex.getParameterName(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResVO<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("파라미터 타입 오류: {} = {}", ex.getName(), ex.getValue());
        ResVO<Object> response = new ResVO<>(HttpStatus.BAD_REQUEST.value(),
                "파라미터 형식이 올바르지 않습니다: " + ex.getName(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 요청 본문 파싱 실패 (잘못된 JSON 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResVO<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("요청 본문 파싱 실패: {}", ex.getMessage());
        ResVO<Object> response = new ResVO<>(HttpStatus.BAD_REQUEST.value(),
                "요청 데이터 형식이 올바르지 않습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResVO<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("지원하지 않는 HTTP 메서드: {}", ex.getMethod());
        ResVO<Object> response = new ResVO<>(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "지원하지 않는 요청 방식입니다.", null);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * NoResourceFoundException 처리 (favicon.ico 등 정적 리소스 없을 때)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResVO<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        String resourcePath = ex.getResourcePath();
        if (resourcePath != null && resourcePath.contains("favicon.ico")) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.warn("리소스를 찾을 수 없음: {}", resourcePath);
        ResVO<Object> response = new ResVO<>(HttpStatus.NOT_FOUND.value(),
                "요청한 리소스를 찾을 수 없습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * NullPointerException 처리 — 내부 로직 오류이므로 상세 내용 숨김
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ResVO<Object>> handleNullPointerException(NullPointerException ex) {
        log.error("NullPointerException 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "요청 처리 중 오류가 발생했습니다. 입력값을 확인해주세요.", null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * DB 접근 오류 (DataAccessException — MyBatis/JDBC 공통 상위)
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ResVO<Object>> handleDataAccessException(DataAccessException ex) {
        log.error("데이터베이스 오류 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "데이터 처리 중 오류가 발생했습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * RuntimeException 처리 — 상세 내용은 로그에만 기록
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResVO<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("런타임 오류 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "처리 중 오류가 발생했습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 일반 예외 처리 — 최종 안전망, 상세 내용 절대 노출 금지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResVO<Object>> handleGeneralException(Exception ex) {
        log.error("예상치 못한 오류 발생", ex);
        ResVO<Object> response = new ResVO<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 오류가 발생했습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 메시지 안전성 검사 — Java 내부 예외 패턴이 포함되면 기본 메시지로 대체
     */
    private String sanitizeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) return fallback;
        // Java 내부 예외 패턴 감지
        if (message.contains("java.") || message.contains("Cannot invoke")
                || message.contains("NullPointer") || message.contains("ClassCast")
                || message.contains("StackOverflow") || message.contains("OutOfMemory")) {
            return fallback;
        }
        return message;
    }
}
