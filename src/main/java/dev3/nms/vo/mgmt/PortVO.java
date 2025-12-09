package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

/**
 * r_port_t 테이블 VO
 * 장비 포트(인터페이스) 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortVO {
    private Integer DEVICE_ID;
    private Integer IF_INDEX;
    private String IF_DESCR;
    private String IF_NAME;
    private String IF_ALIAS;
    private Integer IF_TYPE;
    private Integer IF_MTU;
    private Long IF_SPEED;
    private Integer IF_HIGH_SPEED;
    private String IF_PHYS_ADDRESS;
    private Integer IF_ADMIN_STATUS;
    private Integer IF_OPER_STATUS;
    private Long IF_LAST_CHANGE;
    private Boolean IF_OPER_FLAG;    // Oper 감시 여부
    private Boolean IF_PERF_FLAG;    // 성능 감시 여부

    // JOIN으로 가져오는 정보
    private String DEVICE_NAME;
    private String DEVICE_IP;
    private String IF_TYPE_NAME;  // R_IF_TYPE_T 테이블 조인

    // 공통 필드
    private Integer CREATE_USER_ID;
    private Timestamp CREATE_AT;
    private Integer MODIFY_USER_ID;
    private Timestamp MODIFY_AT;
    private Integer DELETE_USER_ID;
    private Timestamp DELETE_AT;

    /**
     * Admin 상태 텍스트 반환
     */
    public String getAdminStatusText() {
        if (IF_ADMIN_STATUS == null) return "알 수 없음";
        return switch (IF_ADMIN_STATUS) {
            case 1 -> "up";
            case 2 -> "down";
            case 3 -> "testing";
            default -> "unknown(" + IF_ADMIN_STATUS + ")";
        };
    }

    /**
     * Oper 상태 텍스트 반환
     */
    public String getOperStatusText() {
        if (IF_OPER_STATUS == null) return "알 수 없음";
        return switch (IF_OPER_STATUS) {
            case 1 -> "up";
            case 2 -> "down";
            case 3 -> "testing";
            case 4 -> "unknown";
            case 5 -> "dormant";
            case 6 -> "notPresent";
            case 7 -> "lowerLayerDown";
            default -> "unknown(" + IF_OPER_STATUS + ")";
        };
    }

    /**
     * 인터페이스 타입 텍스트 반환
     * R_IF_TYPE_T 테이블에서 조인한 IF_TYPE_NAME 우선 사용
     */
    public String getIfTypeText() {
        if (IF_TYPE_NAME != null && !IF_TYPE_NAME.isEmpty()) {
            return IF_TYPE_NAME;
        }
        if (IF_TYPE == null) return "알 수 없음";
        return "type(" + IF_TYPE + ")";
    }

    /**
     * 속도를 읽기 좋은 형식으로 반환
     */
    public String getSpeedText() {
        // HIGH_SPEED가 있으면 우선 사용 (Mbps)
        if (IF_HIGH_SPEED != null && IF_HIGH_SPEED > 0) {
            if (IF_HIGH_SPEED >= 1000) {
                return (IF_HIGH_SPEED / 1000) + " Gbps";
            }
            return IF_HIGH_SPEED + " Mbps";
        }
        // IF_SPEED 사용 (bps)
        if (IF_SPEED != null && IF_SPEED > 0) {
            if (IF_SPEED >= 1_000_000_000L) {
                return (IF_SPEED / 1_000_000_000L) + " Gbps";
            } else if (IF_SPEED >= 1_000_000L) {
                return (IF_SPEED / 1_000_000L) + " Mbps";
            } else if (IF_SPEED >= 1_000L) {
                return (IF_SPEED / 1_000L) + " Kbps";
            }
            return IF_SPEED + " bps";
        }
        return "-";
    }
}
