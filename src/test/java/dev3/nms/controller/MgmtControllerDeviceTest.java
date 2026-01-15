package dev3.nms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.config.LoginCheckInterceptor;
import dev3.nms.mapper.*;
import dev3.nms.service.AuthService;
import dev3.nms.service.DeviceService;
import dev3.nms.service.GroupService;
import dev3.nms.service.PortService;
import dev3.nms.service.TempDeviceService;
import dev3.nms.vo.auth.UserVO;
import dev3.nms.vo.mgmt.DeviceRegistrationResultVO;
import dev3.nms.vo.mgmt.DeviceScopeVO;
import dev3.nms.vo.mgmt.DeviceVO;
import dev3.nms.vo.mgmt.TempDeviceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MgmtController.class)
class MgmtControllerDeviceTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private PortService portService;

    @MockitoBean
    private TempDeviceService tempDeviceService;

    @MockitoBean
    private AuthService authService;

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

    @MockitoBean
    private LoginCheckInterceptor loginCheckInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // LoginCheckInterceptor가 모든 요청을 통과하도록 설정
        when(loginCheckInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private MockHttpSession createAuthenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1);
        session.setAttribute("SESSION_ID", "session_test");

        // Mock authService to return user
        UserVO user = UserVO.builder().USER_ID(1L).build();
        when(authService.getUserBySession("session_test")).thenReturn(user);

        return session;
    }

    // ==================== 장비 조회 테스트 ====================

    @Test
    @DisplayName("전체 장비 조회 - 성공")
    void getAllDevices_Success() throws Exception {
        DeviceVO device1 = DeviceVO.builder()
                .DEVICE_ID(1)
                .DEVICE_NAME("Switch-01")
                .DEVICE_IP("192.168.1.1")
                .build();

        DeviceVO device2 = DeviceVO.builder()
                .DEVICE_ID(2)
                .DEVICE_NAME("Router-01")
                .DEVICE_IP("192.168.1.2")
                .build();

        when(deviceService.getAllDevices()).thenReturn(Arrays.asList(device1, device2));

        mockMvc.perform(get("/api/mgmt/devices")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].DEVICE_NAME").value("Switch-01"));
    }

    @Test
    @DisplayName("특정 장비 조회 - 성공")
    void getDeviceById_Success() throws Exception {
        DeviceVO device = DeviceVO.builder()
                .DEVICE_ID(1)
                .DEVICE_NAME("Switch-01")
                .DEVICE_IP("192.168.1.1")
                .SNMP_VERSION(2)
                .SNMP_COMMUNITY("public")
                .build();

        when(deviceService.getDeviceById(1)).thenReturn(device);

        mockMvc.perform(get("/api/mgmt/devices/1")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.DEVICE_NAME").value("Switch-01"))
                .andExpect(jsonPath("$.data.SNMP_VERSION").value(2));
    }

    @Test
    @DisplayName("특정 장비 조회 - 실패 (존재하지 않음)")
    void getDeviceById_NotFound() throws Exception {
        when(deviceService.getDeviceById(999)).thenReturn(null);

        mockMvc.perform(get("/api/mgmt/devices/999")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 장비 등록 테스트 ====================

    @Test
    @DisplayName("장비 직접 등록 - SNMP v2c 성공")
    void createDeviceDirectly_SnmpV2c_Success() throws Exception {
        TempDeviceVO input = new TempDeviceVO();
        input.setDEVICE_IP("192.168.1.100");
        input.setDEVICE_NAME("New-Switch");
        input.setSNMP_VERSION(2);
        input.setSNMP_PORT(161);
        input.setSNMP_COMMUNITY("public");
        input.setCOLLECT_SNMP(true);

        DeviceRegistrationResultVO.DeviceRegistrationSuccess success =
                new DeviceRegistrationResultVO.DeviceRegistrationSuccess(
                        null, 10, "New-Switch", "192.168.1.100",
                        "Switch-System", "Cisco", "New switch", true, "SNMP"
                );

        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();
        result.getSuccessList().add(success);

        when(deviceService.createDeviceDirectly(any(), anyInt())).thenReturn(result);

        mockMvc.perform(post("/api/mgmt/devices/direct")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    @DisplayName("장비 직접 등록 - SNMP v3 성공")
    void createDeviceDirectly_SnmpV3_Success() throws Exception {
        TempDeviceVO input = new TempDeviceVO();
        input.setDEVICE_IP("192.168.1.100");
        input.setDEVICE_NAME("New-Switch");
        input.setSNMP_VERSION(3);
        input.setSNMP_PORT(161);
        input.setSNMP_USER("admin");
        input.setSNMP_AUTH_PROTOCOL("SHA");
        input.setSNMP_AUTH_PASSWORD("authpass");
        input.setSNMP_PRIV_PROTOCOL("AES");
        input.setSNMP_PRIV_PASSWORD("privpass");
        input.setCOLLECT_SNMP(true);

        DeviceRegistrationResultVO.DeviceRegistrationSuccess success =
                new DeviceRegistrationResultVO.DeviceRegistrationSuccess(
                        null, 10, "New-Switch", "192.168.1.100",
                        "Switch-System", "Cisco", "New switch", true, "SNMP"
                );

        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();
        result.getSuccessList().add(success);

        when(deviceService.createDeviceDirectly(any(), anyInt())).thenReturn(result);

        mockMvc.perform(post("/api/mgmt/devices/direct")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));
    }

    // ==================== 장비 수정 테스트 ====================

    @Test
    @DisplayName("장비 수정 - 기본 정보 수정")
    void updateDevice_BasicInfo() throws Exception {
        DeviceVO updateReq = DeviceVO.builder()
                .DEVICE_NAME("Updated-Switch")
                .DEVICE_DESC("Updated description")
                .build();

        DeviceVO updatedDevice = DeviceVO.builder()
                .DEVICE_ID(1)
                .DEVICE_NAME("Updated-Switch")
                .DEVICE_DESC("Updated description")
                .build();

        when(deviceService.updateDevice(eq(1), any())).thenReturn(updatedDevice);

        mockMvc.perform(put("/api/mgmt/devices/1")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.DEVICE_NAME").value("Updated-Switch"));
    }

    @Test
    @DisplayName("장비 수정 - SNMP v3 설정 변경")
    void updateDevice_SnmpV3() throws Exception {
        DeviceVO updateReq = DeviceVO.builder()
                .SNMP_VERSION(3)
                .SNMP_PORT(161)
                .SNMP_USER("newadmin")
                .SNMP_AUTH_PROTOCOL("SHA256")
                .SNMP_AUTH_PASSWORD("newauth")
                .SNMP_PRIV_PROTOCOL("AES256")
                .SNMP_PRIV_PASSWORD("newpriv")
                .build();

        DeviceVO updatedDevice = DeviceVO.builder()
                .DEVICE_ID(1)
                .SNMP_VERSION(3)
                .SNMP_USER("newadmin")
                .build();

        when(deviceService.updateDevice(eq(1), any())).thenReturn(updatedDevice);

        mockMvc.perform(put("/api/mgmt/devices/1")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.SNMP_VERSION").value(3))
                .andExpect(jsonPath("$.data.SNMP_USER").value("newadmin"));
    }

    @Test
    @DisplayName("장비 수정 - 실패 (존재하지 않는 장비)")
    void updateDevice_NotFound() throws Exception {
        DeviceVO updateReq = DeviceVO.builder()
                .DEVICE_NAME("Updated")
                .build();

        when(deviceService.updateDevice(eq(999), any()))
                .thenThrow(new IllegalArgumentException("장비를 찾을 수 없습니다: 999"));

        mockMvc.perform(put("/api/mgmt/devices/999")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 장비 삭제 테스트 ====================

    @Test
    @DisplayName("장비 삭제 - 성공")
    void deleteDevice_Success() throws Exception {
        doNothing().when(deviceService).deleteDevice(1);

        mockMvc.perform(delete("/api/mgmt/devices/1")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("장비 삭제 성공"));

        verify(deviceService, times(1)).deleteDevice(1);
    }

    @Test
    @DisplayName("장비 일괄 삭제 - 성공")
    void deleteDevices_Success() throws Exception {
        Map<String, List<Integer>> requestBody = new HashMap<>();
        requestBody.put("deviceIds", Arrays.asList(1, 2, 3));

        doNothing().when(deviceService).deleteDevices(anyList());

        mockMvc.perform(delete("/api/mgmt/devices")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deviceService, times(1)).deleteDevices(anyList());
    }

    // ==================== 관제 설정 테스트 ====================

    @Test
    @DisplayName("관제 설정 조회 - 성공")
    void getDeviceScope_Success() throws Exception {
        DeviceScopeVO scope = DeviceScopeVO.builder()
                .DEVICE_ID(1)
                .COLLECT_PING(true)
                .COLLECT_SNMP(true)
                .COLLECT_AGENT(false)
                .build();

        when(deviceService.getDeviceScope(1)).thenReturn(scope);

        mockMvc.perform(get("/api/mgmt/devices/1/scope")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.COLLECT_PING").value(true))
                .andExpect(jsonPath("$.data.COLLECT_SNMP").value(true))
                .andExpect(jsonPath("$.data.COLLECT_AGENT").value(false));
    }

    @Test
    @DisplayName("관제 설정 수정 - 성공")
    void updateDeviceScope_Success() throws Exception {
        DeviceScopeVO scopeUpdate = DeviceScopeVO.builder()
                .COLLECT_PING(true)
                .COLLECT_SNMP(false)
                .COLLECT_AGENT(true)
                .build();

        DeviceScopeVO updatedScope = DeviceScopeVO.builder()
                .DEVICE_ID(1)
                .COLLECT_PING(true)
                .COLLECT_SNMP(false)
                .COLLECT_AGENT(true)
                .build();

        when(deviceService.updateDeviceScope(any())).thenReturn(updatedScope);

        mockMvc.perform(put("/api/mgmt/devices/1/scope")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scopeUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.COLLECT_AGENT").value(true));
    }

    // ==================== IP 중복 검증 테스트 ====================

    @Test
    @DisplayName("IP 중복 검증 - 중복 없음")
    void validateDevices_NoDuplicate() throws Exception {
        List<TempDeviceVO> devices = Arrays.asList(
                createTempDevice("192.168.1.100"),
                createTempDevice("192.168.1.101")
        );

        when(deviceService.validateDevices(any())).thenReturn(devices);

        mockMvc.perform(post("/api/mgmt/devices/validate")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devices)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private TempDeviceVO createTempDevice(String ip) {
        TempDeviceVO device = new TempDeviceVO();
        device.setDEVICE_IP(ip);
        device.setDEVICE_NAME("Device-" + ip);
        return device;
    }
}
