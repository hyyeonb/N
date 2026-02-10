package dev3.nms.vo.watch;

import lombok.Data;
import java.util.List;

/**
 * 관제 시작/중지 요청 VO
 */
@Data
public class WatchRequestVO {

    /**
     * 관제 시작 요청
     */
    @Data
    public static class StartRequest {
        private Integer groupId;
        private Integer intervalSec;
        private List<DeviceRequest> devices;
    }

    /**
     * 장비별 요청
     */
    @Data
    public static class DeviceRequest {
        private Integer deviceId;
        private List<Integer> ifIndexes;
    }

    /**
     * 관제 중지/Heartbeat 요청
     */
    @Data
    public static class GroupRequest {
        private Integer groupId;
    }
}
