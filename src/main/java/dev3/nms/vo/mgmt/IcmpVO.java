package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * P_ICMP_T 테이블 VO
 * Ping(ICMP) 응답 데이터
 *
 * 주의: 컬럼명 COLLECT_TIME (다른 성능 테이블은 COLLECTED_AT)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IcmpVO {
    private Long ICMP_ID;
    private Integer DEVICE_ID;
    private Float RESPONSE_TIME;
    private Float RESPONSE_MIN_TIME;
    private Float RESPONSE_MAX_TIME;
    private Float PACKET_LOSS;
    private LocalDateTime COLLECT_TIME;
}
