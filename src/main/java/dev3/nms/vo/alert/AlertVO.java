package dev3.nms.vo.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertVO {

    // === 필수 필드 ===
    private String alertId;           // UUID
    private String category;          // CONNECTION, PERFORMANCE, PORT, SYSTEM
    private String alertType;         // PING_FAIL, CPU_THRESHOLD, PORT_DOWN 등
    private String severity;          // CRITICAL, MAJOR, MINOR, WARNING, INFO
    private Integer deviceId;
    private String deviceName;
    private String deviceIp;
    private String message;
    private LocalDateTime occurredAt;
    private Boolean isCleared;        // 장애 해소 여부

    // === 선택 필드 (타입별) ===
    private Integer ifIndex;          // 포트 관련
    private String ifName;            // 포트 이름
    private String metricName;        // CPU_USAGE, MEM_USAGE, IN_BPS 등
    private Double currentValue;      // 현재 값
    private Double thresholdValue;    // 임계치
    private String unit;              // %, bps, ms 등
    private Integer duration;         // 지속 시간 (초)

    // === 메타 정보 ===
    private String groupName;         // 장비 그룹
    private String vendorName;        // 벤더
    private String modelName;         // 모델
    private Map<String, Object> extra; // 추가 데이터

    // === DB 전용 ===
    private Long id;                  // 히스토리 테이블 PK
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
