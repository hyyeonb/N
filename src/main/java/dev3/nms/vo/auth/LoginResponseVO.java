package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseVO {
    private Long USER_ID;
    private String EMAIL;
    private String NAME;
    private String PROFILE_IMAGE;
    private String SOCIAL_TYPE;
    private String SESSION_ID;
}
