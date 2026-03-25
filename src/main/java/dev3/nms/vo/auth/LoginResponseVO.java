package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long USER_ID;
    private Long HISTORY_ID;
    private String EMAIL;
    private String NAME;
    private String PROFILE_IMAGE;
    private String SOCIAL_TYPE;
    private UserPermissionVO permissions;
}
