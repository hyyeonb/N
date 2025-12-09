package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class DeviceVO {
    private Integer DEVICE_ID;
    private Integer GROUP_ID;  // DEVICE_GROUP_ID → GROUP_ID
    private String GROUP_NAME; // 그룹명 (JOIN으로 가져옴)
    private String DEVICE_NAME;
    private String DEVICE_SYSTEM_NAME;
    private String DEVICE_IP;
    private String DEVICE_DESC;
    private Integer MODEL_ID;  // VENDOR_ID → MODEL_ID (r_model_t 참조)
    private Integer PORT_COUNT; // 포트 수

    // JOIN으로 가져오는 정보
    private String VENDOR_NAME;  // model → vendor 조인으로 가져옴
    private String MODEL_NAME;   // model 테이블에서 가져옴
    private String MODEL_OID;    // model 테이블에서 가져옴

    // SNMP 정보 (r_device_snmp_t 조인으로 가져옴)
    private Integer SNMP_PORT;
    private Integer SNMP_VERSION;
    private String SNMP_COMMUNITY;
    private String SNMP_USER;
    private String SNMP_AUTH_PROTOCOL;
    private String SNMP_AUTH_PASSWORD;
    private String SNMP_PRIV_PROTOCOL;
    private String SNMP_PRIV_PASSWORD;

    // 공통 필드
    private Integer CREATE_USER_ID;
    private Timestamp CREATE_AT;
    private Integer MODIFY_USER_ID;
    private Timestamp MODIFY_AT;
    private Integer DELETE_USER_ID;
    private Timestamp DELETE_AT;
}
