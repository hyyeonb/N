package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * r_device_scope_t 테이블 VO
 * 장비 관제 설정 (Ping, SNMP, Agent 수집 여부)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceScopeVO {
    private Integer DEVICE_ID;      // FK (r_device_t.DEVICE_ID)
    private Boolean COLLECT_PING;   // Ping 수집 여부
    private Boolean COLLECT_SNMP;   // SNMP 수집 여부
    private Boolean COLLECT_AGENT;  // Agent 수집 여부
}