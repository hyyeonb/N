package dev3.nms.service;

import dev3.nms.config.SocialConfig;
import dev3.nms.exception.DuplicateEmailException;
import dev3.nms.exception.EmailAlreadyExistsException;
import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.mapper.SocialAccountMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.util.PasswordValidator;
import dev3.nms.vo.auth.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final RestTemplate restTemplate;
    private final SocialConfig socialConfig;
    private final UserMapper userMapper;
    private final SocialAccountMapper socialAccountMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(RestTemplate restTemplate,
                       SocialConfig socialConfig,
                       UserMapper userMapper,
                       SocialAccountMapper socialAccountMapper,
                       LoginHistoryMapper loginHistoryMapper,
                       PasswordEncoder passwordEncoder,
                       PermissionService permissionService,
                       LoginAttemptService loginAttemptService) {
        this.restTemplate = restTemplate;
        this.socialConfig = socialConfig;
        this.userMapper = userMapper;
        this.socialAccountMapper = socialAccountMapper;
        this.loginHistoryMapper = loginHistoryMapper;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * 소셜 로그인 처리
     */
    public LoginResponseVO socialLogin(SocialLoginRequestVO request, String ipAddress, String userAgent) {
        log.info("[AuthService] 소셜 로그인 처리 시작 - SOCIAL_TYPE: {}", request.getSOCIAL_TYPE());

        UserVO userInfo = getUserInfoFromSocial(request.getSOCIAL_TYPE(), request.getACCESS_TOKEN());
        UserVO user = findOrCreateUser(userInfo);
        updateSocialAccount(user, request, userInfo);
        Long historyId = saveLoginHistory(user.getUSER_ID(), request.getSOCIAL_TYPE(), ipAddress, userAgent);

        // 권한 조회
        UserPermissionVO permissions = buildPermissions(user);

        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(user.getUSER_ID())
                .HISTORY_ID(historyId)
                .EMAIL(user.getEMAIL())
                .NAME(user.getNAME())
                .PROFILE_IMAGE(user.getPROFILE_IMAGE())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .permissions(permissions)
                .build();

        log.info("[AuthService] 소셜 로그인 처리 완료");
        return response;
    }

    /**
     * 소셜 로그인 처리 - Authorization Code 방식
     */
    public LoginResponseVO socialLoginWithCode(SocialCodeRequestVO request, String ipAddress, String userAgent) {
        log.info("[AuthService] 소셜 로그인(Code) 처리 시작 - SOCIAL_TYPE: {}", request.getSOCIAL_TYPE());

        String accessToken = exchangeCodeForToken(request.getSOCIAL_TYPE(), request.getCODE(), request.getREDIRECT_URI(), request.getSTATE());
        UserVO userInfo = getUserInfoFromSocial(request.getSOCIAL_TYPE(), accessToken);
        UserVO user = findOrCreateUser(userInfo);
        updateSocialAccountWithToken(user, request.getSOCIAL_TYPE(), userInfo, accessToken);
        Long historyId = saveLoginHistory(user.getUSER_ID(), request.getSOCIAL_TYPE(), ipAddress, userAgent);

        // 권한 조회
        UserPermissionVO permissions = buildPermissions(user);

        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(user.getUSER_ID())
                .HISTORY_ID(historyId)
                .EMAIL(user.getEMAIL())
                .NAME(user.getNAME())
                .PROFILE_IMAGE(user.getPROFILE_IMAGE())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .permissions(permissions)
                .build();

        log.info("[AuthService] 소셜 로그인(Code) 처리 완료");
        return response;
    }

    /**
     * 사용자 ID로 사용자 조회 (HttpSession용)
     */
    public UserVO getUserById(Long userId) {
        return userMapper.findById(userId).orElse(null);
    }

    /**
     * 사용자 권한 정보 구성
     */
    public UserPermissionVO buildPermissions(UserVO user) {
        UserPermissionVO permissions = permissionService.getUserPermissions(user.getUSER_ID());
        permissions.setAllGroupView(Boolean.TRUE.equals(user.getALL_GROUP_VIEW()));
        return permissions;
    }

    /**
     * Authorization Code를 Access Token으로 교환
     */
    private String exchangeCodeForToken(String socialType, String code, String redirectUri, String state) {
        switch (socialType.toUpperCase()) {
            case "KAKAO":
                return exchangeKakaoCodeForToken(code, redirectUri);
            case "GOOGLE":
                return exchangeGoogleCodeForToken(code, redirectUri);
            case "NAVER":
                return exchangeNaverCodeForToken(code, state);
            default:
                throw new IllegalArgumentException("Code 방식을 지원하지 않는 소셜 타입입니다: " + socialType);
        }
    }

    private String exchangeKakaoCodeForToken(String code, String redirectUri) {
        log.info("[Kakao] Code -> Token 교환 시작");
        String url = "https://kauth.kakao.com/oauth/token";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", socialConfig.getKakaoJavascriptKey());
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Kakao Token API 응답이 없습니다");

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) throw new RuntimeException("Kakao Access Token을 받지 못했습니다");

            log.info("[Kakao] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Kakao] Code -> Token 교환 실패", e);
            throw new RuntimeException("Kakao Code 교환 실패: " + e.getMessage(), e);
        }
    }

    private String exchangeGoogleCodeForToken(String code, String redirectUri) {
        log.info("[Google] Code -> Token 교환 시작");
        String url = "https://oauth2.googleapis.com/token";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", socialConfig.getGoogleClientId());
            params.add("client_secret", socialConfig.getGoogleClientSecret());
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Google Token API 응답이 없습니다");

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) throw new RuntimeException("Google Access Token을 받지 못했습니다");

            log.info("[Google] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Google] Code -> Token 교환 실패", e);
            throw new RuntimeException("Google Code 교환 실패: " + e.getMessage(), e);
        }
    }

    private String exchangeNaverCodeForToken(String code, String state) {
        log.info("[Naver] Code -> Token 교환 시작");
        String url = "https://nid.naver.com/oauth2.0/token";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", socialConfig.getNaverClientId());
            params.add("client_secret", socialConfig.getNaverClientSecret());
            params.add("code", code);
            params.add("state", state);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Naver Token API 응답이 없습니다");

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) throw new RuntimeException("Naver Access Token을 받지 못했습니다");

            log.info("[Naver] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Naver] Code -> Token 교환 실패", e);
            throw new RuntimeException("Naver Code 교환 실패: " + e.getMessage(), e);
        }
    }

    private void updateSocialAccountWithToken(UserVO user, String socialType, UserVO socialUserInfo, String accessToken) {
        SocialAccountVO account = SocialAccountVO.builder()
                .USER_ID(user.getUSER_ID())
                .SOCIAL_TYPE(socialType)
                .SOCIAL_ID(socialUserInfo.getSOCIAL_ID())
                .SOCIAL_EMAIL(socialUserInfo.getEMAIL())
                .SOCIAL_NAME(socialUserInfo.getNAME())
                .ACCESS_TOKEN(accessToken)
                .build();

        int count = socialAccountMapper.countByUserIdAndSocialType(user.getUSER_ID(), socialType);
        if (count > 0) {
            socialAccountMapper.update(account);
        } else {
            socialAccountMapper.insert(account);
        }
    }

    /**
     * 소셜 API로부터 사용자 정보 조회
     */
    private UserVO getUserInfoFromSocial(String socialType, String accessToken) {
        switch (socialType.toUpperCase()) {
            case "KAKAO":
                return getKakaoUserInfo(accessToken);
            case "GOOGLE":
                return getGoogleUserInfo(accessToken);
            case "NAVER":
                return getNaverUserInfo(accessToken);
            default:
                throw new IllegalArgumentException("지원하지 않는 소셜 타입입니다: " + socialType);
        }
    }

    private UserVO getKakaoUserInfo(String accessToken) {
        log.info("[Kakao] 사용자 정보 조회 시작");
        String url = "https://kapi.kakao.com/v2/user/me";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Kakao API 응답이 없습니다");

            String socialId = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            return UserVO.builder()
                    .SOCIAL_TYPE("KAKAO")
                    .SOCIAL_ID(socialId)
                    .EMAIL((String) kakaoAccount.get("email"))
                    .NAME((String) profile.get("nickname"))
                    .PROFILE_IMAGE((String) profile.get("profile_image_url"))
                    .build();
        } catch (Exception e) {
            log.error("[Kakao] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Kakao 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    private UserVO getGoogleUserInfo(String accessToken) {
        log.info("[Google] 사용자 정보 조회 시작");
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Google API 응답이 없습니다");

            return UserVO.builder()
                    .SOCIAL_TYPE("GOOGLE")
                    .SOCIAL_ID((String) body.get("id"))
                    .EMAIL((String) body.get("email"))
                    .NAME((String) body.get("name"))
                    .PROFILE_IMAGE((String) body.get("picture"))
                    .build();
        } catch (Exception e) {
            log.error("[Google] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Google 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    private UserVO getNaverUserInfo(String accessToken) {
        log.info("[Naver] 사용자 정보 조회 시작");
        String url = "https://openapi.naver.com/v1/nid/me";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Naver API 응답이 없습니다");

            Map<String, Object> responseNode = (Map<String, Object>) body.get("response");

            return UserVO.builder()
                    .SOCIAL_TYPE("NAVER")
                    .SOCIAL_ID((String) responseNode.get("id"))
                    .EMAIL((String) responseNode.get("email"))
                    .NAME((String) responseNode.get("name"))
                    .PROFILE_IMAGE((String) responseNode.get("profile_image"))
                    .build();
        } catch (Exception e) {
            log.error("[Naver] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Naver 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 조회 또는 생성
     */
    private UserVO findOrCreateUser(UserVO socialUserInfo) {
        Optional<UserVO> userOptional = userMapper.findBySocialTypeAndSocialId(
                socialUserInfo.getSOCIAL_TYPE(),
                socialUserInfo.getSOCIAL_ID()
        );

        if (userOptional.isPresent()) {
            UserVO user = userOptional.get();
            log.info("기존 소셜 계정으로 로그인합니다. USER_ID: {}", user.getUSER_ID());
            user.setNAME(socialUserInfo.getNAME());
            user.setPROFILE_IMAGE(socialUserInfo.getPROFILE_IMAGE());
            userMapper.update(user);
            return user;
        }

        Optional<UserVO> userByEmailOptional = userMapper.findByEmail(socialUserInfo.getEMAIL());

        if (userByEmailOptional.isPresent()) {
            UserVO existingUser = userByEmailOptional.get();
            throw new DuplicateEmailException(
                    socialUserInfo.getEMAIL(),
                    existingUser.getSOCIAL_TYPE(),
                    socialUserInfo.getSOCIAL_TYPE()
            );
        }

        try {
            log.info("신규 사용자입니다. DB에 사용자를 생성합니다. 이메일: {}", socialUserInfo.getEMAIL());
            userMapper.insert(socialUserInfo);
            log.info("신규 사용자 생성 완료. USER_ID: {}", socialUserInfo.getUSER_ID());

            // 신규 사용자 기본 권한 초기화
            permissionService.initializeDefaultPermissions(socialUserInfo.getUSER_ID());

            return socialUserInfo;
        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다: " + socialUserInfo.getEMAIL());
        }
    }

    private void updateSocialAccount(UserVO user, SocialLoginRequestVO request, UserVO socialUserInfo) {
        SocialAccountVO account = SocialAccountVO.builder()
                .USER_ID(user.getUSER_ID())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .SOCIAL_ID(socialUserInfo.getSOCIAL_ID())
                .SOCIAL_EMAIL(socialUserInfo.getEMAIL())
                .SOCIAL_NAME(socialUserInfo.getNAME())
                .ACCESS_TOKEN(request.getACCESS_TOKEN())
                .build();

        int count = socialAccountMapper.countByUserIdAndSocialType(user.getUSER_ID(), request.getSOCIAL_TYPE());
        if (count > 0) {
            socialAccountMapper.update(account);
        } else {
            socialAccountMapper.insert(account);
        }
    }

    private Long saveLoginHistory(Long userId, String loginType, String ipAddress, String userAgent) {
        return createLoginHistory(userId, loginType, ipAddress, userAgent);
    }

    /**
     * 로그인 이력 생성 (세션 복원 시에도 사용)
     */
    public Long createLoginHistory(Long userId, String loginType, String ipAddress, String userAgent) {
        LoginHistoryVO history = LoginHistoryVO.builder()
                .USER_ID(userId)
                .LOGIN_TYPE(loginType)
                .IP_ADDRESS(ipAddress)
                .USER_AGENT(userAgent)
                .build();

        loginHistoryMapper.insert(history);
        return history.getHISTORY_ID();
    }

    // ==================== 로컬 회원가입/로그인 ====================

    public UserVO signup(UserVO user, String ipAddress, String userAgent) {
        log.info("[AuthService] 로컬 회원가입 시작 - LOGIN_ID: {}, EMAIL: {}", user.getLOGIN_ID(), user.getEMAIL());

        // 비밀번호 정책 검증
        List<String> pwErrors = PasswordValidator.validate(user.getPASSWORD());
        if (!pwErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", pwErrors));
        }

        if (user.getEMAIL() != null && !user.getEMAIL().isEmpty()) {
            Optional<UserVO> existingUser = userMapper.findByEmail(user.getEMAIL());
            if (existingUser.isPresent()) {
                UserVO existing = existingUser.get();
                throw new DuplicateEmailException(user.getEMAIL(), existing.getSOCIAL_TYPE(), "LOCAL");
            }
        }

        if (user.getPHONE() != null && !user.getPHONE().isEmpty()) {
            Optional<UserVO> existingUser = userMapper.findByPhone(user.getPHONE());
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");
            }
        }

        String encodedPassword = passwordEncoder.encode(user.getPASSWORD());
        user.setPASSWORD(encodedPassword);

        userMapper.insertLocal(user);
        log.info("[AuthService] 회원가입 완료 - USER_ID: {}", user.getUSER_ID());

        // 신규 사용자 기본 권한 초기화
        permissionService.initializeDefaultPermissions(user.getUSER_ID());

        saveLoginHistory(user.getUSER_ID(), "LOCAL", ipAddress, userAgent);
        return user;
    }

    public LoginResponseVO localLogin(String loginId, String password, String ipAddress, String userAgent) {
        log.info("[AuthService] 로컬 로그인 시작 - LOGIN_ID: {}", loginId);

        // Brute Force 방지: 잠금 상태 확인
        if (loginAttemptService.isBlocked(ipAddress, loginId)) {
            log.warn("[보안] 로그인 차단됨 - IP: {}, LOGIN_ID: {}", ipAddress, loginId);
            throw new IllegalArgumentException("로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해주세요.");
        }

        Optional<UserVO> userOptional = userMapper.findByLoginId(loginId);
        if (userOptional.isEmpty()) {
            loginAttemptService.loginFailed(ipAddress, loginId);
            int remaining = loginAttemptService.getRemainingAttempts(ipAddress, loginId);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다." +
                    (remaining <= 2 ? " (남은 시도: " + remaining + "회)" : ""));
        }

        UserVO user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPASSWORD())) {
            loginAttemptService.loginFailed(ipAddress, loginId);
            int remaining = loginAttemptService.getRemainingAttempts(ipAddress, loginId);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다." +
                    (remaining <= 2 ? " (남은 시도: " + remaining + "회)" : ""));
        }

        // 계정 상태 확인
        if ("SUSPENDED".equals(user.getSTATUS())) {
            throw new IllegalArgumentException("정지된 계정입니다. 관리자에게 문의하세요.");
        }

        // 로그인 성공 - 실패 카운트 초기화
        loginAttemptService.loginSucceeded(ipAddress, loginId);

        Long historyId = saveLoginHistory(user.getUSER_ID(), "LOCAL", ipAddress, userAgent);

        UserPermissionVO permissions = buildPermissions(user);

        log.info("[AuthService] 로컬 로그인 성공 - USER_ID: {}", user.getUSER_ID());

        return LoginResponseVO.builder()
                .USER_ID(user.getUSER_ID())
                .HISTORY_ID(historyId)
                .EMAIL(user.getEMAIL())
                .NAME(user.getNAME())
                .PROFILE_IMAGE(user.getPROFILE_IMAGE())
                .SOCIAL_TYPE("LOCAL")
                .permissions(permissions)
                .build();
    }

    public boolean isLoginIdAvailable(String loginId) {
        return userMapper.findByLoginId(loginId).isEmpty();
    }

    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) return true;
        return userMapper.findByEmail(email).isEmpty();
    }

    public boolean isPhoneAvailable(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        return userMapper.findByPhone(phone).isEmpty();
    }

    public String findLoginId(String name, String phone) {
        Optional<UserVO> userOptional = userMapper.findByNameAndPhone(name, phone);
        if (userOptional.isEmpty()) return null;
        return userOptional.get().getLOGIN_ID();
    }

    public boolean resetPassword(String loginId, String name, String phone, String newPassword) {
        // 비밀번호 정책 검증
        List<String> pwErrors = PasswordValidator.validate(newPassword);
        if (!pwErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", pwErrors));
        }

        Optional<UserVO> userOptional = userMapper.findByLoginIdAndNameAndPhone(loginId, name, phone);
        if (userOptional.isEmpty()) return false;

        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userOptional.get().getUSER_ID(), encodedPassword);
        return true;
    }
}
