package dev3.nms.exception;

import lombok.Getter;

/**
 * 중복 이메일 예외
 * 소셜 로그인 시 이미 등록된 이메일이 있을 경우 발생
 */
@Getter
public class DuplicateEmailException extends RuntimeException {

    private final String email;
    private final String existingSocialType;
    private final String attemptedSocialType;

    public DuplicateEmailException(String email, String existingSocialType, String attemptedSocialType) {
        super(String.format("이메일 '%s'은(는) 이미 %s 계정으로 등록되어 있습니다. %s 계정으로는 로그인할 수 없습니다.",
                email, existingSocialType, attemptedSocialType));
        this.email = email;
        this.existingSocialType = existingSocialType;
        this.attemptedSocialType = attemptedSocialType;
    }

}
