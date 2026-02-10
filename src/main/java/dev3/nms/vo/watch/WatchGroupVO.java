package dev3.nms.vo.watch;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관제 그룹 VO
 * 테이블: R_WATCH_GROUP_T
 */
@Data
public class WatchGroupVO {
    private Integer WATCH_GROUP_ID;
    private Integer PARENT_GROUP_ID;  // 부모 그룹 ID (계층 구조)
    private String GROUP_NAME;
    private Integer INTERVAL_SEC;
    private Integer DEPTH;  // 깊이 (0 = 최상위)
    private String ICON_NAME;  // 아이콘 이름
    private LocalDateTime CREATE_AT;
    private LocalDateTime MODIFY_AT;

    // 조인 데이터 (상세 조회 시)
    private List<WatchGroupDeviceVO> devices;
}
