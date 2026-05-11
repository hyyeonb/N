package dev3.nms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.MiddlewareMapper;
import dev3.nms.vo.mgmt.MiddlewareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiddlewareService {

    private final MiddlewareMapper middlewareMapper;
    private final DeviceMapper deviceMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== CRUD ====================

    public List<MiddlewareVO> getAll() {
        return middlewareMapper.findAll();
    }

    public MiddlewareVO getById(Integer id) {
        return middlewareMapper.findById(id);
    }

    public MiddlewareVO getByDeviceId(Integer deviceId) {
        return middlewareMapper.findByDeviceId(deviceId);
    }

    public void create(MiddlewareVO mw) {
        if (mw.getSTATUS() == null) {
            mw.setSTATUS("ACTIVE");
        }
        middlewareMapper.insert(mw);
    }

    public void update(Integer id, MiddlewareVO mw) {
        mw.setMIDDLEWARE_ID(id);
        middlewareMapper.update(mw);
    }

    /**
     * 미들웨어 삭제 - 할당된 장비를 나머지 미들웨어에 자동 재할당 후 삭제
     */
    public void delete(Integer id) {
        int deviceCount = middlewareMapper.countDevicesByMiddlewareId(id);

        if (deviceCount > 0) {
            List<MiddlewareVO> remaining = middlewareMapper.findActiveMiddlewares().stream()
                    .filter(m -> !m.getMIDDLEWARE_ID().equals(id))
                    .collect(Collectors.toList());

            if (remaining.isEmpty()) {
                // 남은 미들웨어 없음 → NULL 처리
                deviceMapper.bulkUpdateMiddlewareId(id, null);
                log.info("미들웨어 {} 삭제: {}개 장비 MIDDLEWARE_ID=null (대체 미들웨어 없음)", id, deviceCount);
            } else if (remaining.size() == 1) {
                // 1개 남음 → 전부 이관
                deviceMapper.bulkUpdateMiddlewareId(id, remaining.get(0).getMIDDLEWARE_ID());
                log.info("미들웨어 {} 삭제: {}개 장비 → middlewareId={}", id, deviceCount, remaining.get(0).getMIDDLEWARE_ID());
            } else {
                // N개 남음 → NULL 처리 후 삭제 → 전체 재분배
                deviceMapper.bulkUpdateMiddlewareId(id, null);
                middlewareMapper.delete(id);
                rebalanceAllDevices();
                log.info("미들웨어 {} 삭제: {}개 장비 재분배 시작 ({}개 미들웨어)", id, deviceCount, remaining.size());
                return; // 이미 삭제됨
            }
        }

        middlewareMapper.delete(id);
        log.info("미들웨어 {} 삭제 완료", id);
    }

    // ==================== API Key 등록 ====================

    /**
     * Go 미들웨어 시작 시 API Key 자동 등록
     * @return 할당된 MIDDLEWARE_ID
     */
    public Integer registerKey(int middlewareId, String apiKey, String url) {
        MiddlewareVO mw = null;
        boolean isNew = false;

        if (middlewareId > 0) {
            mw = middlewareMapper.findById(middlewareId);
        } else {
            // URL 기준으로 기존 미들웨어 매칭
            List<MiddlewareVO> all = middlewareMapper.findAll();
            if (all != null && url != null) {
                String normalizedUrl = url.replaceAll("/+$", "");
                mw = all.stream()
                        .filter(m -> m.getMIDDLEWARE_URL() != null
                                && m.getMIDDLEWARE_URL().replaceAll("/+$", "").equals(normalizedUrl))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (mw == null) {
            // 신규 생성 - PRIORITY는 현재 최대값 + 1 (먼저 등록된 미들웨어가 우선)
            int nextPriority = middlewareMapper.findAll().stream()
                    .map(MiddlewareVO::getPRIORITY)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;

            mw = new MiddlewareVO();
            mw.setMIDDLEWARE_NAME("Auto-registered");
            mw.setMIDDLEWARE_URL(url);
            mw.setAPI_KEY(apiKey);
            mw.setSTATUS("ACTIVE");
            mw.setPRIORITY(nextPriority);
            middlewareMapper.insert(mw);
            isNew = true;
            log.info("미들웨어 신규 등록: id={}, url={}, key={}...{}",
                    mw.getMIDDLEWARE_ID(), url, apiKey.substring(0, 8), apiKey.substring(apiKey.length() - 4));
        } else {
            // 기존 갱신
            mw.setAPI_KEY(apiKey);
            if (url != null && !url.isBlank()) {
                mw.setMIDDLEWARE_URL(url);
            }
            mw.setSTATUS("ACTIVE");
            middlewareMapper.update(mw);
            log.info("미들웨어 API Key 갱신: id={}, url={}, key={}...{}",
                    mw.getMIDDLEWARE_ID(), url, apiKey.substring(0, 8), apiKey.substring(apiKey.length() - 4));
        }

        Integer assignedId = mw.getMIDDLEWARE_ID();

        // 신규 등록 시 재분배 트리거
        if (isNew) {
            rebalanceAllDevices();
        }

        return assignedId;
    }

    // ==================== 장비 재분배 ====================

    /**
     * 전체 장비를 활성 미들웨어에 재분배 (ping probe 기반)
     * 비동기 실행 - 호출 즉시 반환
     */
    @Async("auditLogExecutor")
    public void rebalanceAllDevices() {
        try {
            Thread.sleep(1000); // 트랜잭션 커밋 대기
            doRebalance();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("장비 재분배 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 고아 장비(MIDDLEWARE_ID=null, FIXED=0) 자가 치유
     * - 5분마다 실행, 고아 장비 발견 시에만 재분배
     * - 미들웨어 삭제/장애/IP 변경 후 분배 누락된 장비 자동 복구
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void healOrphanDevices() {
        try {
            int orphanCount = deviceMapper.countOrphanDevices();
            if (orphanCount == 0) return;
            log.warn("[자가 치유] 고아 장비 {}건 발견, 재분배 트리거", orphanCount);
            rebalanceAllDevices();
        } catch (Exception e) {
            log.error("[자가 치유] 고아 장비 점검 실패: {}", e.getMessage(), e);
        }
    }

    private void doRebalance() {
        List<MiddlewareVO> actives = middlewareMapper.findActiveMiddlewares();

        // Case 0: 활성 미들웨어 없음
        if (actives.isEmpty()) {
            int cleared = deviceMapper.clearAllMiddlewareAssignments();
            log.info("[재분배] 활성 미들웨어 없음. {}개 장비 MIDDLEWARE_ID=null 처리", cleared);
            return;
        }

        List<Map<String, Object>> devices = deviceMapper.findAllActiveDeviceIdsWithIp();
        if (devices.isEmpty()) {
            log.info("[재분배] 장비 없음, 스킵");
            return;
        }

        // Case 1: 미들웨어 1개 → 전부 할당
        if (actives.size() == 1) {
            Integer mwId = actives.get(0).getMIDDLEWARE_ID();
            List<Integer> allIds = devices.stream()
                    .map(d -> ((Number) d.get("DEVICE_ID")).intValue())
                    .collect(Collectors.toList());
            deviceMapper.bulkReassignDevices(mwId, allIds);
            log.info("[재분배] 미들웨어 1개(id={}). 전체 {}개 장비 할당", mwId, allIds.size());
            return;
        }

        // Case N: ping probe 기반 분배
        log.info("[재분배] {}개 미들웨어, {}개 장비 - ping probe 시작", actives.size(), devices.size());
        rebalanceWithPingProbe(actives, devices);
    }

    /**
     * 각 미들웨어에서 장비로 ping을 보내 도달 가능 여부를 확인한 후 분배
     */
    private void rebalanceWithPingProbe(List<MiddlewareVO> middlewares, List<Map<String, Object>> devices) {
        // 1단계: 각 미들웨어에서 모든 장비로 ping probe (병렬)
        // Map<deviceId, List<reachable middlewareId>>
        Map<Integer, List<Integer>> reachabilityMap = new ConcurrentHashMap<>();
        for (Map<String, Object> dev : devices) {
            reachabilityMap.put(((Number) dev.get("DEVICE_ID")).intValue(), Collections.synchronizedList(new ArrayList<>()));
        }

        List<CompletableFuture<Void>> probeFutures = middlewares.stream()
                .map(mw -> CompletableFuture.runAsync(() -> probeFromMiddleware(mw, devices, reachabilityMap)))
                .collect(Collectors.toList());

        // 전체 probe 완료 대기 (최대 30초)
        try {
            CompletableFuture.allOf(probeFutures.toArray(new CompletableFuture[0]))
                    .get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[재분배] ping probe 타임아웃, 수집된 결과로 진행: {}", e.getMessage());
        }

        // 2단계: 장비별 최적 미들웨어 할당
        Map<Integer, List<Integer>> assignment = new HashMap<>(); // middlewareId → List<deviceId>
        for (MiddlewareVO mw : middlewares) {
            assignment.put(mw.getMIDDLEWARE_ID(), new ArrayList<>());
        }

        // 할당 카운터 (로드 밸런싱용) - FIXED 장비는 이미 할당되어 있으므로 초기 부하로 반영
        Map<Integer, Integer> loadCount = new HashMap<>();
        for (MiddlewareVO mw : middlewares) {
            int fixedLoad = middlewareMapper.countFixedDevicesByMiddlewareId(mw.getMIDDLEWARE_ID());
            loadCount.put(mw.getMIDDLEWARE_ID(), fixedLoad);
            if (fixedLoad > 0) {
                log.info("[재분배] middlewareId={} 초기 부하(FIXED): {}대", mw.getMIDDLEWARE_ID(), fixedLoad);
            }
        }

        for (Map<String, Object> dev : devices) {
            int deviceId = ((Number) dev.get("DEVICE_ID")).intValue();
            List<Integer> reachable = reachabilityMap.getOrDefault(deviceId, Collections.emptyList());

            Integer targetMw;
            if (reachable.isEmpty()) {
                // 아무 미들웨어에서도 도달 불가 → 최소 부하 미들웨어에 할당
                targetMw = getMinLoadMiddleware(loadCount);
            } else if (reachable.size() == 1) {
                targetMw = reachable.get(0);
            } else {
                // 도달 가능 미들웨어 중 최소 부하
                targetMw = reachable.stream()
                        .min(Comparator.comparingInt(loadCount::get))
                        .orElse(reachable.get(0));
            }

            assignment.get(targetMw).add(deviceId);
            loadCount.merge(targetMw, 1, Integer::sum);
        }

        // 3단계: 벌크 UPDATE
        for (Map.Entry<Integer, List<Integer>> entry : assignment.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                deviceMapper.bulkReassignDevices(entry.getKey(), entry.getValue());
                log.info("[재분배] middlewareId={}: {}개 장비 할당", entry.getKey(), entry.getValue().size());
            }
        }

        log.info("[재분배] 완료: {}개 장비 → {}개 미들웨어", devices.size(), middlewares.size());
    }

    /**
     * 특정 미들웨어에서 장비 목록으로 probe 실행
     * - 1단계: ping (기본 도달성)
     * - 2단계: SNMP system-info (실제 수집 가능성)
     * - 둘 다 성공해야 해당 미들웨어에서 장비 수집 가능으로 판정
     */
    private void probeFromMiddleware(MiddlewareVO mw, List<Map<String, Object>> devices,
                                      Map<Integer, List<Integer>> reachabilityMap) {
        String baseUrl = mw.getMIDDLEWARE_URL().replaceAll("/+$", "");
        String apiKey = mw.getAPI_KEY() != null ? mw.getAPI_KEY() : "";

        for (Map<String, Object> dev : devices) {
            int deviceId = ((Number) dev.get("DEVICE_ID")).intValue();
            String ip = (String) dev.get("DEVICE_IP");
            if (ip == null || ip.isBlank()) continue;

            if (!pingProbe(baseUrl, apiKey, ip)) continue;
            if (!snmpProbe(baseUrl, apiKey, dev)) continue;

            reachabilityMap.get(deviceId).add(mw.getMIDDLEWARE_ID());
        }

        log.debug("[재분배] middlewareId={} probe 완료 (ping+SNMP)", mw.getMIDDLEWARE_ID());
    }

    private boolean pingProbe(String baseUrl, String apiKey, String ip) {
        try {
            String body = "{\"ipAddress\":\"" + ip + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/check/ping"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;
            Map<?, ?> result = objectMapper.readValue(resp.body(), Map.class);
            return Boolean.TRUE.equals(result.get("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean snmpProbe(String baseUrl, String apiKey, Map<String, Object> dev) {
        Object verObj = dev.get("SNMP_VERSION");
        if (verObj == null) return false; // SNMP 크리덴셜 없으면 수집 불가로 판정

        Map<String, Object> body = new HashMap<>();
        body.put("ipAddress", dev.get("DEVICE_IP"));
        body.put("snmpVersion", ((Number) verObj).intValue());
        Object portObj = dev.get("SNMP_PORT");
        body.put("snmpPort", portObj != null ? ((Number) portObj).intValue() : 161);
        body.put("community", dev.get("SNMP_COMMUNITY"));
        body.put("user", dev.get("SNMP_USER"));
        body.put("authProtocol", dev.get("SNMP_AUTH_PROTOCOL"));
        body.put("authPassword", dev.get("SNMP_AUTH_PASSWORD"));
        body.put("privProtocol", dev.get("SNMP_PRIV_PROTOCOL"));
        body.put("privPassword", dev.get("SNMP_PRIV_PASSWORD"));

        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/snmp/system-info"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;
            Map<?, ?> result = objectMapper.readValue(resp.body(), Map.class);
            return Boolean.TRUE.equals(result.get("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private Integer getMinLoadMiddleware(Map<Integer, Integer> loadCount) {
        return loadCount.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ==================== 헬스체크 ====================

    /**
     * 미들웨어 헬스체크 - GET {url}/health 호출 후 LAST_HEARTBEAT 갱신
     */
    public MiddlewareVO healthCheck(Integer id) {
        MiddlewareVO mw = middlewareMapper.findById(id);
        if (mw == null) {
            throw new RuntimeException("미들웨어를 찾을 수 없습니다: " + id);
        }

        try {
            String healthUrl = mw.getMIDDLEWARE_URL() + "/health";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .header("X-API-Key", mw.getAPI_KEY() != null ? mw.getAPI_KEY() : "")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                middlewareMapper.updateHeartbeat(id);
                mw.setSTATUS("ACTIVE");
                mw.setLAST_HEARTBEAT(java.time.LocalDateTime.now());
                log.info("미들웨어 헬스체크 성공 - id: {}, url: {}", id, mw.getMIDDLEWARE_URL());
            } else {
                mw.setSTATUS("DOWN");
                log.warn("미들웨어 헬스체크 실패 - id: {}, status: {}", id, response.statusCode());
            }
        } catch (Exception e) {
            mw.setSTATUS("DOWN");
            log.error("미들웨어 헬스체크 오류 - id: {}, error: {}", id, e.getMessage());
        }

        middlewareMapper.update(mw);
        return middlewareMapper.findById(id);
    }
}
