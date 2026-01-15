package dev3.nms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.config.LoginCheckInterceptor;
import dev3.nms.mapper.*;
import dev3.nms.service.AuthService;
import dev3.nms.vo.auth.LoginResponseVO;
import dev3.nms.vo.auth.SocialCodeRequestVO;
import dev3.nms.vo.auth.SocialLoginRequestVO;
import dev3.nms.vo.auth.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LoginCheckInterceptor loginCheckInterceptor;

    // Mapper Mocks (MyBatis)
    @MockitoBean
    private CommonOidMapper commonOidMapper;

    @MockitoBean
    private DeviceMapper deviceMapper;

    @MockitoBean
    private GroupMapper groupMapper;

    @MockitoBean
    private LoginHistoryMapper loginHistoryMapper;

    @MockitoBean
    private ModelMapper modelMapper;

    @MockitoBean
    private PortMapper portMapper;

    @MockitoBean
    private SnmpMetricMapper snmpMetricMapper;

    @MockitoBean
    private SnmpModelOidMapper snmpModelOidMapper;

    @MockitoBean
    private SocialAccountMapper socialAccountMapper;

    @MockitoBean
    private TempDeviceMapper tempDeviceMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private VendorMapper vendorMapper;

    @BeforeEach
    void setUp() throws Exception {
        // LoginCheckInterceptor가 모든 요청을 통과하도록 설정
        when(loginCheckInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ==================== 소셜 로그인 테스트 ====================

    @Test
    @DisplayName("소셜 로그인 - 성공")
    void socialLogin_Success() throws Exception {
        SocialLoginRequestVO request = new SocialLoginRequestVO("KAKAO", "valid_access_token");

        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(1L)
                .EMAIL("test@example.com")
                .SESSION_ID("session_123")
                .build();

        when(authService.socialLogin(any(SocialLoginRequestVO.class), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.EMAIL").value("test@example.com"));

        verify(authService, times(1)).socialLogin(any(SocialLoginRequestVO.class), anyString(), any());
    }

    @Test
    @DisplayName("소셜 로그인 (Code) - 성공")
    void socialLoginWithCode_Success() throws Exception {
        SocialCodeRequestVO request = new SocialCodeRequestVO("KAKAO", "valid_auth_code", "http://localhost:3000/callback", null);

        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(1L)
                .EMAIL("test@example.com")
                .SESSION_ID("session_456")
                .build();

        when(authService.socialLoginWithCode(any(SocialCodeRequestVO.class), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/social/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.SESSION_ID").value("session_456"));
    }

    // ==================== 로컬 로그인/회원가입 테스트 ====================

    @Test
    @DisplayName("로컬 회원가입 - 성공")
    void signup_Success() throws Exception {
        UserVO user = UserVO.builder()
                .LOGIN_ID("testuser")
                .EMAIL("test@example.com")
                .NAME("테스트유저")
                .PASSWORD("password123")
                .build();

        UserVO savedUser = UserVO.builder()
                .USER_ID(1L)
                .LOGIN_ID("testuser")
                .EMAIL("test@example.com")
                .NAME("테스트유저")
                .build();

        when(authService.signup(any(UserVO.class), anyString(), any())).thenReturn(savedUser);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("회원가입 성공"))
                .andExpect(jsonPath("$.data.LOGIN_ID").value("testuser"));
    }

    @Test
    @DisplayName("로컬 로그인 - 성공")
    void login_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("loginId", "testuser");
        request.put("password", "password123");

        LoginResponseVO response = LoginResponseVO.builder()
                .USER_ID(1L)
                .EMAIL("test@example.com")
                .SESSION_ID("session_789")
                .build();

        when(authService.localLogin(eq("testuser"), eq("password123"), anyString(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("로그인 성공"));
    }

    @Test
    @DisplayName("로컬 로그인 - 실패 (잘못된 자격증명)")
    void login_Fail_InvalidCredentials() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("loginId", "testuser");
        request.put("password", "wrongpassword");

        when(authService.localLogin(eq("testuser"), eq("wrongpassword"), anyString(), any()))
                .thenThrow(new IllegalArgumentException("아이디 또는 비밀번호가 잘못되었습니다."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== 로그아웃 테스트 ====================

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_ID", "session_123");
        session.setAttribute("USER_ID", 1);

        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        verify(authService, times(1)).logout("session_123");
    }

    // ==================== 사용자 정보 조회 테스트 ====================

    @Test
    @DisplayName("현재 사용자 조회 - 성공")
    void getCurrentUser_Success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_ID", "session_123");

        UserVO user = UserVO.builder()
                .USER_ID(1L)
                .EMAIL("test@example.com")
                .NAME("테스트유저")
                .build();

        when(authService.getUserBySession("session_123")).thenReturn(user);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.EMAIL").value("test@example.com"));
    }

    @Test
    @DisplayName("현재 사용자 조회 - 실패 (세션 없음)")
    void getCurrentUser_NoSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다"));
    }

    // ==================== 세션 검증 테스트 ====================

    @Test
    @DisplayName("세션 검증 - 유효")
    void validateSession_Valid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_ID", "session_123");

        UserVO user = UserVO.builder()
                .USER_ID(1L)
                .build();

        when(authService.getUserBySession("session_123")).thenReturn(user);

        mockMvc.perform(get("/api/auth/validate")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("세션 검증 - 무효 (세션 없음)")
    void validateSession_NoSession() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").value(false));
    }

    // ==================== 중복 체크 테스트 ====================

    @Test
    @DisplayName("아이디 중복 체크 - 사용 가능")
    void checkLoginId_Available() throws Exception {
        when(authService.isLoginIdAvailable("newuser")).thenReturn(true);

        mockMvc.perform(get("/api/auth/check-id")
                        .param("loginId", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 아이디입니다."));
    }

    @Test
    @DisplayName("아이디 중복 체크 - 이미 존재")
    void checkLoginId_NotAvailable() throws Exception {
        when(authService.isLoginIdAvailable("existinguser")).thenReturn(false);

        mockMvc.perform(get("/api/auth/check-id")
                        .param("loginId", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    @DisplayName("이메일 중복 체크 - 사용 가능")
    void checkEmail_Available() throws Exception {
        when(authService.isEmailAvailable("new@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
