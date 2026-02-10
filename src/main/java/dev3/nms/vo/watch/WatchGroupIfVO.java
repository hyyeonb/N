package dev3.nms.vo.watch;

import lombok.Data;

/**
 * 관제 그룹 인터페이스 VO
 * 테이블: R_WATCH_GROUP_IF_T
 */
@Data
public class WatchGroupIfVO {
    private Integer WATCH_GROUP_ID;
    private Integer DEVICE_ID;
    private Integer IF_INDEX;

    // 조인 데이터
    private String IF_DESCR;
    private String IF_NAME;
    private Long IF_SPEED;
}
