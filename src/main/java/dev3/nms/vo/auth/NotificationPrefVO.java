package dev3.nms.vo.auth;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPrefVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long USER_ID;

    // 알림 유형별 ON/OFF
    private Boolean NOTIFY_SNMP;
    private Boolean NOTIFY_ICMP;
    private Boolean NOTIFY_PORT;
    private Boolean NOTIFY_CPU_MEM;
    private Boolean NOTIFY_TRAFFIC;
    private Boolean NOTIFY_URGENT;

    // 등급별 ON/OFF
    private Boolean NOTIFY_CRITICAL;
    private Boolean NOTIFY_MAJOR;
    private Boolean NOTIFY_MINOR;
    private Boolean NOTIFY_WARNING;

    // 사운드 설정
    private Boolean SOUND_ENABLED;
    private Integer SOUND_VOLUME;
    private String SOUND_TYPE;

    // 브라우저 알림
    private Boolean BROWSER_NOTIFY;

    // 방해금지
    private Boolean QUIET_ENABLED;
    private LocalTime QUIET_START_TIME;
    private LocalTime QUIET_END_TIME;

    private LocalDateTime CREATE_AT;
    private LocalDateTime MODIFY_AT;
}
