package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class TempDeviceVO {
    private int TEMP_DEVICE_ID;
    private String DEVICE_NAME;
    private int GROUP_ID;  // DEVICE_GROUP_ID → GROUP_ID
    private String DEVICE_IP;
    private Integer SNMP_VERSION;
    private Integer SNMP_PORT;
    private String SNMP_COMMUNITY;
    private String SNMP_USER;
    private String SNMP_AUTH_PROTOCOL;
    private String SNMP_AUTH_PASSWORD;
    private String SNMP_PRIV_PROTOCOL;
    private String SNMP_PRIV_PASSWORD;

    // 수집 설정
    private Boolean COLLECT_PING;
    private Boolean COLLECT_SNMP;
    private Boolean COLLECT_AGENT;

    private Integer CREATE_USER_ID;
    private Timestamp CREATE_AT;
    private Integer MODIFY_USER_ID;
    private Timestamp MODIFY_AT;
    private Integer DELETE_USER_ID;
    private Timestamp DELETE_AT;
}
