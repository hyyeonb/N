package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogVO {
    private Long LOG_ID;
    private Long HISTORY_ID;
    private Long USER_ID;
    private String ACTION_TYPE;       // PAGE_VIEW, VIEW, CREATE, UPDATE, DELETE, CONTROL
    private String TARGET_TYPE;       // DEVICE, GROUP, MODEL, NOTICE 등
    private String TARGET_ID;         // 대상 ID (다형성)
    private String TARGET_NAME;       // 대상 이름 (비정규화)
    private String PAGE_CODE;
    private String REQUEST_METHOD;
    private String REQUEST_URI;
    private String IP_ADDRESS;
    private String DETAIL;            // JSON (변경 상세)
    private LocalDateTime CREATED_AT;

    // JOIN용 필드
    private String USER_NAME;
}
