package dev3.nms.vo.mgmt;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareVO {
    private Integer MIDDLEWARE_ID;
    private String MIDDLEWARE_NAME;
    private String MIDDLEWARE_URL;
    private String API_KEY;
    private String STATUS;  // ACTIVE, DOWN, MAINTENANCE
    private LocalDateTime LAST_HEARTBEAT;
    private Integer PRIORITY;
    private String DESCRIPTION;
    private LocalDateTime CREATE_AT;
    private LocalDateTime MODIFY_AT;
    // 할당 장비 수 (조회용)
    private Integer DEVICE_COUNT;
}
