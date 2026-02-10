package dev3.nms.vo.mgmt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSshVO {
    private Long DEVICE_SSH_ID;
    private Integer DEVICE_ID;
    private String SSH_USER;
    private String SSH_PASS;
    private Integer SSH_PORT;
    private String CONNECT_AS;      // SSH or TELNET
    private java.sql.Timestamp REG_DT;
    private java.sql.Timestamp UPT_DT;
    private java.sql.Timestamp DEL_DT;
}
