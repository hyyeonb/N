package dev3.nms.config;

import dev3.nms.mapper.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 고아 세션 정리 스케줄러
 *
 * LOGOUT_AT IS NULL이면서 마지막 활동이 30분 이상 지난 세션을 자동으로 종료 처리.
 * Redis 세션 도입 이전에 생성된 인메모리 세션이나,
 * SessionExpiredListener가 놓친 세션을 정리.
 *
 * 5분마다 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleSessionCleanup {

    private final LoginHistoryMapper loginHistoryMapper;

    @Scheduled(fixedRate = 300_000) // 5분마다
    public void cleanupStaleSessions() {
        try {
            int cleaned = loginHistoryMapper.closeStaleSession(30);
            if (cleaned > 0) {
                log.info("[Session Cleanup] 만료 세션 {}건 종료 처리", cleaned);
            }
        } catch (Exception e) {
            log.warn("[Session Cleanup] 실패: {}", e.getMessage());
        }
    }
}
