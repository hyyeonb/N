package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * b_snmp_model_oid 테이블 VO
 * 모델별 SNMP OID 매핑
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnmpModelOidVO {
    private Integer OID_ID;
    private Integer METRIC_ID;
    private Integer MODEL_ID;
    private String OID;

    // JOIN용 필드
    private String OID_NAME;
    private String OID_DESC;
    private String TYPE;
}
