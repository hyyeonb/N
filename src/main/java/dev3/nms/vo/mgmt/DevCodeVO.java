package dev3.nms.vo.mgmt;

import lombok.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * b_dev_code_t 테이블 VO
 * 장비군(Device Code) 정보 - 트리 구조
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevCodeVO {
    private Long DEV_CODE_ID;
    private Long PARENT_DEV_CODE_ID;
    private String CODE_NM;
    private String CODE_DESCRIPTION;
    private Timestamp CREATE_AT;
    private Timestamp UPDATE_AT;

    // 트리 구조를 위한 자식 노드 목록
    private List<DevCodeVO> children;
}
