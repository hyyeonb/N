package dev3.nms.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev3.nms.mapper.AlertMapper;
import dev3.nms.vo.alert.AlertVO;
import dev3.nms.vo.common.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AlertMapper alertMapper;

    // 중복 알림 방지 캐시 (5분)
    private final Cache<String, Boolean> recentAlerts = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    /**
     * 알림 수신 및 처리 (메인 진입점)
     */
    @Transactional
    public void processAlert(AlertVO alert) {
        // 1. 알림 ID 생성
        if (alert.getAlertId() == null || alert.getAlertId().isEmpty()) {
            alert.setAlertId(UUID.randomUUID().toString());
        }

        // 2. 발생 시간 설정
        if (alert.getOccurredAt() == null) {
            alert.setOccurredAt(LocalDateTime.now());
        }

        // 3. 중복 체크 (동일 장비/타입 5분 이내) - 해소 알림은 중복 체크 안함
        String cacheKey = generateCacheKey(alert);
        if (Boolean.FALSE.equals(alert.getIsCleared()) && recentAlerts.getIfPresent(cacheKey) != null) {
            log.debug("중복 알림 무시: {}", cacheKey);
            return;
        }
        if (Boolean.FALSE.equals(alert.getIsCleared())) {
            recentAlerts.put(cacheKey, true);
        }

        // 4. DB 저장 (히스토리)
        saveAlertHistory(alert);

        // 5. 현재 장애 테이블 업데이트
        updateCurrentAlerts(alert);

        // 6. WebSocket 브로드캐스트
        broadcastAlert(alert);

        log.info("[Alert] {} - {} ({}) - {}",
                alert.getSeverity(), alert.getAlertType(),
                alert.getDeviceIp(), alert.getMessage());
    }

    /**
     * 배치 알림 처리
     */
    @Transactional
    public void processAlerts(List<AlertVO> alerts) {
        for (AlertVO alert : alerts) {
            processAlert(alert);
        }
    }

    /**
     * WebSocket 브로드캐스트
     */
    private void broadcastAlert(AlertVO alert) {
        // 전체 채널
        messagingTemplate.convertAndSend("/topic/alerts", alert);

        // 카테고리별 채널
        if (alert.getCategory() != null) {
            String categoryTopic = "/topic/alerts/" + alert.getCategory().toLowerCase();
            messagingTemplate.convertAndSend(categoryTopic, alert);
        }

        log.debug("[WebSocket] 알림 브로드캐스트: {}", alert.getAlertType());
    }

    /**
     * 중복 체크용 키 생성
     */
    private String generateCacheKey(AlertVO alert) {
        String ifIndexPart = alert.getIfIndex() != null ? String.valueOf(alert.getIfIndex()) : "device";
        return String.format("%d:%s:%s",
                alert.getDeviceId(),
                alert.getAlertType(),
                ifIndexPart
        );
    }

    /**
     * 알림 히스토리 저장
     */
    private void saveAlertHistory(AlertVO alert) {
        try {
            alertMapper.insertAlertHistory(alert);
        } catch (Exception e) {
            log.error("알림 히스토리 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 현재 장애 상태 업데이트
     */
    private void updateCurrentAlerts(AlertVO alert) {
        try {
            if (Boolean.TRUE.equals(alert.getIsCleared())) {
                // 장애 해소 - 현재 장애 테이블에서 삭제
                alertMapper.deleteCurrentAlert(
                        alert.getDeviceId(),
                        getClearTargetType(alert.getAlertType()),
                        alert.getIfIndex()
                );
            } else {
                // 신규 장애 - 현재 장애 테이블에 추가/갱신
                alertMapper.upsertCurrentAlert(alert);
            }
        } catch (Exception e) {
            log.error("현재 장애 테이블 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 해소 알림 타입에서 원본 장애 타입 추출
     * PING_CLEAR -> PING_FAIL, THRESHOLD_CLEAR -> CPU_THRESHOLD 등
     */
    private String getClearTargetType(String alertType) {
        if (alertType == null) return null;

        return switch (alertType) {
            case "PING_CLEAR" -> "PING_FAIL";
            case "SNMP_CLEAR" -> "SNMP_FAIL";
            case "PORT_UP" -> "PORT_DOWN";
            case "THRESHOLD_CLEAR" -> null; // 별도 처리 필요
            default -> alertType.replace("_CLEAR", "_FAIL");
        };
    }

    /**
     * 현재 장애 목록 조회
     */
    public List<AlertVO> getCurrentAlerts(String category, String severity) {
        return alertMapper.selectCurrentAlerts(category, severity);
    }

    /**
     * 특정 장비의 현재 장애 조회
     */
    public List<AlertVO> getCurrentAlertsByDeviceId(Integer deviceId) {
        return alertMapper.selectCurrentAlertsByDeviceId(deviceId);
    }

    /**
     * 현재 장애 수 조회
     */
    public int countCurrentAlerts(String category, String severity) {
        return alertMapper.countCurrentAlerts(category, severity);
    }

    /**
     * 알림 히스토리 조회
     */
    public PageVO<AlertVO> getAlertHistory(int page, int size, String category,
                                           Integer deviceId, String startDate, String endDate) {
        int offset = (page - 1) * size;
        List<AlertVO> list = alertMapper.selectAlertHistory(category, deviceId, startDate, endDate, offset, size);
        int total = alertMapper.countAlertHistory(category, deviceId, startDate, endDate);

        return PageVO.of(list, page, size, total);
    }
}
