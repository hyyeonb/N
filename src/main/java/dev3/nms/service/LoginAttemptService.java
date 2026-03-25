package dev3.nms.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 시도 횟수 제한 (Brute Force 방지)
 * - IP + 로그인 ID 기반 실패 횟수 추적
 * - 5회 실패 시 15분 잠금
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    // key: "ip:loginId" or "ip:*", value: 실패 횟수
    private final Cache<String, Integer> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(LOCK_DURATION)
            .maximumSize(10_000)
            .build();

    /**
     * 로그인 실패 기록
     */
    public void loginFailed(String ip, String loginId) {
        String key = buildKey(ip, loginId);
        Integer attempts = attemptsCache.getIfPresent(key);
        int newAttempts = (attempts != null ? attempts : 0) + 1;
        attemptsCache.put(key, newAttempts);

        // IP 단위도 추적 (계정 스터핑 방지)
        String ipKey = "ip:" + ip;
        Integer ipAttempts = attemptsCache.getIfPresent(ipKey);
        attemptsCache.put(ipKey, (ipAttempts != null ? ipAttempts : 0) + 1);

        if (newAttempts >= MAX_ATTEMPTS) {
            log.warn("[보안] 로그인 시도 횟수 초과 - IP: {}, LOGIN_ID: {}, 시도: {}회", ip, loginId, newAttempts);
        }
    }

    /**
     * 로그인 성공 시 실패 카운트 초기화
     */
    public void loginSucceeded(String ip, String loginId) {
        attemptsCache.invalidate(buildKey(ip, loginId));
    }

    /**
     * 잠금 상태 확인
     */
    public boolean isBlocked(String ip, String loginId) {
        // 계정별 체크
        String key = buildKey(ip, loginId);
        Integer attempts = attemptsCache.getIfPresent(key);
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            return true;
        }

        // IP 단위 체크 (30회 초과 시 IP 차단)
        String ipKey = "ip:" + ip;
        Integer ipAttempts = attemptsCache.getIfPresent(ipKey);
        return ipAttempts != null && ipAttempts >= MAX_ATTEMPTS * 6;
    }

    /**
     * 남은 시도 횟수
     */
    public int getRemainingAttempts(String ip, String loginId) {
        Integer attempts = attemptsCache.getIfPresent(buildKey(ip, loginId));
        int used = attempts != null ? attempts : 0;
        return Math.max(0, MAX_ATTEMPTS - used);
    }

    private String buildKey(String ip, String loginId) {
        return ip + ":" + (loginId != null ? loginId : "unknown");
    }
}
