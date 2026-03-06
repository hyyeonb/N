package dev3.nms.vo.mgmt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityCheckVO {

    // PING 결과
    private boolean pingSuccess;
    private Long pingResponseTimeMs;
    private String pingMessage;

    // SNMP 결과
    private boolean snmpConfigured;   // SNMP 설정 여부
    private boolean snmpSuccess;
    private String sysName;
    private String sysDescr;
    private String snmpMessage;

    // SSH 결과
    private boolean sshConfigured;    // SSH 설정 여부
    private boolean sshSuccess;
    private Integer sshPort;
    private String sshMessage;
}
