package dev3.nms.service;

import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.util.PasswordValidator;
import dev3.nms.vo.auth.LoginHistoryVO;
import dev3.nms.vo.auth.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계정 정보 조회 (프로필 + 최근 로그인 이력)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAccountInfo(Long userId) {
        UserVO user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 비밀번호 제거
        user.setPASSWORD(null);

        List<LoginHistoryVO> loginHistory = loginHistoryMapper.findByUserId(userId, 10);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("loginHistory", loginHistory);
        return result;
    }

    /**
     * 프로필 수정 (이름, 이메일, 전화번호)
     */
    @Transactional
    public void updateProfile(Long userId, String name, String email, String phone) {
        userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이름 필수
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        // 이메일 중복 검사 (본인 제외)
        if (email != null && !email.trim().isEmpty()) {
            Optional<UserVO> existing = userMapper.findByEmail(email);
            if (existing.isPresent() && !existing.get().getUSER_ID().equals(userId)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
        }

        // 전화번호 중복 검사 (본인 제외)
        if (phone != null && !phone.trim().isEmpty()) {
            Optional<UserVO> existing = userMapper.findByPhone(phone);
            if (existing.isPresent() && !existing.get().getUSER_ID().equals(userId)) {
                throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");
            }
        }

        userMapper.updateProfile(userId, name.trim(), email, phone);
        log.info("[Account] 프로필 수정 완료 - USER_ID: {}", userId);
    }

    /**
     * 프로필 이미지 업데이트
     */
    @Transactional
    public void updateProfileImage(Long userId, String imageUrl) {
        UserVO user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPROFILE_IMAGE(imageUrl);
        userMapper.update(user);
        log.info("[Account] 프로필 이미지 변경 - USER_ID: {}", userId);
    }

    /**
     * 비밀번호 변경 (LOCAL 유저만)
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserVO user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // LOCAL 유저만 비밀번호 변경 가능
        if (!"LOCAL".equals(user.getSOCIAL_TYPE())) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPASSWORD())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호 정책 검증
        List<String> pwErrors = PasswordValidator.validate(newPassword);
        if (!pwErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", pwErrors));
        }

        // 현재 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(newPassword, user.getPASSWORD())) {
            throw new IllegalArgumentException("현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encodedPassword);
        log.info("[Account] 비밀번호 변경 완료 - USER_ID: {}", userId);
    }
}
