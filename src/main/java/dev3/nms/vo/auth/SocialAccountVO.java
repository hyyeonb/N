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
public class SocialAccountVO {
    private Long SOCIAL_ACCOUNT_ID;
    private Long USER_ID;
    private String SOCIAL_TYPE;
    private String SOCIAL_ID;
    private String SOCIAL_EMAIL;
    private String SOCIAL_NAME;
    private String ACCESS_TOKEN;
    private String REFRESH_TOKEN;
    private LocalDateTime TOKEN_EXPIRES_AT;
    private LocalDateTime CREATED_AT;
    private LocalDateTime UPDATED_AT;
}
