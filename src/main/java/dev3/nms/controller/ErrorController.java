package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.config.RequireEditPermission;
import dev3.nms.service.ErrorService;
import dev3.nms.service.PermissionService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fault")
@RequiredArgsConstructor
public class ErrorController {

    private final ErrorService errorService;
    private final PermissionService permissionService;
    private final dev3.nms.mapper.ErrorMapper errorMapper;

    /**
     * 실시간 장애 목록 조회
     */
    @GetMapping("/errors")
    public ResVO<Map<String, Object>> getErrors(
            @RequestParam(required = false) String errorLevel,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long devCodeId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {

        // 권한 기반 + 프론트 필터 교집합
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> effectiveDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);

        List<ErrorVO> errors = errorService.getErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);
        int totalCount = errorService.countErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);

        int criticalCount = errorService.countErrors("C", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);
        int majorCount = errorService.countErrors("M", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);
        int minorCount = errorService.countErrors("N", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);
        int warningCount = errorService.countErrors("W", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, effectiveDeviceIds);

        Map<String, Object> result = new HashMap<>();
        result.put("list", errors);
        result.put("totalCount", totalCount);
        result.put("criticalCount", criticalCount);
        result.put("majorCount", majorCount);
        result.put("minorCount", minorCount);
        result.put("warningCount", warningCount);

        return new ResVO<>(200, "조회 성공", result);
    }

    /**
     * 장애 상세 조회
     */
    @GetMapping("/errors/{errorId}")
    public ResVO<ErrorVO> getError(@PathVariable Long errorId, HttpSession session) {
        ErrorVO error = errorService.getErrorById(errorId);
        if (error == null) {
            return new ResVO<>(404, "장애 정보를 찾을 수 없습니다", null);
        }
        return new ResVO<>(200, "조회 성공", error);
    }

    /**
     * 장애 인지 처리
     */
    @AuditLog(actionType = "UPDATE", targetType = "ERROR", pageCode = "fault_realtime")
    @RequireEditPermission("fault_realtime")
    @PutMapping("/errors/{errorId}/acknowledge")
    public ResVO<Void> acknowledgeError(
            @PathVariable Long errorId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("userMessage");
        boolean success = errorService.acknowledgeError(errorId, userMessage);

        if (success) {
            return new ResVO<>(200, "인지 처리 완료", null);
        } else {
            return new ResVO<>(400, "인지 처리 실패", null);
        }
    }

    /**
     * 장애 이력 목록 조회 (페이징)
     */
    @GetMapping("/history")
    public ResVO<PageVO<ErrorHistoryVO>> getErrorHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String errorLevel,
            @RequestParam(required = false) List<String> errorLevels,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long devCodeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) String groupName,
            @RequestParam(defaultValue = "CLEAR_AT") String sortKey,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {

        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> effectiveDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);

        // errorLevels (다중) 우선, 없으면 errorLevel (단일) 사용
        String effectiveLevel = errorLevel;
        List<String> effectiveLevels = errorLevels;

        PageVO<ErrorHistoryVO> pageVO = errorService.getErrorHistory(
                page, size, effectiveLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName,
                sortKey, sortDirection, effectiveDeviceIds, effectiveLevels);

        return new ResVO<>(200, "조회 성공", pageVO);
    }

    /**
     * 장애 이력 상세 조회
     */
    @GetMapping("/history/{errorHistoryId}")
    public ResVO<ErrorHistoryVO> getErrorHistoryDetail(@PathVariable Long errorHistoryId) {
        ErrorHistoryVO history = errorService.getErrorHistoryById(errorHistoryId);
        if (history == null) {
            return new ResVO<>(404, "이력 정보를 찾을 수 없습니다", null);
        }
        return new ResVO<>(200, "조회 성공", history);
    }

    // ========== 장애 통계 ==========

    /**
     * 장애 통계 요약
     */
    @GetMapping("/stats/summary")
    public ResVO<Map<String, Object>> getStatsSummary(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> filteredDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);
        return new ResVO<>(200, "조회 성공", errorService.getErrorStatsSummary(startDate, endDate, groupIds, filteredDeviceIds));
    }

    /**
     * 장애 발생 추이
     */
    @GetMapping("/stats/trend")
    public ResVO<Map<String, Object>> getStatsTrend(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> filteredDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);
        return new ResVO<>(200, "조회 성공", errorService.getErrorTrend(startDate, endDate, period, groupIds, filteredDeviceIds));
    }

    /**
     * MTTR 통계
     */
    @GetMapping("/stats/mttr")
    public ResVO<List<Map<String, Object>>> getStatsMttr(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> filteredDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);
        return new ResVO<>(200, "조회 성공", errorService.getMttr(startDate, endDate, groupIds, filteredDeviceIds));
    }

    /**
     * 상습 장애 장비 Top N
     */
    @GetMapping("/stats/top-devices")
    public ResVO<List<Map<String, Object>>> getStatsTopDevices(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> filteredDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);
        return new ResVO<>(200, "조회 성공", errorService.getTopErrorDevices(startDate, endDate, limit, groupIds, filteredDeviceIds));
    }

    /**
     * 시간대/요일별 장애 패턴
     */
    @GetMapping("/stats/pattern")
    public ResVO<Map<String, Object>> getStatsPattern(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) List<Long> groupIds,
            @RequestParam(required = false) List<Long> deviceIds,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Long> filteredDeviceIds = intersectDeviceIds(deviceIds, accessibleDeviceIds);
        return new ResVO<>(200, "조회 성공", errorService.getErrorPattern(startDate, endDate, groupIds, filteredDeviceIds));
    }

    // ========== 헬퍼 ==========

    private List<Long> getAccessibleDeviceIds(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return List.of(); // 비로그인 → 빈 리스트
        return permissionService.getAccessibleDeviceIds(userId);
    }

    /**
     * 프론트에서 보낸 deviceIds와 접근 가능 deviceIds의 교집합
     * accessibleDeviceIds가 null이면 전체 허용 → 프론트 요청 그대로 반환
     */
    private List<Long> intersectDeviceIds(List<Long> requestedDeviceIds, List<Long> accessibleDeviceIds) {
        if (accessibleDeviceIds == null) {
            return requestedDeviceIds; // 전체 허용
        }
        if (requestedDeviceIds == null || requestedDeviceIds.isEmpty()) {
            return accessibleDeviceIds; // 프론트에서 필터 안 함 → 접근 가능 전체
        }
        // 교집합
        return requestedDeviceIds.stream()
                .filter(accessibleDeviceIds::contains)
                .toList();
    }

    /**
     * 장애 이력의 페이지 위치 조회
     * 해당 이력이 장비별 목록에서 몇 페이지에 있는지 반환
     */
    @GetMapping("/history/{errorHistoryId}/position")
    public ResVO<Map<String, Object>> getHistoryPosition(
            @PathVariable Long errorHistoryId,
            @RequestParam Long deviceId,
            @RequestParam(defaultValue = "20") int size) {
        // 해당 이력 앞에 몇 건이 있는지 조회
        int rowsBefore = errorMapper.countHistoryBefore(errorHistoryId, deviceId);
        int page = (rowsBefore / size) + 1;

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("position", rowsBefore + 1);
        return new ResVO<>(200, "조회 성공", result);
    }
}
