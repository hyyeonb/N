package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialCodeRequestVO {
    private String SOCIAL_TYPE;  // KAKAO, GOOGLE, NAVER
    private String CODE;
    private String REDIRECT_URI;
    private String STATE;
}