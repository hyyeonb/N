package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * r_device_snmp_t 테이블 VO
 * DEVICE_ID를 PK이자 FK로 사용 (r_device_t 참조)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSnmpVO {
    private Integer DEVICE_ID;  // PK & FK (r_device_t.DEVICE_ID)
    private Integer SNMP_VERSION;
    private Integer SNMP_PORT;
    private String SNMP_COMMUNITY;
    private String SNMP_USER;
    private String SNMP_AUTH_PROTOCOL;
    private String SNMP_AUTH_PASSWORD;
    private String SNMP_PRIV_PROTOCOL;
    private String SNMP_PRIV_PASSWORD;
}
