package dev3.nms.vo.watch;

import lombok.Data;
import java.util.List;

/**
 * 관제 그룹 장비 VO
 * 테이블: R_WATCH_GROUP_DEVICE_T
 */
@Data
public class WatchGroupDeviceVO {
    private Integer WATCH_GROUP_ID;
    private Integer DEVICE_ID;

    // 조인 데이터
    private String DEVICE_NAME;
    private String DEVICE_IP;

    // 인터페이스 목록
    private List<WatchGroupIfVO> interfaces;
}
