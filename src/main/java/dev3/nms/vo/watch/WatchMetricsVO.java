package dev3.nms.vo.watch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관제 메트릭 VO (Redis 데이터)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatchMetricsVO {
    private Integer groupId;
    private LocalDateTime collectedAt;
    private List<DeviceMetricsVO> devices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceMetricsVO {
        private Integer deviceId;
        private String deviceName;
        private String deviceIp;
        private CpuMetricsVO cpu;
        private MemMetricsVO mem;
        private IcmpMetricsVO icmp;
        private List<InterfaceMetricsVO> interfaces;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CpuMetricsVO {
        private Double usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemMetricsVO {
        private Double usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IcmpMetricsVO {
        private Double responseTime;
        private Boolean isReachable;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterfaceMetricsVO {
        private Integer ifIndex;
        private String ifName;
        private Long inBps;
        private Long outBps;
        private Double inUsed;
        private Double outUsed;
        private Long ifSpeed;
    }
}
