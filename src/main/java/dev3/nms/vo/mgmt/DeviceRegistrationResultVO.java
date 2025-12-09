package dev3.nms.vo.mgmt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class DeviceRegistrationResultVO {
    private List<DeviceRegistrationSuccess> successList;
    private List<DeviceRegistrationFailure> failureList;

    public DeviceRegistrationResultVO() {
        this.successList = new ArrayList<>();
        this.failureList = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DeviceRegistrationSuccess {
        private Integer tempDeviceId;
        private Integer deviceId;
        private String deviceName;
        private String deviceIp;
        private String systemName;      // SNMP로 가져온 시스템명
        private String vendorName;      // 벤더명
        private String deviceDesc;      // 장비 설명
        private Boolean pingResult;     // Ping 테스트 결과
        private String collectType;     // 수집 유형 (PING, SNMP, AGENT)
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DeviceRegistrationFailure {
        private Integer tempDeviceId;
        private String deviceName;
        private String deviceIp;
        private String errorMessage;
        private String collectType;     // 수집 유형 (PING, SNMP, AGENT)
    }
}
