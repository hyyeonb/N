package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryVO {
    private Long HISTORY_ID;
    private Long USER_ID;
    private String LOGIN_TYPE;
    private String IP_ADDRESS;
    private String USER_AGENT;
    private LocalDateTime LOGIN_AT;
    private LocalDateTime LOGOUT_AT;

    // JOIN/서브쿼리 필드
    private String USER_NAME;
    private LocalDateTime LAST_ACTIVITY_AT;

    // 세션별 활동 CRUD 횟수
    private Integer ACTIVITY_CREATE;
    private Integer ACTIVITY_UPDATE;
    private Integer ACTIVITY_DELETE;
    private Integer ACTIVITY_VIEW;
    private Integer ACTIVITY_CONTROL;
}
