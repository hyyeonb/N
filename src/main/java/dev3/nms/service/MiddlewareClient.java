package dev3.nms.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Middleware API 클라이언트
 * Go Middleware의 SNMP API를 호출
 */
@Slf4j
@Component
public class MiddlewareClient {

    @Value("${middleware.url:http://localhost:18081}")
    private String middlewareUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MiddlewareClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * SNMP 시스템 정보 조회
     */
    public SystemInfoResponse getSystemInfo(SnmpRequest request) {
        try {
            String url = middlewareUrl + "/api/snmp/system-info";
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), SystemInfoResponse.class);
            } else {
                log.error("Middleware API 오류 - status: {}, body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Middleware API 오류: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Middleware API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Middleware API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SNMP 포트 정보 조회
     */
    public PortsResponse getPorts(SnmpRequest request) {
        try {
            String url = middlewareUrl + "/api/snmp/ports";
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))  // 포트 수집은 시간이 더 걸릴 수 있음
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), PortsResponse.class);
            } else {
                log.error("Middleware API 오류 - status: {}, body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Middleware API 오류: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Middleware API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Middleware API 호출 실패: " + e.getMessage(), e);
        }
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
            throw new RuntimeException("포트 정보 조회 실패: " + response.getMessage());
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

        SystemInfoResponse response = getSystemInfo(request);

        if (!response.isSuccess()) {
            throw new RuntimeException("SNMP 조회 실패: " + response.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("sysDescr", response.getSysDescr());
        result.put("sysObjectId", dev3.nms.util.CommonUtil.normalizeOid(response.getSysObjectId()));
        result.put("sysName", response.getSysName());
        return result;
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
}
