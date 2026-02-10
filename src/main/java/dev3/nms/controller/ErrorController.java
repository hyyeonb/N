package dev3.nms.controller;

import dev3.nms.service.ErrorService;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
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

    /**
     * 실시간 장애 목록 조회
     * GET /api/fault/errors?errorLevel=C&deviceId=1&deviceName=xxx
     */
    @GetMapping("/errors")
    public ResVO<Map<String, Object>> getErrors(
            @RequestParam(required = false) String errorLevel,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long devCodeId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) String groupName) {

        List<ErrorVO> errors = errorService.getErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
        int totalCount = errorService.countErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);

        // 등급별 카운트 (검색 조건 적용)
        int criticalCount = errorService.countErrors("C", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
        int majorCount = errorService.countErrors("M", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
        int minorCount = errorService.countErrors("N", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
        int warningCount = errorService.countErrors("W", deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);

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
     * GET /api/fault/errors/{errorId}
     */
    @GetMapping("/errors/{errorId}")
    public ResVO<ErrorVO> getError(@PathVariable Long errorId) {
        ErrorVO error = errorService.getErrorById(errorId);
        if (error == null) {
            return new ResVO<>(404, "장애 정보를 찾을 수 없습니다", null);
        }
        return new ResVO<>(200, "조회 성공", error);
    }

    /**
     * 장애 인지 처리
     * PUT /api/fault/errors/{errorId}/acknowledge
     */
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
     * GET /api/fault/history?page=1&size=20&errorLevel=C&startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/history")
    public ResVO<PageVO<ErrorHistoryVO>> getErrorHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String errorLevel,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long devCodeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) String groupName,
            @RequestParam(defaultValue = "CLEAR_AT") String sortKey,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        PageVO<ErrorHistoryVO> pageVO = errorService.getErrorHistory(
                page, size, errorLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName,
                sortKey, sortDirection);

        return new ResVO<>(200, "조회 성공", pageVO);
    }

    /**
     * 장애 이력 상세 조회
     * GET /api/fault/history/{errorHistoryId}
     */
    @GetMapping("/history/{errorHistoryId}")
    public ResVO<ErrorHistoryVO> getErrorHistoryDetail(@PathVariable Long errorHistoryId) {
        ErrorHistoryVO history = errorService.getErrorHistoryById(errorHistoryId);
        if (history == null) {
            return new ResVO<>(404, "이력 정보를 찾을 수 없습니다", null);
        }
        return new ResVO<>(200, "조회 성공", history);
    }
}
