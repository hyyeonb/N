package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * b_snmp_metric_t 테이블 VO
 * SNMP 메트릭 정의
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnmpMetricVO {
    private Integer METRIC_ID;
    private String TYPE;        // CPU, MEMORY 등
    private String OID_NAME;    // CPU_USAGE, MEM_TOTAL 등
    private String OID_DESC;    // 설명
}
