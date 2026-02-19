package dev3.nms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.mapper.GroupMapper;
import dev3.nms.mapper.WatchMapper;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.watch.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 관제 서비스
 * - 관제 그룹 CRUD
 * - Go Middleware 프록시
 * - Redis 메트릭 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchService {

    private final WatchMapper watchMapper;
    private final GroupMapper groupMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${middleware.url:http://localhost:18081}")
    private String middlewareUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ==================== 관제 그룹 CRUD ====================

    /**
     * 모든 관제 그룹 목록 조회
     */
    public List<WatchGroupVO> getAllGroups() {
        return watchMapper.findAllGroups();
    }

    /**
     * 관제 그룹 상세 조회 (장비 + 인터페이스 포함)
     */
    public WatchGroupVO getGroupDetail(Integer watchGroupId) {
        WatchGroupVO group = watchMapper.findGroupById(watchGroupId);
        if (group == null) {
            throw new IllegalArgumentException("관제 그룹을 찾을 수 없습니다: " + watchGroupId);
        }

        // 장비 목록 조회
        List<WatchGroupDeviceVO> devices = watchMapper.findDevicesByGroupId(watchGroupId);

        // 각 장비별 인터페이스 목록 조회
        for (WatchGroupDeviceVO device : devices) {
            List<WatchGroupIfVO> interfaces = watchMapper.findInterfacesByDevice(watchGroupId, device.getDEVICE_ID());
            device.setInterfaces(interfaces);
        }

        group.setDevices(devices);
        return group;
    }

    /**
     * 관제 그룹 생성
     */
    @Transactional
    public WatchGroupVO createGroup(WatchGroupVO group, List<WatchGroupDeviceVO> devices) {
        // 그룹 생성
        watchMapper.insertGroup(group);
        Integer groupId = group.getWATCH_GROUP_ID();

        // 장비 및 인터페이스 추가
        if (devices != null) {
            for (WatchGroupDeviceVO device : devices) {
                device.setWATCH_GROUP_ID(groupId);
                watchMapper.insertGroupDevice(device);

                if (device.getInterfaces() != null) {
                    for (WatchGroupIfVO iface : device.getInterfaces()) {
                        iface.setWATCH_GROUP_ID(groupId);
                        iface.setDEVICE_ID(device.getDEVICE_ID());
                        watchMapper.insertGroupInterface(iface);
                    }
                }
            }
        }

        return getGroupDetail(groupId);
    }

    /**
     * 관제 그룹 수정
     */
    @Transactional
    public WatchGroupVO updateGroup(Integer watchGroupId, WatchGroupVO group, List<WatchGroupDeviceVO> devices) {
        WatchGroupVO existing = watchMapper.findGroupById(watchGroupId);
        if (existing == null) {
            throw new IllegalArgumentException("관제 그룹을 찾을 수 없습니다: " + watchGroupId);
        }

        group.setWATCH_GROUP_ID(watchGroupId);
        watchMapper.updateGroup(group);

        // 기존 장비/인터페이스 삭제 후 재등록
        watchMapper.deleteGroupInterfaces(watchGroupId);
        watchMapper.deleteGroupDevices(watchGroupId);

        if (devices != null) {
            for (WatchGroupDeviceVO device : devices) {
                device.setWATCH_GROUP_ID(watchGroupId);
                watchMapper.insertGroupDevice(device);

                if (device.getInterfaces() != null) {
                    for (WatchGroupIfVO iface : device.getInterfaces()) {
                        iface.setWATCH_GROUP_ID(watchGroupId);
                        iface.setDEVICE_ID(device.getDEVICE_ID());
                        watchMapper.insertGroupInterface(iface);
                    }
                }
            }
        }

        return getGroupDetail(watchGroupId);
    }

    /**
     * 관제 그룹 삭제
     */
    @Transactional
    public void deleteGroup(Integer watchGroupId) {
        WatchGroupVO existing = watchMapper.findGroupById(watchGroupId);
        if (existing == null) {
            throw new IllegalArgumentException("관제 그룹을 찾을 수 없습니다: " + watchGroupId);
        }

        // 수집 중지 요청 (실패해도 삭제 진행)
        try {
            stopWatch(watchGroupId);
        } catch (Exception e) {
            log.warn("관제 중지 실패 (무시): {}", e.getMessage());
        }

        watchMapper.deleteGroupInterfaces(watchGroupId);
        watchMapper.deleteGroupDevices(watchGroupId);
        watchMapper.deleteGroup(watchGroupId);
    }

    /**
     * 관제 그룹 이동 (부모 변경)
     */
    @Transactional
    public void moveGroup(Integer watchGroupId, Integer newParentGroupId) {
        WatchGroupVO existing = watchMapper.findGroupById(watchGroupId);
        if (existing == null) {
            throw new IllegalArgumentException("관제 그룹을 찾을 수 없습니다: " + watchGroupId);
        }

        // 새 depth 계산
        int newDepth = 0;
        if (newParentGroupId != null) {
            WatchGroupVO parentGroup = watchMapper.findGroupById(newParentGroupId);
            if (parentGroup == null) {
                throw new IllegalArgumentException("부모 그룹을 찾을 수 없습니다: " + newParentGroupId);
            }
            newDepth = (parentGroup.getDEPTH() != null ? parentGroup.getDEPTH() : 0) + 1;
        }

        // 그룹 이동
        watchMapper.moveGroup(watchGroupId, newParentGroupId, newDepth);

        // 하위 그룹들의 depth도 재계산
        updateDescendantsDepth(watchGroupId, newDepth);
    }

    /**
     * 하위 그룹들의 depth 재계산
     */
    private void updateDescendantsDepth(Integer parentGroupId, int parentDepth) {
        List<WatchGroupVO> descendants = watchMapper.findDescendants(parentGroupId);
        for (WatchGroupVO descendant : descendants) {
            // 부모로부터의 상대적 depth 계산
            int relativeDepth = descendant.getDEPTH() - (parentDepth - 1);
            int newDescendantDepth = parentDepth + relativeDepth;
            watchMapper.moveGroup(descendant.getWATCH_GROUP_ID(), descendant.getPARENT_GROUP_ID(), newDescendantDepth);
        }
    }

    /**
     * 관제 그룹 아이콘 설정
     */
    public void updateGroupIcon(Integer watchGroupId, String iconName) {
        WatchGroupVO existing = watchMapper.findGroupById(watchGroupId);
        if (existing == null) {
            throw new IllegalArgumentException("관제 그룹을 찾을 수 없습니다: " + watchGroupId);
        }
        watchMapper.updateGroupIcon(watchGroupId, iconName);
    }

    /**
     * 하위 그룹 개수 조회
     */
    public Integer getDescendantsCount(Integer watchGroupId) {
        return watchMapper.countDescendants(watchGroupId);
    }

    // ==================== 장비 그룹 연동 ====================

    /**
     * R_GROUP_T 그룹을 관제 그룹으로 가져오기
     * - 선택된 그룹 + 하위 그룹을 R_WATCH_GROUP_T에 LINKED 상태로 생성
     * - 이미 연동된 그룹은 건너뜀
     * - R_GROUP_T의 부모-자식 관계를 PARENT_GROUP_ID로 매핑
     */
    @Transactional
    public List<WatchGroupVO> importFromGroups(List<Integer> groupIds) {
        // 1. 이미 연동된 GROUP_ID 목록 조회
        List<Integer> existingLinkedIds = watchMapper.findAllLinkedGroupIds();

        // 2. 대상 그룹 조회 (이미 연동된 것 제외)
        List<GroupVO> allGroups = groupMapper.findAllGroups();
        Map<Integer, GroupVO> groupMap = new java.util.HashMap<>();
        for (GroupVO g : allGroups) {
            groupMap.put(g.getGROUP_ID(), g);
        }

        // 3. 선택된 그룹 중 미연동 그룹만 필터
        List<Integer> targetIds = new ArrayList<>();
        for (Integer gid : groupIds) {
            if (!existingLinkedIds.contains(gid)) {
                targetIds.add(gid);
            }
        }

        if (targetIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. R_GROUP_T의 부모 -> WATCH_GROUP_T 매핑 (부모-자식 관계 유지)
        // linkedGroupId -> watchGroupId 매핑
        Map<Integer, Integer> linkedToWatchMap = new java.util.HashMap<>();
        // 기존 연동된 그룹의 매핑도 추가
        for (Integer linkedId : existingLinkedIds) {
            WatchGroupVO existing = watchMapper.findByLinkedGroupId(linkedId);
            if (existing != null) {
                linkedToWatchMap.put(linkedId, existing.getWATCH_GROUP_ID());
            }
        }

        // 5. depth 순으로 정렬하여 부모 먼저 생성
        targetIds.sort((a, b) -> {
            GroupVO ga = groupMap.get(a);
            GroupVO gb = groupMap.get(b);
            int depthA = ga != null && ga.getDEPTH() != null ? ga.getDEPTH() : 0;
            int depthB = gb != null && gb.getDEPTH() != null ? gb.getDEPTH() : 0;
            return Integer.compare(depthA, depthB);
        });

        List<WatchGroupVO> created = new ArrayList<>();
        for (Integer gid : targetIds) {
            GroupVO srcGroup = groupMap.get(gid);
            if (srcGroup == null) continue;

            WatchGroupVO watchGroup = new WatchGroupVO();
            watchGroup.setGROUP_NAME(srcGroup.getGROUP_NAME());
            watchGroup.setLINKED_GROUP_ID(gid);
            watchGroup.setINTERVAL_SEC(5);

            // 부모 그룹 매핑
            Integer srcParentId = srcGroup.getPARENT_GROUP_ID();
            if (srcParentId != null && linkedToWatchMap.containsKey(srcParentId)) {
                watchGroup.setPARENT_GROUP_ID(linkedToWatchMap.get(srcParentId));
                // 부모의 depth + 1
                WatchGroupVO parentWatch = watchMapper.findGroupById(linkedToWatchMap.get(srcParentId));
                watchGroup.setDEPTH(parentWatch != null ? (parentWatch.getDEPTH() != null ? parentWatch.getDEPTH() + 1 : 1) : 0);
            } else {
                watchGroup.setPARENT_GROUP_ID(null);
                watchGroup.setDEPTH(0);
            }

            watchMapper.insertGroup(watchGroup);
            linkedToWatchMap.put(gid, watchGroup.getWATCH_GROUP_ID());
            created.add(watchGroup);
        }

        return created;
    }

    /**
     * 연동 해제 (linked watch group 및 하위 연동 그룹 삭제)
     */
    @Transactional
    public void deleteLinkedGroup(Integer linkedGroupId) {
        WatchGroupVO group = watchMapper.findByLinkedGroupId(linkedGroupId);
        if (group != null) {
            // 하위 그룹도 재귀 삭제
            List<WatchGroupVO> descendants = watchMapper.findDescendants(group.getWATCH_GROUP_ID());
            for (WatchGroupVO desc : descendants) {
                watchMapper.deleteGroupInterfaces(desc.getWATCH_GROUP_ID());
                watchMapper.deleteGroupDevices(desc.getWATCH_GROUP_ID());
                watchMapper.deleteGroup(desc.getWATCH_GROUP_ID());
            }
            // 자기 자신 삭제
            watchMapper.deleteGroupInterfaces(group.getWATCH_GROUP_ID());
            watchMapper.deleteGroupDevices(group.getWATCH_GROUP_ID());
            watchMapper.deleteGroup(group.getWATCH_GROUP_ID());
        }
    }

    /**
     * 이미 연동된 GROUP_ID 목록 조회
     */
    public List<Integer> getLinkedGroupIds() {
        return watchMapper.findAllLinkedGroupIds();
    }

    // ==================== Go Middleware 프록시 ====================

    /**
     * 관제 수집 시작 (Go Middleware 호출)
     */
    public void startWatch(Integer watchGroupId) {
        WatchGroupVO group = getGroupDetail(watchGroupId);

        // 요청 데이터 구성
        WatchRequestVO.StartRequest request = new WatchRequestVO.StartRequest();
        request.setGroupId(watchGroupId);
        request.setIntervalSec(group.getINTERVAL_SEC());

        List<WatchRequestVO.DeviceRequest> deviceRequests = new ArrayList<>();
        for (WatchGroupDeviceVO device : group.getDevices()) {
            WatchRequestVO.DeviceRequest dr = new WatchRequestVO.DeviceRequest();
            dr.setDeviceId(device.getDEVICE_ID());

            List<Integer> ifIndexes = new ArrayList<>();
            if (device.getInterfaces() != null) {
                for (WatchGroupIfVO iface : device.getInterfaces()) {
                    ifIndexes.add(iface.getIF_INDEX());
                }
            }
            dr.setIfIndexes(ifIndexes);
            deviceRequests.add(dr);
        }
        request.setDevices(deviceRequests);

        // Go Middleware 호출
        callMiddleware("/api/watch/start", request);
        log.info("[WATCH] 수집 시작 - GroupID: {}", watchGroupId);
    }

    /**
     * 관제 수집 중지 (Go Middleware 호출)
     */
    public void stopWatch(Integer watchGroupId) {
        WatchRequestVO.GroupRequest request = new WatchRequestVO.GroupRequest();
        request.setGroupId(watchGroupId);

        callMiddleware("/api/watch/stop", request);
        log.info("[WATCH] 수집 중지 - GroupID: {}", watchGroupId);
    }

    /**
     * Heartbeat 전송 (Go Middleware 호출)
     */
    public void sendHeartbeat(Integer watchGroupId) {
        WatchRequestVO.GroupRequest request = new WatchRequestVO.GroupRequest();
        request.setGroupId(watchGroupId);

        callMiddleware("/api/watch/heartbeat", request);
    }

    /**
     * Go Middleware HTTP 호출
     */
    private void callMiddleware(String path, Object body) {
        try {
            String url = middlewareUrl + path;
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Middleware API 오류 - status: {}, body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Middleware API 오류: " + response.statusCode());
            }

            // 응답 확인
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
            if (result.get("success") != null && !(Boolean) result.get("success")) {
                throw new RuntimeException("Middleware 처리 실패: " + result.get("message"));
            }

        } catch (Exception e) {
            log.error("Middleware API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Middleware API 호출 실패: " + e.getMessage(), e);
        }
    }

    // ==================== Redis 메트릭 조회 ====================

    /**
     * 최신 메트릭 조회 (Redis)
     */
    public WatchMetricsVO getLatestMetrics(Integer watchGroupId) {
        String key = "metrics:latest:" + watchGroupId;
        try {
            Object data = redisTemplate.opsForValue().get(key);
            if (data == null) {
                return null;
            }
            return objectMapper.convertValue(data, WatchMetricsVO.class);
        } catch (Exception e) {
            log.error("Redis 메트릭 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 히스토리 조회 (Redis)
     */
    public List<WatchHistoryVO> getHistory(Integer watchGroupId, Integer deviceId) {
        String key = "metrics:history:" + watchGroupId + ":" + deviceId;
        try {
            List<Object> data = redisTemplate.opsForList().range(key, 0, 59);
            if (data == null || data.isEmpty()) {
                return new ArrayList<>();
            }

            List<WatchHistoryVO> history = new ArrayList<>();
            for (Object item : data) {
                WatchHistoryVO entry = objectMapper.convertValue(item, WatchHistoryVO.class);
                history.add(entry);
            }
            return history;
        } catch (Exception e) {
            log.error("Redis 히스토리 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
