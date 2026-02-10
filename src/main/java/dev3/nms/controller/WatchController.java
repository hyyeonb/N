package dev3.nms.controller;

import dev3.nms.service.WatchService;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.watch.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 실시간 성능 감시 컨트롤러
 * - 관제 그룹 CRUD
 * - 관제 시작/중지/Heartbeat
 * - 메트릭 조회 (Redis)
 */
@Slf4j
@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchService watchService;

    // ==================== 관제 그룹 CRUD ====================

    /**
     * 관제 그룹 목록 조회
     */
    @GetMapping("/groups")
    public ResponseEntity<ResVO<List<WatchGroupVO>>> getGroups() {
        try {
            List<WatchGroupVO> groups = watchService.getAllGroups();
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", groups));
        } catch (Exception e) {
            log.error("관제 그룹 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "조회 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 상세 조회 (장비 + 인터페이스 포함)
     */
    @GetMapping("/groups/{watchGroupId}")
    public ResponseEntity<ResVO<WatchGroupVO>> getGroupDetail(@PathVariable Integer watchGroupId) {
        try {
            WatchGroupVO group = watchService.getGroupDetail(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", group));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("관제 그룹 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "조회 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 생성
     * 요청 Body 예시:
     * {
     *   "group": { "GROUP_NAME": "관제그룹1", "INTERVAL_SEC": 60 },
     *   "devices": [
     *     { "DEVICE_ID": 1, "interfaces": [{ "IF_INDEX": 1 }, { "IF_INDEX": 5 }] }
     *   ]
     * }
     */
    @PostMapping("/groups")
    public ResponseEntity<ResVO<WatchGroupVO>> createGroup(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> groupMap = (Map<String, Object>) requestBody.get("group");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deviceMaps = (List<Map<String, Object>>) requestBody.get("devices");

            WatchGroupVO group = new WatchGroupVO();
            group.setGROUP_NAME((String) groupMap.get("GROUP_NAME"));
            group.setINTERVAL_SEC((Integer) groupMap.getOrDefault("INTERVAL_SEC", 60));

            // PARENT_GROUP_ID 처리
            Object parentGroupIdObj = groupMap.get("PARENT_GROUP_ID");
            if (parentGroupIdObj != null) {
                group.setPARENT_GROUP_ID((Integer) parentGroupIdObj);
                // 부모의 DEPTH + 1로 설정
                WatchGroupVO parentGroup = watchService.getGroupDetail(group.getPARENT_GROUP_ID());
                group.setDEPTH(parentGroup.getDEPTH() != null ? parentGroup.getDEPTH() + 1 : 1);
            } else {
                group.setPARENT_GROUP_ID(null);
                group.setDEPTH(0);
            }

            List<WatchGroupDeviceVO> devices = parseDevices(deviceMaps);

            WatchGroupVO created = watchService.createGroup(group, devices);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResVO<>(201, "관제 그룹 생성 성공", created));
        } catch (Exception e) {
            log.error("관제 그룹 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "생성 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 수정
     */
    @PutMapping("/groups/{watchGroupId}")
    public ResponseEntity<ResVO<WatchGroupVO>> updateGroup(
            @PathVariable Integer watchGroupId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> groupMap = (Map<String, Object>) requestBody.get("group");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deviceMaps = (List<Map<String, Object>>) requestBody.get("devices");

            WatchGroupVO group = new WatchGroupVO();
            group.setGROUP_NAME((String) groupMap.get("GROUP_NAME"));
            group.setINTERVAL_SEC((Integer) groupMap.getOrDefault("INTERVAL_SEC", 60));

            // PARENT_GROUP_ID 처리
            Object parentGroupIdObj = groupMap.get("PARENT_GROUP_ID");
            if (parentGroupIdObj != null) {
                group.setPARENT_GROUP_ID((Integer) parentGroupIdObj);
                // 부모의 DEPTH + 1로 설정
                WatchGroupVO parentGroup = watchService.getGroupDetail(group.getPARENT_GROUP_ID());
                group.setDEPTH(parentGroup.getDEPTH() != null ? parentGroup.getDEPTH() + 1 : 1);
            } else {
                group.setPARENT_GROUP_ID(null);
                group.setDEPTH(0);
            }

            List<WatchGroupDeviceVO> devices = parseDevices(deviceMaps);

            WatchGroupVO updated = watchService.updateGroup(watchGroupId, group, devices);
            return ResponseEntity.ok(new ResVO<>(200, "관제 그룹 수정 성공", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("관제 그룹 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "수정 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 삭제
     */
    @DeleteMapping("/groups/{watchGroupId}")
    public ResponseEntity<ResVO<Void>> deleteGroup(@PathVariable Integer watchGroupId) {
        try {
            watchService.deleteGroup(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "관제 그룹 삭제 성공", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("관제 그룹 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "삭제 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 이동 (드래그 앤 드롭)
     */
    @PatchMapping("/groups/{watchGroupId}/move")
    public ResponseEntity<ResVO<Void>> moveGroup(
            @PathVariable Integer watchGroupId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Object parentGroupIdObj = requestBody.get("PARENT_GROUP_ID");
            Integer newParentGroupId = parentGroupIdObj != null ? (Integer) parentGroupIdObj : null;

            watchService.moveGroup(watchGroupId, newParentGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "관제 그룹 이동 성공", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("관제 그룹 이동 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "이동 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 그룹 아이콘 설정
     */
    @PatchMapping("/groups/{watchGroupId}/icon")
    public ResponseEntity<ResVO<Void>> updateGroupIcon(
            @PathVariable Integer watchGroupId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String iconName = (String) requestBody.get("ICON_NAME");
            watchService.updateGroupIcon(watchGroupId, iconName);
            return ResponseEntity.ok(new ResVO<>(200, "아이콘 설정 성공", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("아이콘 설정 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "아이콘 설정 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 하위 그룹 개수 조회
     */
    @GetMapping("/groups/{watchGroupId}/descendants/count")
    public ResponseEntity<ResVO<Integer>> getDescendantsCount(@PathVariable Integer watchGroupId) {
        try {
            Integer count = watchService.getDescendantsCount(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", count));
        } catch (Exception e) {
            log.error("하위 그룹 개수 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "조회 실패: " + e.getMessage(), null));
        }
    }

    // ==================== 관제 시작/중지/Heartbeat ====================

    /**
     * 관제 수집 시작
     */
    @PostMapping("/start/{watchGroupId}")
    public ResponseEntity<ResVO<Void>> startWatch(@PathVariable Integer watchGroupId) {
        try {
            watchService.startWatch(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "관제 수집 시작됨", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResVO<>(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("관제 시작 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "관제 시작 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 관제 수집 중지
     */
    @PostMapping("/stop/{watchGroupId}")
    public ResponseEntity<ResVO<Void>> stopWatch(@PathVariable Integer watchGroupId) {
        try {
            watchService.stopWatch(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "관제 수집 중지됨", null));
        } catch (Exception e) {
            log.error("관제 중지 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "관제 중지 실패: " + e.getMessage(), null));
        }
    }

    /**
     * Heartbeat 전송
     */
    @PostMapping("/heartbeat/{watchGroupId}")
    public ResponseEntity<ResVO<Void>> sendHeartbeat(@PathVariable Integer watchGroupId) {
        try {
            watchService.sendHeartbeat(watchGroupId);
            return ResponseEntity.ok(new ResVO<>(200, "Heartbeat OK", null));
        } catch (Exception e) {
            log.error("Heartbeat 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "Heartbeat 실패: " + e.getMessage(), null));
        }
    }

    // ==================== 메트릭 조회 (Redis) ====================

    /**
     * 최신 메트릭 조회
     */
    @GetMapping("/metrics/{watchGroupId}")
    public ResponseEntity<ResVO<WatchMetricsVO>> getMetrics(@PathVariable Integer watchGroupId) {
        try {
            WatchMetricsVO metrics = watchService.getLatestMetrics(watchGroupId);
            if (metrics == null) {
                return ResponseEntity.ok(new ResVO<>(200, "메트릭 없음", null));
            }
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", metrics));
        } catch (Exception e) {
            log.error("메트릭 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "조회 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 히스토리 조회 (차트용)
     */
    @GetMapping("/history/{watchGroupId}/{deviceId}")
    public ResponseEntity<ResVO<List<WatchHistoryVO>>> getHistory(
            @PathVariable Integer watchGroupId,
            @PathVariable Integer deviceId) {
        try {
            List<WatchHistoryVO> history = watchService.getHistory(watchGroupId, deviceId);
            return ResponseEntity.ok(new ResVO<>(200, "조회 성공", history));
        } catch (Exception e) {
            log.error("히스토리 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResVO<>(500, "조회 실패: " + e.getMessage(), null));
        }
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * 요청 데이터에서 장비/인터페이스 파싱
     */
    @SuppressWarnings("unchecked")
    private List<WatchGroupDeviceVO> parseDevices(List<Map<String, Object>> deviceMaps) {
        if (deviceMaps == null) return null;

        List<WatchGroupDeviceVO> devices = new java.util.ArrayList<>();
        for (Map<String, Object> dm : deviceMaps) {
            WatchGroupDeviceVO device = new WatchGroupDeviceVO();
            device.setDEVICE_ID((Integer) dm.get("DEVICE_ID"));

            List<Map<String, Object>> ifMaps = (List<Map<String, Object>>) dm.get("interfaces");
            if (ifMaps != null) {
                List<WatchGroupIfVO> interfaces = new java.util.ArrayList<>();
                for (Map<String, Object> im : ifMaps) {
                    WatchGroupIfVO iface = new WatchGroupIfVO();
                    iface.setIF_INDEX((Integer) im.get("IF_INDEX"));
                    interfaces.add(iface);
                }
                device.setInterfaces(interfaces);
            }
            devices.add(device);
        }
        return devices;
    }
}
