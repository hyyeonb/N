package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * b_common_oid 테이블 VO
 * 공통 OID 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommonOidVO {
    private String OID_NAME;
    private String OID;
}