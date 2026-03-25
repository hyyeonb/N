package dev3.nms.service;

import dev3.nms.mapper.ActivityLogMapper;
import dev3.nms.vo.auth.ActivityLogVO;
import dev3.nms.vo.common.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    /**
     * 활동 로그 비동기 기록
     */
    @Async("auditLogExecutor")
    public void logActivity(ActivityLogVO logVO) {
        try {
            activityLogMapper.insert(logVO);
        } catch (Exception e) {
            log.warn("[ActivityLog] INSERT 실패: {}", e.getMessage());
        }
    }

    /**
     * 페이지 뷰 비동기 기록
     */
    @Async("auditLogExecutor")
    public void logPageView(Long historyId, Long userId, String pageCode, String pagePath, String ipAddress,
                            String targetType, String targetName, String detail) {
        try {
            ActivityLogVO logVO = ActivityLogVO.builder()
                    .HISTORY_ID(historyId)
                    .USER_ID(userId)
                    .ACTION_TYPE("PAGE_VIEW")
                    .PAGE_CODE(pageCode)
                    .REQUEST_URI(pagePath)
                    .IP_ADDRESS(ipAddress)
                    .TARGET_TYPE(targetType)
                    .TARGET_NAME(targetName)
                    .DETAIL(detail)
                    .build();
            activityLogMapper.insert(logVO);
        } catch (Exception e) {
            log.warn("[ActivityLog] PAGE_VIEW INSERT 실패: {}", e.getMessage());
        }
    }

    /**
     * 특정 로그인 세션의 활동 로그 조회
     */
    public PageVO<ActivityLogVO> getActivitiesByHistoryId(Long historyId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("historyId", historyId);
        params.put("offset", (page - 1) * size);
        params.put("size", size);

        List<ActivityLogVO> content = activityLogMapper.findByHistoryId(params);
        long totalElements = activityLogMapper.countByHistoryId(historyId);

        return PageVO.of(content, page, size, totalElements);
    }

    /**
     * 특정 장비의 변경 이력 조회
     */
    public PageVO<ActivityLogVO> getActivitiesByDeviceId(String deviceId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId", deviceId);
        params.put("offset", (page - 1) * size);
        params.put("size", size);

        List<ActivityLogVO> content = activityLogMapper.findByDeviceId(params);
        long totalElements = activityLogMapper.countByDeviceId(params);

        return PageVO.of(content, page, size, totalElements);
    }

    /**
     * 활동 로그 페이지네이션 조회
     */
    public PageVO<ActivityLogVO> getActivityLogs(int page, int size, String sort, String order,
                                                  String actionType, String targetType,
                                                  Long userId, String startDate, String endDate,
                                                  String search) {
        Map<String, Object> params = new HashMap<>();
        params.put("offset", (page - 1) * size);
        params.put("size", size);
        params.put("sort", sort);
        params.put("order", order);
        params.put("actionType", actionType);
        params.put("targetType", targetType);
        params.put("userId", userId);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("search", search);

        List<ActivityLogVO> content = activityLogMapper.findPaged(params);
        long totalElements = activityLogMapper.count(params);

        return PageVO.of(content, page, size, totalElements);
    }
}
