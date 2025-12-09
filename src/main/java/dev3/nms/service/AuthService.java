package dev3.nms.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev3.nms.config.SocialConfig;
import dev3.nms.exception.DuplicateEmailException;
import dev3.nms.exception.EmailAlreadyExistsException;
import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.mapper.SocialAccountMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.vo.auth.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final RestTemplate restTemplate;
    private final SocialConfig socialConfig;
    private final UserMapper userMapper;
    private final SocialAccountMapper socialAccountMapper;
    private final LoginHistoryMapper loginHistoryMapper;

    // 임시 세션 저장소 (실제로는 Redis 등 사용 권장)
    private final Map<String, UserVO> sessionStore = new HashMap<>();

    public AuthService(RestTemplate restTemplate,
                       SocialConfig socialConfig,
                       UserMapper userMapper,
                       SocialAccountMapper socialAccountMapper,
                       LoginHistoryMapper loginHistoryMapper) {
        this.restTemplate = restTemplate;
        this.socialConfig = socialConfig;
        this.userMapper = userMapper;
        this.socialAccountMapper = socialAccountMapper;
        this.loginHistoryMapper = loginHistoryMapper;
    }

    /**
     * 소셜 로그인 처리
     */
    public LoginResponseVO socialLogin(SocialLoginRequestVO request, String ipAddress, String userAgent) {
        log.info("[AuthService] 소셜 로그인 처리 시작 - SOCIAL_TYPE: {}", request.getSOCIAL_TYPE());

        // 1. 소셜 타입에 따라 사용자 정보 조회
        log.info("[Step 1] 소셜 API에서 사용자 정보 조회");
        UserVO userInfo = getUserInfoFromSocial(request.getSOCIAL_TYPE(), request.getACCESS_TOKEN());
        log.info("[Step 1 완료] 소셜 사용자 정보 - SOCIAL_ID: {}, EMAIL: {}, NAME: {}",
                userInfo.getSOCIAL_ID(), userInfo.getEMAIL(), userInfo.getNAME());

        // 2. DB에서 사용자 조회 또는 생성
        log.info("[Step 2] DB에서 사용자 조회 또는 생성");
        UserVO user = findOrCreateUser(userInfo);
        log.info("[Step 2 완료] USER_ID: {}", user.getUSER_ID());

        // 3. 소셜 계정 정보 업데이트
        log.info("[Step 3] 소셜 계정 정보 업데이트");
        updateSocialAccount(user, request, userInfo);
        log.info("[Step 3 완료]");

        // 4. 로그인 히스토리 저장
        log.info("[Step 4] 로그인 히스토리 저장");
        saveLoginHistory(user.getUSER_ID(), request.getSOCIAL_TYPE(), ipAddress, userAgent);
        log.info("[Step 4 완료]");

        // 5. 세션 생성
        log.info("[Step 5] 세션 생성");
        String sessionId = createSession(user);
        log.info("[Step 5 완료] SESSION_ID: {}", sessionId);

        // 6. 응답 생성
        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(user.getUSER_ID())
                .EMAIL(user.getEMAIL())
                .NAME(user.getNAME())
                .PROFILE_IMAGE(user.getPROFILE_IMAGE())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .SESSION_ID(sessionId)
                .build();

        log.info("[AuthService] 소셜 로그인 처리 완료");
        return response;
    }

    /**
     * 소셜 로그인 처리 - Authorization Code 방식
     */
    public LoginResponseVO socialLoginWithCode(SocialCodeRequestVO request, String ipAddress, String userAgent) {
        log.info("[AuthService] 소셜 로그인(Code) 처리 시작 - SOCIAL_TYPE: {}", request.getSOCIAL_TYPE());

        // 1. Code를 Access Token으로 교환
        log.info("[Step 1] Authorization Code -> Access Token 교환");
        String accessToken = exchangeCodeForToken(request.getSOCIAL_TYPE(), request.getCODE(), request.getREDIRECT_URI(), request.getSTATE());
        log.info("[Step 1 완료] Access Token 획득 성공");

        // 2. Access Token으로 사용자 정보 조회
        log.info("[Step 2] 소셜 API에서 사용자 정보 조회");
        UserVO userInfo = getUserInfoFromSocial(request.getSOCIAL_TYPE(), accessToken);
        log.info("[Step 2 완료] 소셜 사용자 정보 - SOCIAL_ID: {}, EMAIL: {}, NAME: {}",
                userInfo.getSOCIAL_ID(), userInfo.getEMAIL(), userInfo.getNAME());

        // 3. DB에서 사용자 조회 또는 생성
        log.info("[Step 3] DB에서 사용자 조회 또는 생성");
        UserVO user = findOrCreateUser(userInfo);
        log.info("[Step 3 완료] USER_ID: {}", user.getUSER_ID());

        // 4. 소셜 계정 정보 업데이트
        log.info("[Step 4] 소셜 계정 정보 업데이트");
        updateSocialAccountWithToken(user, request.getSOCIAL_TYPE(), userInfo, accessToken);
        log.info("[Step 4 완료]");

        // 5. 로그인 히스토리 저장
        log.info("[Step 5] 로그인 히스토리 저장");
        saveLoginHistory(user.getUSER_ID(), request.getSOCIAL_TYPE(), ipAddress, userAgent);
        log.info("[Step 5 완료]");

        // 6. 세션 생성
        log.info("[Step 6] 세션 생성");
        String sessionId = createSession(user);
        log.info("[Step 6 완료] SESSION_ID: {}", sessionId);

        // 7. 응답 생성
        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(user.getUSER_ID())
                .EMAIL(user.getEMAIL())
                .NAME(user.getNAME())
                .PROFILE_IMAGE(user.getPROFILE_IMAGE())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .SESSION_ID(sessionId)
                .build();

        log.info("[AuthService] 소셜 로그인(Code) 처리 완료");
        return response;
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

    /**
     * 카카오 Authorization Code를 Access Token으로 교환
     * API 문서: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#request-token
     */
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

            log.debug("[Kakao] Token Request - client_id: {}, redirect_uri: {}",
                    socialConfig.getKakaoJavascriptKey(), redirectUri);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("[Kakao] Token API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Kakao] Token API 응답 Body가 null입니다");
                throw new RuntimeException("Kakao Token API 응답이 없습니다");
            }
            log.debug("[Kakao] Token API 응답: {}", body);

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) {
                log.error("[Kakao] Access Token이 응답에 없습니다");
                throw new RuntimeException("Kakao Access Token을 받지 못했습니다");
            }

            log.info("[Kakao] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Kakao] Code -> Token 교환 실패", e);
            throw new RuntimeException("Kakao Code 교환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 구글 Authorization Code를 Access Token으로 교환
     * API 문서: https://developers.google.com/identity/protocols/oauth2/web-server#exchange-authorization-code
     */
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

            log.debug("[Google] Token Request - client_id: {}, redirect_uri: {}",
                    socialConfig.getGoogleClientId(), redirectUri);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("[Google] Token API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Google] Token API 응답 Body가 null입니다");
                throw new RuntimeException("Google Token API 응답이 없습니다");
            }
            log.debug("[Google] Token API 응답: {}", body);

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) {
                log.error("[Google] Access Token이 응답에 없습니다");
                throw new RuntimeException("Google Access Token을 받지 못했습니다");
            }

            log.info("[Google] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Google] Code -> Token 교환 실패", e);
            throw new RuntimeException("Google Code 교환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 네이버 Authorization Code를 Access Token으로 교환
     * API 문서: https://developers.naver.com/docs/login/api/api.md#4-2-3-%EC-A0-91%EA%B7%BC-%ED-86-A0%ED-81-B0-%EB-B0-9C%EA-B8-89-%EC-9A-94%EC-B2-AD
     */
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
            params.add("state", state); // CSRF 공격 방지를 위해 state 값을 전송해야 합니다.

            log.debug("[Naver] Token Request - client_id: {}", socialConfig.getNaverClientId());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("[Naver] Token API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Naver] Token API 응답 Body가 null입니다");
                throw new RuntimeException("Naver Token API 응답이 없습니다");
            }
            log.debug("[Naver] Token API 응답: {}", body);

            String accessToken = (String) body.get("access_token");
            if (accessToken == null) {
                log.error("[Naver] Access Token이 응답에 없습니다");
                throw new RuntimeException("Naver Access Token을 받지 못했습니다");
            }

            log.info("[Naver] Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("[Naver] Code -> Token 교환 실패", e);
            throw new RuntimeException("Naver Code 교환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 소셜 계정 정보 업데이트 (Token 포함)
     */
    private void updateSocialAccountWithToken(UserVO user, String socialType, UserVO socialUserInfo, String accessToken) {
        SocialAccountVO account = SocialAccountVO.builder()
                .USER_ID(user.getUSER_ID())
                .SOCIAL_TYPE(socialType)
                .SOCIAL_ID(socialUserInfo.getSOCIAL_ID())
                .SOCIAL_EMAIL(socialUserInfo.getEMAIL())
                .SOCIAL_NAME(socialUserInfo.getNAME())
                .ACCESS_TOKEN(accessToken)
                .build();

        // 존재 여부 확인 후 INSERT or UPDATE
        int count = socialAccountMapper.countByUserIdAndSocialType(user.getUSER_ID(), socialType);
        if (count > 0) {
            socialAccountMapper.update(account);
        } else {
            socialAccountMapper.insert(account);
        }
    }

    /**
     * 세션으로 사용자 정보 조회
     */
    public UserVO getUserBySession(String sessionId) {
        return sessionStore.get(sessionId);
    }

    /**
     * 로그아웃
     */
    public void logout(String sessionId) {
        sessionStore.remove(sessionId);
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

    /**
     * Kakao 사용자 정보 조회
     * API 문서: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
     */
    private UserVO getKakaoUserInfo(String accessToken) {
        log.info("[Kakao] 사용자 정보 조회 시작");
        String url = "https://kapi.kakao.com/v2/user/me";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            log.debug("[Kakao] Request URL: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            log.info("[Kakao] API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Kakao] API 응답 Body가 null입니다");
                throw new RuntimeException("Kakao API 응답이 없습니다");
            }
            log.debug("[Kakao] API 응답 Body: {}", body);

            String socialId = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String name = (String) profile.get("nickname");
            String profileImage = (String) profile.get("profile_image_url");

            log.info("[Kakao] 사용자 정보 조회 성공 - ID: {}, EMAIL: {}, NAME: {}", socialId, email, name);

            return UserVO.builder()
                    .SOCIAL_TYPE("KAKAO")
                    .SOCIAL_ID(socialId)
                    .EMAIL(email)
                    .NAME(name)
                    .PROFILE_IMAGE(profileImage)
                    .build();

        } catch (Exception e) {
            log.error("[Kakao] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Kakao 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Google 사용자 정보 조회
     * API 문서: https://developers.google.com/identity/protocols/oauth2/web-server#callinganapi
     */
    private UserVO getGoogleUserInfo(String accessToken) {
        log.info("[Google] 사용자 정보 조회 시작");
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            log.debug("[Google] Request URL: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            log.info("[Google] API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Google] API 응답 Body가 null입니다");
                throw new RuntimeException("Google API 응답이 없습니다");
            }
            log.debug("[Google] API 응답 Body: {}", body);

            String socialId = (String) body.get("id");
            String email = (String) body.get("email");
            String name = (String) body.get("name");
            String profileImage = (String) body.get("picture");

            log.info("[Google] 사용자 정보 조회 성공 - ID: {}, EMAIL: {}, NAME: {}", socialId, email, name);

            return UserVO.builder()
                    .SOCIAL_TYPE("GOOGLE")
                    .SOCIAL_ID(socialId)
                    .EMAIL(email)
                    .NAME(name)
                    .PROFILE_IMAGE(profileImage)
                    .build();

        } catch (Exception e) {
            log.error("[Google] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Google 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Naver 사용자 정보 조회
     * API 문서: https://developers.naver.com/docs/login/api/api.md
     */
    private UserVO getNaverUserInfo(String accessToken) {
        log.info("[Naver] 사용자 정보 조회 시작");
        String url = "https://openapi.naver.com/v1/nid/me";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            log.debug("[Naver] Request URL: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            log.info("[Naver] API 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.error("[Naver] API 응답 Body가 null입니다");
                throw new RuntimeException("Naver API 응답이 없습니다");
            }
            log.debug("[Naver] API 응답 Body: {}", body);

            Map<String, Object> responseNode = (Map<String, Object>) body.get("response");
            String socialId = (String) responseNode.get("id");
            String email = (String) responseNode.get("email");
            String name = (String) responseNode.get("name");
            String profileImage = (String) responseNode.get("profile_image");

            log.info("[Naver] 사용자 정보 조회 성공 - ID: {}, EMAIL: {}, NAME: {}", socialId, email, name);

            return UserVO.builder()
                    .SOCIAL_TYPE("NAVER")
                    .SOCIAL_ID(socialId)
                    .EMAIL(email)
                    .NAME(name)
                    .PROFILE_IMAGE(profileImage)
                    .build();

        } catch (Exception e) {
            log.error("[Naver] 사용자 정보 조회 실패", e);
            throw new RuntimeException("Naver 사용자 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 조회 또는 생성
     * 중복 이메일 체크 강화
     */
    private UserVO findOrCreateUser(UserVO socialUserInfo) {
        // 1. 소셜 타입과 소셜 ID로 사용자 조회
        Optional<UserVO> userOptional = userMapper.findBySocialTypeAndSocialId(
                socialUserInfo.getSOCIAL_TYPE(),
                socialUserInfo.getSOCIAL_ID()
        );

        if (userOptional.isPresent()) {
            // 기존 소셜 계정으로 로그인한 경우, 사용자 정보 최신화
            UserVO user = userOptional.get();
            log.info("기존 소셜 계정으로 로그인합니다. 사용자 정보를 최신화합니다. USER_ID: {}", user.getUSER_ID());
            user.setNAME(socialUserInfo.getNAME());
            user.setPROFILE_IMAGE(socialUserInfo.getPROFILE_IMAGE());
            // EMAIL은 기존 것을 유지하고, NAME과 PROFILE_IMAGE만 업데이트
            userMapper.update(user);
            return user;
        }

        // 2. 이메일로 사용자 조회 (중복 이메일 체크)
        Optional<UserVO> userByEmailOptional = userMapper.findByEmail(socialUserInfo.getEMAIL());

        if (userByEmailOptional.isPresent()) {
            // 이메일이 존재하지만 다른 소셜 계정으로 가입한 경우
            UserVO existingUser = userByEmailOptional.get();

            // 중복 이메일 예외 발생: 다른 소셜 타입으로 이미 가입됨
            log.error("중복 이메일 발견 - 이메일: {}, 기존 소셜 타입: {}, 시도한 소셜 타입: {}",
                    socialUserInfo.getEMAIL(), existingUser.getSOCIAL_TYPE(), socialUserInfo.getSOCIAL_TYPE());

            throw new DuplicateEmailException(
                    socialUserInfo.getEMAIL(),
                    existingUser.getSOCIAL_TYPE(),
                    socialUserInfo.getSOCIAL_TYPE()
            );
        }

        // 3. 신규 사용자 생성
        try {
            log.info("신규 사용자입니다. DB에 사용자를 생성합니다. 이메일: {}, 소셜 타입: {}",
                    socialUserInfo.getEMAIL(), socialUserInfo.getSOCIAL_TYPE());
            userMapper.insert(socialUserInfo);
            log.info("신규 사용자 생성 완료. USER_ID: {}", socialUserInfo.getUSER_ID());
            return socialUserInfo;
        } catch (Exception e) {
            // 데이터베이스 제약 조건 위반 등 예외 처리
            log.error("사용자 생성 실패", e);
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다: " + socialUserInfo.getEMAIL());
        }
    }

    /**
     * 소셜 계정 정보 업데이트
     */
    private void updateSocialAccount(UserVO user, SocialLoginRequestVO request, UserVO socialUserInfo) {
        SocialAccountVO account = SocialAccountVO.builder()
                .USER_ID(user.getUSER_ID())
                .SOCIAL_TYPE(request.getSOCIAL_TYPE())
                .SOCIAL_ID(socialUserInfo.getSOCIAL_ID())
                .SOCIAL_EMAIL(socialUserInfo.getEMAIL())
                .SOCIAL_NAME(socialUserInfo.getNAME())
                .ACCESS_TOKEN(request.getACCESS_TOKEN())
                .build();

        // 존재 여부 확인 후 INSERT or UPDATE
        int count = socialAccountMapper.countByUserIdAndSocialType(user.getUSER_ID(), request.getSOCIAL_TYPE());
        if (count > 0) {
            socialAccountMapper.update(account);
        } else {
            socialAccountMapper.insert(account);
        }
    }

    /**
     * 로그인 히스토리 저장
     */
    private void saveLoginHistory(Long userId, String loginType, String ipAddress, String userAgent) {
        LoginHistoryVO history = LoginHistoryVO.builder()
                .USER_ID(userId)
                .LOGIN_TYPE(loginType)
                .IP_ADDRESS(ipAddress)
                .USER_AGENT(userAgent)
                .build();

        loginHistoryMapper.insert(history);
    }

    /**
     * 세션 생성
     */
    private String createSession(UserVO user) {
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, user);
        return sessionId;
    }
}
