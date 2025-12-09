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
}
