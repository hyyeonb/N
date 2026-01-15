package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long USER_ID;
    private String LOGIN_ID;      // 로컬 로그인용 아이디
    private String PASSWORD;      // 로컬 로그인용 비밀번호 (BCrypt)
    private String EMAIL;
    private String NAME;
    private String PHONE;         // 전화번호
    private String PROFILE_IMAGE;
    private String SOCIAL_TYPE;   // KAKAO, GOOGLE, NAVER, LOCAL
    private String SOCIAL_ID;
    private LocalDateTime CREATED_AT;
    private LocalDateTime UPDATED_AT;
}
