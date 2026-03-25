package dev3.nms.config;

import dev3.nms.mapper.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Session 이벤트 리스너
 *
 * Redis 세션 만료/삭제 시 자동으로 로그인 이력(LOGOUT_AT) 기록.
 * - SessionExpiredEvent: maxInactiveInterval 초과로 세션 자동 만료
 * - SessionDestroyedEvent: session.invalidate() 호출 또는 Redis TTL 만료
 *
 * 이 리스너가 없으면 브라우저가 session-end beacon을 보내지 못한 경우
 * (브라우저 크래시, 네트워크 단절 등) LOGOUT_AT이 null로 남음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionExpiredListener {

    private final LoginHistoryMapper loginHistoryMapper;

    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        handleSessionEnd(event.getSessionId(), event.getSession());
    }

    @EventListener
    public void onSessionDestroyed(SessionDestroyedEvent event) {
        handleSessionEnd(event.getSessionId(), event.getSession());
    }

    private void handleSessionEnd(String sessionId, org.springframework.session.Session session) {
        if (session == null) return;

        try {
            Object historyIdObj = session.getAttribute("HISTORY_ID");
            Long historyId = toLong(historyIdObj);

            if (historyId != null) {
                loginHistoryMapper.updateLogoutAt(historyId);

                Object userIdObj = session.getAttribute("USER_ID");
                log.info("[Session] 세션 만료/종료 - sessionId={}, userId={}, historyId={}",
                        sessionId, userIdObj, historyId);
            }
        } catch (Exception e) {
            log.warn("[Session] 세션 종료 처리 실패: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    private Long toLong(Object obj) {
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        return null;
    }
}
