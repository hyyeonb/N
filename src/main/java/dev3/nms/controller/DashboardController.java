package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.service.DashboardService;
import dev3.nms.service.PermissionService;
import dev3.nms.service.TopoService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.dashboard.DashboardDto;
import dev3.nms.vo.topo.TopoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PermissionService permissionService;

    /**
     * 대시보드 위젯 목록 조회
     */
    @GetMapping("/widgets")
    public ResponseEntity<ResVO<List<DashboardDto.WidgetsRes>>> getWidgetsView() {
        List<DashboardDto.WidgetsRes> widgets = dashboardService.getWidgets();
        ResVO<List<DashboardDto.WidgetsRes>> response = new ResVO<>(200, "조회 성공", widgets);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 대시보드 위젯 조회
     */
    @GetMapping("/default-widget")
    public ResponseEntity<ResVO<List<DashboardDto.DefaultWidgetRes>>> getDefaultWidgetView(
            HttpSession session
    ) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<DashboardDto.DefaultWidgetRes> defaultWidgetRes = dashboardService.getDefaultWidget(accessibleDeviceIds);
        ResVO<List<DashboardDto.DefaultWidgetRes>> response = new ResVO<>(200, "조회 성공", defaultWidgetRes);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 기본 대시보드 특정 위젯 데이터 갱신
     */
    @GetMapping("/default-widget/{widgetId}")
    public ResponseEntity<ResVO<DashboardDto.DefaultWidgetRes>> getDefaultWidgetById(
            @PathVariable Long widgetId,
            HttpSession session
    ) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        DashboardDto.DefaultWidgetRes defaultWidgetRes = dashboardService.getDefaultWidgetById(widgetId, accessibleDeviceIds);
        if (defaultWidgetRes == null) {
            ResVO<DashboardDto.DefaultWidgetRes> response = new ResVO<>(404, "위젯을 찾을 수 없습니다", null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ResVO<DashboardDto.DefaultWidgetRes> response = new ResVO<>(200, "조회 성공", defaultWidgetRes);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 대시보드 위젯 표출 조회
     */
    @GetMapping("/user-widget/{userId}")
    public ResponseEntity<ResVO<List<DashboardDto.UserWidgetRes>>> getUserWidgetView(
            @PathVariable Long userId,
            HttpSession session
    ) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return new ResponseEntity<>(new ResVO<>(403, "접근 권한이 없습니다", null), HttpStatus.FORBIDDEN);
        }
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<DashboardDto.UserWidgetRes> userWidgets = dashboardService.getUserWidget(userId, accessibleDeviceIds);
        ResVO<List<DashboardDto.UserWidgetRes>> response = new ResVO<>(200, "조회 성공", userWidgets);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 사용자 대시보드 특정 위젯 데이터 갱신
     */
    @GetMapping("/user-widget/refresh/{userDashboardWidgetId}")
    public ResponseEntity<ResVO<DashboardDto.UserWidgetRes>> getUserWidgetById(
            @PathVariable Long userDashboardWidgetId,
            HttpSession session
    ) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        DashboardDto.UserWidgetRes userWidgetRes = dashboardService.getUserWidgetById(userDashboardWidgetId, accessibleDeviceIds);
        if (userWidgetRes == null) {
            ResVO<DashboardDto.UserWidgetRes> response = new ResVO<>(404, "위젯을 찾을 수 없습니다", null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ResVO<DashboardDto.UserWidgetRes> response = new ResVO<>(200, "조회 성공", userWidgetRes);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 대시보드 위젯 표출 저장
     */
    @AuditLog(actionType = "UPDATE", targetType = "DASHBOARD", pageCode = "dashboard")
    @PutMapping("/user-widget/{userId}")
    public ResponseEntity<ResVO<Boolean>> putUserWidget(
            @PathVariable Long userId,
            @RequestBody DashboardDto.UpdateUserWidgetsReq req,
            HttpSession session
    ) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return new ResponseEntity<>(new ResVO<>(403, "접근 권한이 없습니다", null), HttpStatus.FORBIDDEN);
        }
        Boolean userWidgets = dashboardService.putUserWidget(userId, req);
        ResVO<Boolean> response = new ResVO<>(200, "저장 성공", userWidgets);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 대시보드 위젯 초기화
     */
    @AuditLog(actionType = "UPDATE", targetType = "DASHBOARD", pageCode = "dashboard")
    @PostMapping("/user-widget/{userId}/reset")
    public ResponseEntity<ResVO<List<DashboardDto.UserWidgetRes>>> resetUserWidget(
            @PathVariable Long userId,
            HttpSession session
    ) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return new ResponseEntity<>(new ResVO<>(403, "접근 권한이 없습니다", null), HttpStatus.FORBIDDEN);
        }
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<DashboardDto.UserWidgetRes> userWidgets = dashboardService.resetUserWidget(userId, accessibleDeviceIds);
        ResVO<List<DashboardDto.UserWidgetRes>> response = new ResVO<>(200, "리셋 성공", userWidgets);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<Long> getAccessibleDeviceIds(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return List.of();
        return permissionService.getAccessibleDeviceIds(userId);
    }

}
