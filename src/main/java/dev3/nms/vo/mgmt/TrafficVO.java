package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * P_TRAFFIC_T 테이블 VO
 * 포트별 트래픽 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficVO {
    private Integer DEVICE_ID;
    private Integer IF_INDEX;
    private LocalDateTime COLLECTED_AT;

    // Raw values (누적)
    private Long IN_OCTET;
    private Long IN_HIGH_OCTET;
    private Long OUT_OCTET;
    private Long OUT_HIGH_OCTET;
    private Long IN_DISCARD;
    private Long IN_ERROR;
    private Long OUT_DISCARD;
    private Long OUT_ERROR;

    // Used values (차이값)
    private Long IN_USED;
    private Long IN_HIGH_USED;
    private Long OUT_USED;
    private Long OUT_HIGH_USED;
    private Long IN_DISCARD_USED;
    private Long IN_ERROR_USED;
    private Long OUT_DISCARD_USED;
    private Long OUT_ERROR_USED;

    // BPS values (초당 비트)
    private Double IN_BPS;
    private Double IN_HIGH_BPS;
    private Double OUT_BPS;
    private Double OUT_HIGH_BPS;

    // JOIN으로 가져오는 정보
    private String IF_NAME;
    private String IF_DESCR;

    /**
     * 입력 BPS 반환 (64비트 우선)
     */
    public Double getInBpsValue() {
        if (IN_HIGH_BPS != null && IN_HIGH_BPS > 0) {
            return IN_HIGH_BPS;
        }
        return IN_BPS;
    }

    /**
     * 출력 BPS 반환 (64비트 우선)
     */
    public Double getOutBpsValue() {
        if (OUT_HIGH_BPS != null && OUT_HIGH_BPS > 0) {
            return OUT_HIGH_BPS;
        }
        return OUT_BPS;
    }

    /**
     * 포트 이름 반환 (IF_NAME 우선, IF_DESCR 차선)
     */
    public String getPortName() {
        if (IF_NAME != null && !IF_NAME.isEmpty()) {
            return IF_NAME;
        }
        if (IF_DESCR != null && !IF_DESCR.isEmpty()) {
            return IF_DESCR;
        }
        return "Port " + IF_INDEX;
    }
}
