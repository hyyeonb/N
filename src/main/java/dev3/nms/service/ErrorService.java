package dev3.nms.service;

import dev3.nms.mapper.ErrorMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorService {

    private final ErrorMapper errorMapper;

    /**
     * 실시간 장애 목록 조회
     */
    public List<ErrorVO> getErrors(String errorLevel, Long deviceId, Long devCodeId,
                                    String deviceName, String deviceIp,
                                    String errorMessage, String groupName,
                                    List<Long> accessibleDeviceIds) {
        return errorMapper.selectErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, accessibleDeviceIds);
    }

    /**
     * 실시간 장애 수 조회
     */
    public int countErrors(String errorLevel, Long deviceId, Long devCodeId,
                           String deviceName, String deviceIp,
                           String errorMessage, String groupName,
                           List<Long> accessibleDeviceIds) {
        return errorMapper.countErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName, accessibleDeviceIds);
    }

    /**
     * 장애 상세 조회
     */
    public ErrorVO getErrorById(Long errorId) {
        return errorMapper.selectErrorById(errorId);
    }

    /**
     * 장애 인지 처리
     */
    @Transactional
    public boolean acknowledgeError(Long errorId, String userMessage) {
        int result = errorMapper.updateErrorFlag(errorId, 1, userMessage);
        return result > 0;
    }

    /**
     * 장애 이력 목록 조회 (페이징)
     */
    public PageVO<ErrorHistoryVO> getErrorHistory(int page, int size,
                                                   String errorLevel, Long deviceId, Long devCodeId,
                                                   String startDate, String endDate,
                                                   String deviceName, String deviceIp,
                                                   String errorMessage, String groupName,
                                                   String sortKey, String sortDirection,
                                                   List<Long> accessibleDeviceIds) {
        return getErrorHistory(page, size, errorLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName, sortKey, sortDirection, accessibleDeviceIds, null);
    }

    public PageVO<ErrorHistoryVO> getErrorHistory(int page, int size,
                                                   String errorLevel, Long deviceId, Long devCodeId,
                                                   String startDate, String endDate,
                                                   String deviceName, String deviceIp,
                                                   String errorMessage, String groupName,
                                                   String sortKey, String sortDirection,
                                                   List<Long> accessibleDeviceIds, List<String> errorLevels) {
        int offset = (page - 1) * size;
        String effectiveLevel = (errorLevels != null && !errorLevels.isEmpty()) ? null : errorLevel;
        List<ErrorHistoryVO> list = errorMapper.selectErrorHistory(
                effectiveLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName,
                sortKey, sortDirection, offset, size, accessibleDeviceIds, errorLevels);
        int total = errorMapper.countErrorHistory(
                effectiveLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName, accessibleDeviceIds, errorLevels);

        return PageVO.of(list, page, size, total);
    }

    /**
     * 장애 이력 상세 조회
     */
    public ErrorHistoryVO getErrorHistoryById(Long errorHistoryId) {
        return errorMapper.selectErrorHistoryById(errorHistoryId);
    }

    /**
     * 포트 비관리 전환 시 해당 포트의 PORT 장애 자동 해소
     * f_error_t → f_error_history_t (CLEAR_AT=NOW()) 이관 후 삭제
     */
    @Transactional
    public void clearPortError(Integer deviceId, Integer ifIndex) {
        int moved = errorMapper.insertPortErrorHistory(deviceId, ifIndex);
        if (moved > 0) {
            errorMapper.deletePortError(deviceId, ifIndex);
            log.info("포트 비관리 - 장애 자동 해소: deviceId={}, ifIndex={}, {}건", deviceId, ifIndex, moved);
        }
    }

    /**
     * 삭제된 포트(DELETE_AT IS NOT NULL)의 잔존 Oper 장애 정리
     * Docker 컨테이너 등 동적 인터페이스가 삭제된 후 장애가 남는 경우 처리
     */
    @Transactional
    public void cleanupDeletedPortErrors(Integer deviceId) {
        int moved = errorMapper.moveDeletedPortErrorsToHistory(deviceId);
        if (moved > 0) {
            int deleted = errorMapper.deleteDeletedPortErrors(deviceId);
            log.info("삭제 포트 장애 정리: deviceId={}, {}건 이관, {}건 삭제", deviceId, moved, deleted);
        }
    }

    // ========== 장애 통계 ==========

    /**
     * 장애 통계 요약 (기간 내 발생 기준: 등급별 + 유형별 + Aging)
     */
    public Map<String, Object> getErrorStatsSummary(String startDate, String endDate, List<Long> groupIds, List<Long> deviceIds) {
        Map<String, Object> result = new HashMap<>();
        result.put("byLevel", errorMapper.selectErrorCountByLevel(startDate, endDate, groupIds, deviceIds));
        result.put("byType", errorMapper.selectErrorCountByType(startDate, endDate, groupIds, deviceIds));
        result.put("aging", errorMapper.selectErrorAging(startDate, endDate, groupIds, deviceIds));
        return result;
    }

    /**
     * 장애 발생 추이 (period: daily/hourly)
     */
    public Map<String, Object> getErrorTrend(String startDate, String endDate, String period, List<Long> groupIds, List<Long> deviceIds) {
        Map<String, Object> result = new HashMap<>();
        if ("hourly".equals(period)) {
            result.put("hourly", errorMapper.selectErrorTrendHourly(startDate, endDate, groupIds, deviceIds));
            result.put("byType", errorMapper.selectErrorTrendByTypeHourly(startDate, endDate, groupIds, deviceIds));
            result.put("period", "hourly");
        } else {
            result.put("daily", errorMapper.selectErrorTrendDaily(startDate, endDate, groupIds, deviceIds));
            result.put("byType", errorMapper.selectErrorTrendByType(startDate, endDate, groupIds, deviceIds));
            result.put("period", "daily");
        }
        return result;
    }

    /**
     * MTTR 통계
     */
    public List<Map<String, Object>> getMttr(String startDate, String endDate, List<Long> groupIds, List<Long> deviceIds) {
        return errorMapper.selectMttrByType(startDate, endDate, groupIds, deviceIds);
    }

    /**
     * 상습 장애 장비 Top N
     */
    public List<Map<String, Object>> getTopErrorDevices(String startDate, String endDate, int limit, List<Long> groupIds, List<Long> deviceIds) {
        return errorMapper.selectTopErrorDevices(startDate, endDate, limit, groupIds, deviceIds);
    }

    /**
     * 시간대/요일별 장애 패턴
     */
    public Map<String, Object> getErrorPattern(String startDate, String endDate, List<Long> groupIds, List<Long> deviceIds) {
        Map<String, Object> result = new HashMap<>();
        result.put("byHour", errorMapper.selectErrorPatternByHour(startDate, endDate, groupIds, deviceIds));
        result.put("byDow", errorMapper.selectErrorPatternByDow(startDate, endDate, groupIds, deviceIds));
        return result;
    }
}
