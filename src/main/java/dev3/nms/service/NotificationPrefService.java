package dev3.nms.service;

import dev3.nms.mapper.NotificationPrefMapper;
import dev3.nms.vo.auth.NotificationPrefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPrefService {

    private final NotificationPrefMapper notificationPrefMapper;

    /**
     * 알림 환경설정 조회 (없으면 기본값 자동 생성)
     */
    @Transactional
    public NotificationPrefVO getPreferences(Long userId) {
        return notificationPrefMapper.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * 알림 환경설정 저장
     */
    @Transactional
    public void updatePreferences(Long userId, NotificationPrefVO pref) {
        pref.setUSER_ID(userId);

        if (notificationPrefMapper.findByUserId(userId).isPresent()) {
            notificationPrefMapper.update(pref);
        } else {
            notificationPrefMapper.insert(pref);
        }

        log.info("[NotificationPref] 설정 저장 - USER_ID: {}", userId);
    }

    /**
     * 기본 환경설정 생성
     */
    private NotificationPrefVO createDefaultPreferences(Long userId) {
        NotificationPrefVO pref = NotificationPrefVO.builder()
                .USER_ID(userId)
                .NOTIFY_SNMP(true)
                .NOTIFY_ICMP(true)
                .NOTIFY_PORT(true)
                .NOTIFY_CPU_MEM(true)
                .NOTIFY_TRAFFIC(true)
                .NOTIFY_URGENT(true)
                .NOTIFY_CRITICAL(true)
                .NOTIFY_MAJOR(true)
                .NOTIFY_MINOR(true)
                .NOTIFY_WARNING(true)
                .SOUND_ENABLED(true)
                .SOUND_VOLUME(30)
                .SOUND_TYPE("SINE")
                .BROWSER_NOTIFY(false)
                .QUIET_ENABLED(false)
                .build();

        notificationPrefMapper.insert(pref);
        log.info("[NotificationPref] 기본 설정 생성 - USER_ID: {}", userId);
        return pref;
    }
}
