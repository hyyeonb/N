package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestVO {
    private String SOCIAL_TYPE;  // KAKAO, GOOGLE, NAVER
    private String ACCESS_TOKEN;
}
