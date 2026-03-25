package dev3.nms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 비밀번호 정책 검증
 * - 최소 8자, 최대 64자
 * - 영문, 숫자, 특수문자 중 3종 이상 포함
 * - 연속 동일 문자 3회 이상 금지
 * - 공백 금지
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]");
    private static final Pattern HAS_WHITESPACE = Pattern.compile("\\s");
    private static final Pattern CONSECUTIVE_SAME = Pattern.compile("(.)\\1{2,}");

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("비밀번호를 입력해주세요.");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("비밀번호는 최소 " + MIN_LENGTH + "자 이상이어야 합니다.");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("비밀번호는 최대 " + MAX_LENGTH + "자까지 가능합니다.");
        }

        if (HAS_WHITESPACE.matcher(password).find()) {
            errors.add("비밀번호에 공백을 포함할 수 없습니다.");
        }

        // 3종 이상 조합 체크
        int typeCount = 0;
        if (HAS_UPPER.matcher(password).find()) typeCount++;
        if (HAS_LOWER.matcher(password).find()) typeCount++;
        if (HAS_DIGIT.matcher(password).find()) typeCount++;
        if (HAS_SPECIAL.matcher(password).find()) typeCount++;

        if (typeCount < 3) {
            errors.add("영문 대문자, 소문자, 숫자, 특수문자 중 3종 이상 포함해야 합니다.");
        }

        // 연속 동일 문자 체크
        if (CONSECUTIVE_SAME.matcher(password).find()) {
            errors.add("동일한 문자를 3회 이상 연속 사용할 수 없습니다.");
        }

        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
