package dev3.nms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.config.LoginCheckInterceptor;
import dev3.nms.mapper.*;
import dev3.nms.service.AuthService;
import dev3.nms.service.DeviceService;
import dev3.nms.service.GroupService;
import dev3.nms.service.PortService;
import dev3.nms.service.TempDeviceService;
import dev3.nms.vo.mgmt.GroupVO;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MgmtController.class)
class MgmtControllerGroupTest {

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
        return session;
    }

    // ==================== 그룹 조회 테스트 ====================

    @Test
    @DisplayName("전체 그룹 조회 - 성공")
    void getGroupHierarchy_Success() throws Exception {
        GroupVO group1 = GroupVO.builder()
                .GROUP_ID(1)
                .GROUP_NAME("서울본부")
                .PARENT_GROUP_ID(null)
                .DEPTH(0)
                .build();

        GroupVO group2 = GroupVO.builder()
                .GROUP_ID(2)
                .GROUP_NAME("강남지사")
                .PARENT_GROUP_ID(1)
                .DEPTH(1)
                .build();

        when(groupService.getGroupHierarchy()).thenReturn(Arrays.asList(group1, group2));

        mockMvc.perform(get("/api/mgmt/groups")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].GROUP_NAME").value("서울본부"));
    }

    @Test
    @DisplayName("그룹 트리 조회 - 성공")
    void getGroupTree_Success() throws Exception {
        GroupVO child = GroupVO.builder()
                .GROUP_ID(2)
                .GROUP_NAME("지사")
                .PARENT_GROUP_ID(1)
                .DEPTH(1)
                .build();

        GroupVO root = GroupVO.builder()
                .GROUP_ID(1)
                .GROUP_NAME("본부")
                .PARENT_GROUP_ID(null)
                .DEPTH(0)
                .children(Arrays.asList(child))
                .build();

        when(groupService.getGroupHierarchy()).thenReturn(Arrays.asList(root));

        mockMvc.perform(get("/api/mgmt/groups/tree")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].GROUP_NAME").value("본부"))
                .andExpect(jsonPath("$.data[0].children[0].GROUP_NAME").value("지사"));
    }

    @Test
    @DisplayName("특정 그룹 조회 - 성공")
    void getGroupById_Success() throws Exception {
        GroupVO group = GroupVO.builder()
                .GROUP_ID(1)
                .GROUP_NAME("서울본부")
                .ADDRESS("서울시 강남구")
                .PHONE("02-1234-5678")
                .build();

        when(groupService.getGroupById(1)).thenReturn(group);

        mockMvc.perform(get("/api/mgmt/groups/1")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.GROUP_NAME").value("서울본부"))
                .andExpect(jsonPath("$.data.ADDRESS").value("서울시 강남구"));
    }

    @Test
    @DisplayName("특정 그룹 조회 - 실패 (존재하지 않음)")
    void getGroupById_NotFound() throws Exception {
        when(groupService.getGroupById(999))
                .thenThrow(new IllegalArgumentException("Group not found or already deleted with ID: 999"));

        mockMvc.perform(get("/api/mgmt/groups/999")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 그룹 생성 테스트 ====================

    @Test
    @DisplayName("그룹 생성 - 성공 (루트 그룹)")
    void createGroup_RootGroup_Success() throws Exception {
        GroupVO input = GroupVO.builder()
                .GROUP_NAME("새로운 본부")
                .ADDRESS("서울시 종로구")
                .build();

        GroupVO savedGroup = GroupVO.builder()
                .GROUP_ID(10)
                .GROUP_NAME("새로운 본부")
                .ADDRESS("서울시 종로구")
                .DEPTH(0)
                .build();

        when(groupService.createGroup(any())).thenReturn(savedGroup);

        mockMvc.perform(post("/api/mgmt/groups")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.GROUP_NAME").value("새로운 본부"));
    }

    @Test
    @DisplayName("그룹 생성 - 성공 (하위 그룹)")
    void createGroup_ChildGroup_Success() throws Exception {
        GroupVO input = GroupVO.builder()
                .GROUP_NAME("하위 지사")
                .PARENT_GROUP_ID(1)
                .build();

        GroupVO savedGroup = GroupVO.builder()
                .GROUP_ID(11)
                .GROUP_NAME("하위 지사")
                .PARENT_GROUP_ID(1)
                .DEPTH(1)
                .build();

        when(groupService.createGroup(any())).thenReturn(savedGroup);

        mockMvc.perform(post("/api/mgmt/groups")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.PARENT_GROUP_ID").value(1));
    }

    // ==================== 그룹 수정 테스트 ====================

    @Test
    @DisplayName("그룹 수정 - 성공")
    void updateGroup_Success() throws Exception {
        GroupVO updateReq = GroupVO.builder()
                .GROUP_NAME("수정된 그룹명")
                .ADDRESS("수정된 주소")
                .build();

        GroupVO updatedGroup = GroupVO.builder()
                .GROUP_ID(1)
                .GROUP_NAME("수정된 그룹명")
                .ADDRESS("수정된 주소")
                .build();

        when(groupService.updateGroup(eq(1), any())).thenReturn(updatedGroup);

        mockMvc.perform(put("/api/mgmt/groups/1")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("그룹 수정 성공"))
                .andExpect(jsonPath("$.data.GROUP_NAME").value("수정된 그룹명"));
    }

    @Test
    @DisplayName("그룹 수정 - 실패 (존재하지 않음)")
    void updateGroup_NotFound() throws Exception {
        GroupVO updateReq = GroupVO.builder()
                .GROUP_NAME("수정")
                .build();

        when(groupService.updateGroup(eq(999), any()))
                .thenThrow(new IllegalArgumentException("Group not found or already deleted with ID: 999"));

        mockMvc.perform(put("/api/mgmt/groups/999")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 그룹 삭제 테스트 ====================

    @Test
    @DisplayName("그룹 삭제 - 성공")
    void deleteGroup_Success() throws Exception {
        doNothing().when(groupService).deleteGroup(1);

        mockMvc.perform(delete("/api/mgmt/groups/1")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("그룹 삭제 성공"));

        verify(groupService, times(1)).deleteGroup(1);
    }

    @Test
    @DisplayName("그룹 삭제 - 실패 (존재하지 않음)")
    void deleteGroup_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Group not found or already deleted with ID: 999"))
                .when(groupService).deleteGroup(999);

        mockMvc.perform(delete("/api/mgmt/groups/999")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("그룹 삭제 - 실패 (하위 그룹 존재)")
    void deleteGroup_HasChildren() throws Exception {
        doThrow(new IllegalStateException("하위 그룹이 존재하여 삭제할 수 없습니다."))
                .when(groupService).deleteGroup(1);

        mockMvc.perform(delete("/api/mgmt/groups/1")
                        .session(createAuthenticatedSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 그룹별 장비 조회 테스트 ====================

    @Test
    @DisplayName("그룹별 장비 조회 - 성공")
    void getDevicesByGroup_Success() throws Exception {
        List<Integer> groupIds = Arrays.asList(1, 2);

        when(deviceService.getDevicesByGroupIds(groupIds)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/mgmt/devices/search")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 그룹 이동 테스트 ====================

    @Test
    @DisplayName("그룹 이동 - 성공")
    void moveGroup_Success() throws Exception {
        GroupVO moveRequest = GroupVO.builder()
                .PARENT_GROUP_ID(2)
                .build();

        GroupVO movedGroup = GroupVO.builder()
                .GROUP_ID(3)
                .GROUP_NAME("이동된 그룹")
                .PARENT_GROUP_ID(2)
                .DEPTH(2)
                .build();

        when(groupService.moveGroup(anyInt(), any())).thenReturn(movedGroup);

        mockMvc.perform(patch("/api/mgmt/groups/3/move")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("그룹 이동 성공"))
                .andExpect(jsonPath("$.data.PARENT_GROUP_ID").value(2));
    }

    @Test
    @DisplayName("그룹 이동 - 실패 (자기 자신 하위로 이동 시도)")
    void moveGroup_InvalidMove() throws Exception {
        GroupVO moveRequest = GroupVO.builder()
                .PARENT_GROUP_ID(5)
                .build();

        when(groupService.moveGroup(anyInt(), any()))
                .thenThrow(new IllegalArgumentException("그룹을 자신의 하위 그룹으로 이동할 수 없습니다."));

        mockMvc.perform(patch("/api/mgmt/groups/3/move")
                        .session(createAuthenticatedSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
