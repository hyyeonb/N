package dev3.nms.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.mapper.MiddlewareMapper;
import dev3.nms.vo.mgmt.MiddlewareVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Middleware API 클라이언트
 * Go Middleware의 SNMP API를 호출
 * - DB에서 미들웨어 URL/API Key를 동적 조회 (장비별 또는 기본)
 * - application.properties의 middleware.url은 fallback으로 사용
 */
@Slf4j
@Component
public class MiddlewareClient {

    @Value("${middleware.url:http://localhost:18081}")
    private String middlewareUrl;

    @Value("${middleware.api-key:}")
    private String defaultApiKey;

    @Autowired(required = false)
    private MiddlewareMapper middlewareMapper;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public MiddlewareClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ========== Middleware Resolution ==========

    /**
     * 장비 ID로 할당된 미들웨어를 조회, 없으면 첫 번째 ACTIVE 미들웨어, 그것도 없으면 properties fallback
     */
    private MiddlewareVO resolveMiddleware(Integer deviceId) {
        if (middlewareMapper == null) {
            return null;
        }

        try {
            // 1. 장비에 할당된 미들웨어 조회
            if (deviceId != null) {
                MiddlewareVO mw = middlewareMapper.findByDeviceId(deviceId);
                if (mw != null) {
                    return mw;
                }
            }

            // 2. 첫 번째 ACTIVE 미들웨어 조회 (PRIORITY 순)
            List<MiddlewareVO> all = middlewareMapper.findAll();
            if (all != null && !all.isEmpty()) {
                return all.stream()
                        .filter(m -> "ACTIVE".equals(m.getSTATUS()))
                        .findFirst()
                        .orElse(all.get(0));
            }
        } catch (Exception e) {
            log.warn("미들웨어 조회 실패, fallback 사용: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 미들웨어 URL 결정 (DB 우선, fallback: properties)
     */
    private String resolveUrl(MiddlewareVO mw) {
        if (mw != null && mw.getMIDDLEWARE_URL() != null && !mw.getMIDDLEWARE_URL().isEmpty()) {
            return mw.getMIDDLEWARE_URL();
        }
        return middlewareUrl;
    }

    /**
     * API Key 결정 (DB 우선, fallback: properties)
     */
    private String resolveApiKey(MiddlewareVO mw) {
        if (mw != null && mw.getAPI_KEY() != null && !mw.getAPI_KEY().isEmpty()) {
            return mw.getAPI_KEY();
        }
        return defaultApiKey;
    }

    // ========== Circuit Breaker ==========

    /**
     * Circuit Breaker 상태 관리
     * - failCount >= 3 이면 OPEN (30초간 요청 차단)
     * - 30초 경과 후 HALF-OPEN (1회 시도 허용)
     * - 성공 시 CLOSED로 복귀
     */
    public static class CircuitState {
        private static final int FAIL_THRESHOLD = 3;
        private static final long OPEN_DURATION_MS = 30_000;

        private final AtomicInteger failCount = new AtomicInteger(0);
        private volatile long openedAt = 0;

        public boolean isOpen() {
            return failCount.get() >= FAIL_THRESHOLD
                    && (System.currentTimeMillis() - openedAt) < OPEN_DURATION_MS;
        }

        public boolean isHalfOpen() {
            return failCount.get() >= FAIL_THRESHOLD
                    && (System.currentTimeMillis() - openedAt) >= OPEN_DURATION_MS;
        }

        public void recordSuccess() {
            failCount.set(0);
        }

        public void recordFailure() {
            int count = failCount.incrementAndGet();
            if (count >= FAIL_THRESHOLD) {
                openedAt = System.currentTimeMillis();
            }
        }
    }

    /**
     * Circuit Breaker로 HTTP 호출을 감싸는 헬퍼
     */
    private <T> T executeWithCircuitBreaker(String middlewareUrl, String path, Supplier<T> request) {
        CircuitState state = circuits.computeIfAbsent(middlewareUrl, k -> new CircuitState());

        if (state.isOpen()) {
            log.warn("Circuit OPEN - 요청 차단: {} {}", middlewareUrl, path);
            throw new RuntimeException("Middleware 연결 차단 중 (Circuit OPEN): " + middlewareUrl);
        }

        if (state.isHalfOpen()) {
            log.info("Circuit HALF-OPEN - 재시도 허용: {} {}", middlewareUrl, path);
        }

        try {
            T result = request.get();
            state.recordSuccess();
            return result;
        } catch (Exception e) {
            state.recordFailure();
            log.warn("Circuit Breaker 실패 기록 - url: {}, failCount: {}", middlewareUrl, state.failCount.get());
            throw e;
        }
    }

    // ========== API Methods ==========

    /**
     * SNMP 시스템 정보 조회
     */
    public SystemInfoResponse getSystemInfo(SnmpRequest request) {
        return getSystemInfo(request, null);
    }

    /**
     * SNMP 시스템 정보 조회 (미들웨어 URL/API Key 직접 지정)
     */
    public SystemInfoResponse getSystemInfo(SnmpRequest request, String middlewareUrl, String apiKey) {
        return executeWithCircuitBreaker(middlewareUrl, "/api/snmp/system-info", () -> {
            try {
                String url = middlewareUrl + "/api/snmp/system-info";
                String requestBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey != null ? apiKey : "")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), SystemInfoResponse.class);
                } else {
                    log.error("Middleware API 오류 - status: {}, body: {}", response.statusCode(), response.body());
                    throw new RuntimeException("SNMP 실패: 미들웨어 통신 오류 (" + response.statusCode() + ")");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("Middleware API 호출 실패: {}", e.getMessage());
                throw new RuntimeException("SNMP 실패: 미들웨어 연결 실패 (에이전트가 실행 중인지 확인하세요)", e);
            }
        });
    }

    /**
     * SNMP 시스템 정보 조회 (장비 ID 기반 미들웨어 자동 조회)
     */
    public SystemInfoResponse getSystemInfo(SnmpRequest request, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        return getSystemInfo(request, resolveUrl(mw), resolveApiKey(mw));
    }

    /**
     * SNMP 포트 정보 조회
     */
    public PortsResponse getPorts(SnmpRequest request) {
        return getPorts(request, (Integer) null);
    }

    /**
     * SNMP 포트 정보 조회 (미들웨어 URL/API Key 직접 지정)
     */
    public PortsResponse getPorts(SnmpRequest request, String middlewareUrl, String apiKey) {
        return executeWithCircuitBreaker(middlewareUrl, "/api/snmp/ports", () -> {
            try {
                String url = middlewareUrl + "/api/snmp/ports";
                String requestBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey != null ? apiKey : "")
                        .timeout(Duration.ofSeconds(60))  // 포트 수집은 시간이 더 걸릴 수 있음
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), PortsResponse.class);
                } else {
                    log.error("Middleware API 오류 - status: {}, body: {}", response.statusCode(), response.body());
                    throw new RuntimeException("SNMP 실패: 미들웨어 통신 오류 (" + response.statusCode() + ")");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("Middleware API 호출 실패: {}", e.getMessage());
                throw new RuntimeException("SNMP 실패: 미들웨어 연결 실패 (에이전트가 실행 중인지 확인하세요)", e);
            }
        });
    }

    /**
     * SNMP 포트 정보 조회 (장비 ID 기반 미들웨어 자동 조회)
     */
    public PortsResponse getPorts(SnmpRequest request, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        return getPorts(request, resolveUrl(mw), resolveApiKey(mw));
    }

    /**
     * 편의 메서드 - PortVO 리스트로 포트 정보 반환 (기존 SnmpService 호환)
     */
    public List<dev3.nms.vo.mgmt.PortVO> getDevicePortInfo(String ipAddress, int snmpVersion, int snmpPort,
                                                            String community, String user, String authProtocol,
                                                            String authPassword, String privProtocol, String privPassword) {
        SnmpRequest request = new SnmpRequest();
        request.setIpAddress(ipAddress);
        request.setSnmpVersion(snmpVersion);
        request.setSnmpPort(snmpPort);
        request.setCommunity(community);
        request.setUser(user);
        request.setAuthProtocol(authProtocol);
        request.setAuthPassword(authPassword);
        request.setPrivProtocol(privProtocol);
        request.setPrivPassword(privPassword);

        PortsResponse response = getPorts(request);

        if (!response.isSuccess()) {
            throw new RuntimeException(toSnmpUserMessage(response.getMessage()));
        }

        // PortInfo -> PortVO 변환
        return response.getPorts().stream()
                .map(this::toPortVO)
                .toList();
    }

    /**
     * PortInfo -> PortVO 변환
     */
    private dev3.nms.vo.mgmt.PortVO toPortVO(PortInfo info) {
        dev3.nms.vo.mgmt.PortVO vo = new dev3.nms.vo.mgmt.PortVO();
        vo.setIF_INDEX(info.getIfIndex());
        vo.setIF_DESCR(info.getIfDescr());
        vo.setIF_TYPE(info.getIfType());
        vo.setIF_MTU(info.getIfMtu());
        vo.setIF_SPEED(info.getIfSpeed());
        vo.setIF_HIGH_SPEED(info.getIfHighSpeed());
        vo.setIF_MAC_ADDRESS(info.getIfMacAddress());
        // 신규 등록 시 AdminStatus/OperStatus는 무조건 ON(1)으로 설정
        vo.setIF_ADMIN_STATUS(1);
        vo.setIF_OPER_STATUS(1);
        vo.setIF_DESCRIPTION(info.getIfAlias());
        vo.setIF_IP_ADDRESS(info.getIfIpAddress());
        vo.setIF_IP_NETMASK(info.getIfIpNetmask());
        return vo;
    }

    /**
     * 편의 메서드 - Map 형식으로 시스템 정보 반환 (기존 SnmpService 호환)
     */
    public Map<String, String> getDeviceSystemInfo(String ipAddress, int snmpVersion, int snmpPort,
                                                    String community, String user, String authProtocol,
                                                    String authPassword, String privProtocol, String privPassword) {
        return getDeviceSystemInfo(ipAddress, snmpVersion, snmpPort, community, user, authProtocol, authPassword, privProtocol, privPassword, null);
    }

    public Map<String, String> getDeviceSystemInfo(String ipAddress, int snmpVersion, int snmpPort,
                                                    String community, String user, String authProtocol,
                                                    String authPassword, String privProtocol, String privPassword,
                                                    Integer deviceId) {
        SnmpRequest request = new SnmpRequest();
        request.setIpAddress(ipAddress);
        request.setSnmpVersion(snmpVersion);
        request.setSnmpPort(snmpPort);
        request.setCommunity(community);
        request.setUser(user);
        request.setAuthProtocol(authProtocol);
        request.setAuthPassword(authPassword);
        request.setPrivProtocol(privProtocol);
        request.setPrivPassword(privPassword);

        SystemInfoResponse response = getSystemInfo(request, deviceId);

        if (!response.isSuccess()) {
            throw new RuntimeException(toSnmpUserMessage(response.getMessage()));
        }

        Map<String, String> result = new HashMap<>();
        result.put("sysDescr", response.getSysDescr());
        result.put("sysObjectId", dev3.nms.util.CommonUtil.normalizeOid(response.getSysObjectId()));
        result.put("sysName", response.getSysName());
        return result;
    }

    /**
     * PING 체크 (Go Middleware /api/check/ping)
     */
    public PingResponse pingCheck(String ipAddress) {
        return pingCheck(ipAddress, (Integer) null);
    }

    /**
     * PING 체크 (장비 ID 기반 미들웨어 자동 조회)
     */
    public PingResponse pingCheck(String ipAddress, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        String targetUrl = resolveUrl(mw);
        String apiKey = resolveApiKey(mw);

        try {
            return executeWithCircuitBreaker(targetUrl, "/api/check/ping", () -> {
                try {
                    String url = targetUrl + "/api/check/ping";
                    Map<String, String> body = Map.of("ipAddress", ipAddress);
                    String requestBody = objectMapper.writeValueAsString(body);

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("X-API-Key", apiKey != null ? apiKey : "")
                            .timeout(Duration.ofSeconds(3))
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    return objectMapper.readValue(response.body(), PingResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("PING 체크 실패 - IP: {}, error: {}", ipAddress, e.getMessage());
            PingResponse fail = new PingResponse();
            fail.setSuccess(false);
            fail.setMessage("Middleware 호출 실패: " + e.getMessage());
            return fail;
        }
    }

    /**
     * SSH 포트 체크 (Go Middleware /api/check/ssh)
     */
    public SshCheckResponse sshCheck(String ipAddress, int port) {
        return sshCheck(ipAddress, port, null);
    }

    /**
     * SSH 포트 체크 (장비 ID 기반 미들웨어 자동 조회)
     */
    public SshCheckResponse sshCheck(String ipAddress, int port, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        String targetUrl = resolveUrl(mw);
        String apiKey = resolveApiKey(mw);

        try {
            return executeWithCircuitBreaker(targetUrl, "/api/check/ssh", () -> {
                try {
                    String url = targetUrl + "/api/check/ssh";
                    Map<String, Object> body = Map.of("ipAddress", ipAddress, "port", port);
                    String requestBody = objectMapper.writeValueAsString(body);

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("X-API-Key", apiKey != null ? apiKey : "")
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    return objectMapper.readValue(response.body(), SshCheckResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("SSH 체크 실패 - IP: {}:{}, error: {}", ipAddress, port, e.getMessage());
            SshCheckResponse fail = new SshCheckResponse();
            fail.setSuccess(false);
            fail.setPort(port);
            fail.setMessage("Middleware 호출 실패: " + e.getMessage());
            return fail;
        }
    }

    /**
     * 특정 포트 상태 체크 (AdminStatus / OperStatus 실시간 SNMP 조회)
     */
    public PortStatusResponse getPortStatus(SnmpRequest snmpRequest, int ifIndex) {
        return getPortStatus(snmpRequest, ifIndex, null);
    }

    /**
     * 특정 포트 상태 체크 (장비 ID 기반 미들웨어 자동 조회)
     */
    public PortStatusResponse getPortStatus(SnmpRequest snmpRequest, int ifIndex, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        String targetUrl = resolveUrl(mw);
        String apiKey = resolveApiKey(mw);

        try {
            return executeWithCircuitBreaker(targetUrl, "/api/snmp/port-status", () -> {
                try {
                    String url = targetUrl + "/api/snmp/port-status";
                    Map<String, Object> body = new HashMap<>();
                    body.put("ipAddress", snmpRequest.getIpAddress());
                    body.put("snmpVersion", snmpRequest.getSnmpVersion());
                    body.put("snmpPort", snmpRequest.getSnmpPort());
                    body.put("community", snmpRequest.getCommunity());
                    body.put("user", snmpRequest.getUser());
                    body.put("authProtocol", snmpRequest.getAuthProtocol());
                    body.put("authPassword", snmpRequest.getAuthPassword());
                    body.put("privProtocol", snmpRequest.getPrivProtocol());
                    body.put("privPassword", snmpRequest.getPrivPassword());
                    body.put("ifIndex", ifIndex);

                    String requestBody = objectMapper.writeValueAsString(body);

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("X-API-Key", apiKey != null ? apiKey : "")
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    return objectMapper.readValue(response.body(), PortStatusResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("포트 상태 체크 실패 - ifIndex: {}, error: {}", ifIndex, e.getMessage());
            PortStatusResponse fail = new PortStatusResponse();
            fail.setSuccess(false);
            fail.setIfIndex(ifIndex);
            fail.setMessage("Middleware 호출 실패: " + e.getMessage());
            return fail;
        }
    }

    /**
     * SNMP 원문 에러를 사용자 친화적 한국어 메시지로 변환 (Go Middleware 방어 레이어)
     */
    private String toSnmpUserMessage(String msg) {
        if (msg == null || msg.isEmpty()) return "SNMP 통신 실패";
        // 이미 한글 번역된 메시지면 그대로 반환
        if (msg.startsWith("SNMP 실패")) return msg;

        String low = msg.toLowerCase();

        if (low.contains("timeout") || low.contains("request timeout") || low.contains("i/o timeout")) {
            return "SNMP 실패 (타임아웃): 장비가 응답하지 않습니다";
        }
        if (low.contains("connection refused")) {
            return "SNMP 실패 (연결 거부): SNMP 포트가 열려 있는지 확인하세요";
        }
        if (low.contains("no route to host") || low.contains("network is unreachable")) {
            return "SNMP 실패 (네트워크 도달 불가): 장비 IP 또는 네트워크 경로를 확인하세요";
        }
        if (low.contains("unknown user name") || low.contains("usmstatsunknownusernames")) {
            return "SNMP 실패 (인증 오류): SNMPv3 사용자명이 올바르지 않습니다";
        }
        if (low.contains("wrong digest") || low.contains("authentication failure") || low.contains("usmstatswrongdigests")) {
            return "SNMP 실패 (인증 오류): SNMPv3 인증 비밀번호 또는 프로토콜이 올바르지 않습니다";
        }
        if (low.contains("decryption error") || low.contains("usmstatsdecryptionerrors")) {
            return "SNMP 실패 (암호화 오류): SNMPv3 암호화 비밀번호 또는 프로토콜이 올바르지 않습니다";
        }
        if (low.contains("no such object") || low.contains("no such instance")) {
            return "SNMP 실패 (OID 오류): 요청한 OID를 장비에서 지원하지 않습니다";
        }
        if (low.contains("community") || low.contains("authorization error")) {
            return "SNMP 실패 (인증 오류): Community 문자열이 올바르지 않습니다";
        }
        // 미들웨어 연결 자체 실패
        if (low.contains("미들웨어") || low.contains("middleware")) {
            return msg; // 이미 한글이거나 미들웨어 관련 메시지는 그대로
        }

        return "SNMP 실패: " + msg;
    }

    // ========== Request/Response DTOs ==========

    @Data
    public static class SnmpRequest {
        private String ipAddress;
        private int snmpVersion;
        private int snmpPort = 161;
        private String community;
        private String user;
        private String authProtocol;
        private String authPassword;
        private String privProtocol;
        private String privPassword;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemInfoResponse {
        private boolean success;
        private String message;
        private String sysDescr;
        private String sysObjectId;
        private String sysName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortsResponse {
        private boolean success;
        private String message;
        private List<PortInfo> ports;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortInfo {
        private int ifIndex;
        private String ifDescr;
        private int ifType;
        private int ifMtu;
        private long ifSpeed;
        private int ifHighSpeed;
        private String ifMacAddress;
        private int ifAdminStatus;
        private int ifOperStatus;
        private String ifAlias;
        private String ifIpAddress;
        private String ifIpNetmask;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PingResponse {
        private boolean success;
        private String message;
        private double responseTimeMs;
        private double minTimeMs;
        private double maxTimeMs;
        private double packetLoss;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SshCheckResponse {
        private boolean success;
        private String message;
        private int port;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortStatusResponse {
        private boolean success;
        private String message;
        private int ifIndex;
        private int adminStatus;
        private int operStatus;
        private String adminStatusText;
        private String operStatusText;
    }

    /**
     * Traceroute 실행 - 로컬 (Middleware 서버에서 직접)
     */
    public TracerouteResponse traceroute(String targetIp, int maxHops, int timeoutMs) {
        return traceroute(targetIp, maxHops, timeoutMs, null);
    }

    /**
     * Traceroute 실행 (장비 ID 기반 미들웨어 자동 조회)
     */
    public TracerouteResponse traceroute(String targetIp, int maxHops, int timeoutMs, Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        String targetUrl = resolveUrl(mw);
        String apiKey = resolveApiKey(mw);

        try {
            return executeWithCircuitBreaker(targetUrl, "/api/check/traceroute", () -> {
                try {
                    String url = targetUrl + "/api/check/traceroute";
                    Map<String, Object> body = Map.of("targetIp", targetIp, "maxHops", maxHops, "timeout", timeoutMs);
                    String requestBody = objectMapper.writeValueAsString(body);

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("X-API-Key", apiKey != null ? apiKey : "")
                            .timeout(Duration.ofSeconds(120))  // traceroute는 최대 2분 허용
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    return objectMapper.readValue(response.body(), TracerouteResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("Traceroute 실패 - target: {}, error: {}", targetIp, e.getMessage());
            TracerouteResponse fail = new TracerouteResponse();
            fail.setSuccess(false);
            fail.setMessage("Middleware 호출 실패: " + e.getMessage());
            return fail;
        }
    }

    /**
     * Traceroute 실행 - SSH 원격 (출발 장비에서 traceroute 수행)
     */
    public TracerouteResponse tracerouteFromDevice(String sourceIp, int sshPort,
                                                    String sshUser, String sshPass,
                                                    String targetIp, int maxHops, int timeoutMs) {
        return tracerouteFromDevice(sourceIp, sshPort, sshUser, sshPass, targetIp, maxHops, timeoutMs, null);
    }

    /**
     * Traceroute 실행 - SSH 원격 (장비 ID 기반 미들웨어 자동 조회)
     */
    public TracerouteResponse tracerouteFromDevice(String sourceIp, int sshPort,
                                                    String sshUser, String sshPass,
                                                    String targetIp, int maxHops, int timeoutMs,
                                                    Integer deviceId) {
        MiddlewareVO mw = resolveMiddleware(deviceId);
        String targetUrl = resolveUrl(mw);
        String apiKey = resolveApiKey(mw);

        try {
            return executeWithCircuitBreaker(targetUrl, "/api/check/traceroute", () -> {
                try {
                    String url = targetUrl + "/api/check/traceroute";
                    Map<String, Object> body = new HashMap<>();
                    body.put("targetIp", targetIp);
                    body.put("sourceIp", sourceIp);
                    body.put("sshPort", sshPort);
                    body.put("sshUser", sshUser);
                    body.put("sshPass", sshPass);
                    body.put("maxHops", maxHops);
                    body.put("timeout", timeoutMs);

                    String requestBody = objectMapper.writeValueAsString(body);
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("X-API-Key", apiKey != null ? apiKey : "")
                            .timeout(Duration.ofSeconds(120))
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    return objectMapper.readValue(response.body(), TracerouteResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("Traceroute (SSH) 실패 - source: {}, target: {}, error: {}", sourceIp, targetIp, e.getMessage());
            TracerouteResponse fail = new TracerouteResponse();
            fail.setSuccess(false);
            fail.setMessage("Middleware 호출 실패: " + e.getMessage());
            return fail;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HopInfo {
        private int hopNumber;
        private String ip;
        private List<String> rtts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracerouteResponse {
        private boolean success;
        private String message;
        private String targetIp;
        private List<HopInfo> hops;
    }
}
