package dev3.nms.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestVO {
    private String LOGIN_ID;
    private String PASSWORD;
    private String EMAIL;
    private String NAME;
    private String PHONE;
}
