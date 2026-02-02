package dev3.nms.vo.deviceConfig;

import lombok.*;

public class DeviceConfigDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceConfigRes {
        private Long deviceConfigId;
        private Long deviceId;
        private String config;
        private String regDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceConfigTableRes {
        private Long deviceId;
        private Long groupId;
        private String groupNm;
        private String deviceNm;
        private String deviceIp;
        private String modelId;
        private String modelNm;
        private String deviceConfigDate;
    }
}
