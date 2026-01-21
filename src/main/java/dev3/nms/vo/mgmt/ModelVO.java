package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

/**
 * r_model_t 테이블 VO
 * 장비 모델 정보 (SYSOID 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelVO {
    private Integer MODEL_ID;
    private Integer VENDOR_ID;
    private Long DEV_CODE_ID;   // 장비군 코드 (b_dev_code_t FK)
    private String MODEL_OID;   // SYSOID에서 벤더 OID 제외한 나머지 부분
    private String MODEL_NAME;  // 모델 이름 (자동 수집 모델 등)

    // JOIN으로 가져오는 벤더 정보
    private String VENDOR_NAME;

    // JOIN으로 가져오는 장비군 정보
    private String DEV_CODE_NM;

    // 공통 필드
    private Integer CREATE_USER_ID;
    private Timestamp CREATE_AT;
    private Integer MODIFY_USER_ID;
    private Timestamp MODIFY_AT;
    private Integer DELETE_USER_ID;
    private Timestamp DELETE_AT;
}
