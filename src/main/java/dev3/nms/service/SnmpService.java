package dev3.nms.service;

import dev3.nms.mapper.CommonOidMapper;
import dev3.nms.util.CommonUtil;
import dev3.nms.vo.mgmt.CommonOidVO;
import dev3.nms.vo.mgmt.PortVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnmpService {

    private final CommonOidMapper commonOidMapper;

    // OID Map (DB에서 로드)
    private Map<String, String> oidMap = new HashMap<>();

    // SNMP OID - 시스템 정보 (기본값)
    private static final String DEFAULT_OID_SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    private static final String DEFAULT_OID_SYS_OBJECT_ID = "1.3.6.1.2.1.1.2.0";
    private static final String DEFAULT_OID_SYS_NAME = "1.3.6.1.2.1.1.5.0";

    // OID 이름 상수
    private static final String OID_NAME_IF_INDEX = "IF_INDEX";
    private static final String OID_NAME_IF_DESCR = "IF_DESCR";
    private static final String OID_NAME_IF_TYPE = "IF_TYPE";
    private static final String OID_NAME_IF_MTU = "IF_MTU";
    private static final String OID_NAME_IF_SPEED = "IF_SPEED";
    private static final String OID_NAME_IF_HIGH_SPEED = "IF_HIGH_SPEED";
    private static final String OID_NAME_IF_MAC_ADDRESS = "IF_MAC_ADDRESS";
    private static final String OID_NAME_IF_ADMIN_STATUS = "IF_ADMIN_STATUS";
    private static final String OID_NAME_IF_OPER_STATUS = "IF_OPER_STATUS";
    private static final String OID_NAME_IF_DESCRIPTION = "IF_DESCRIPTION";
    private static final String OID_NAME_IP_ADDRESS_ENTRY = "IP_ADDRESS_ENTRY";
    private static final String OID_NAME_IP_ADDRESS_ENTRY_INDEX = "IP_ADDRESS_ENTRY_INDEX";
    private static final String OID_NAME_IF_ADDRESS_ENTRY_NETMASK = "IF_ADDRESS_ENTRY_NETMASK";

    /**
     * 애플리케이션 시작 시 OID 로드
     */
    @PostConstruct
    public void loadOids() {
        refreshOidMap();
    }

    /**
     * OID Map 갱신 (DB에서 다시 로드)
     */
    public void refreshOidMap() {
        try {
            List<CommonOidVO> oidList = commonOidMapper.findAll();
            oidMap = oidList.stream()
                    .collect(Collectors.toMap(
                            CommonOidVO::getOID_NAME,
                            oid -> oid.getOID().startsWith(".") ? oid.getOID().substring(1) : oid.getOID(),
                            (existing, replacement) -> existing
                    ));
            log.info("OID Map 로드 완료 - {} 개", oidMap.size());
        } catch (Exception e) {
            log.error("OID Map 로드 실패: {}", e.getMessage());
            // 기본 OID로 폴백
            initDefaultOidMap();
        }
    }

    /**
     * 기본 OID Map 초기화 (DB 로드 실패 시 폴백)
     */
    private void initDefaultOidMap() {
        oidMap.put(OID_NAME_IF_INDEX, "1.3.6.1.2.1.2.2.1.1");
        oidMap.put(OID_NAME_IF_DESCR, "1.3.6.1.2.1.2.2.1.2");
        oidMap.put(OID_NAME_IF_TYPE, "1.3.6.1.2.1.2.2.1.3");
        oidMap.put(OID_NAME_IF_MTU, "1.3.6.1.2.1.2.2.1.4");
        oidMap.put(OID_NAME_IF_SPEED, "1.3.6.1.2.1.2.2.1.5");
        oidMap.put(OID_NAME_IF_MAC_ADDRESS, "1.3.6.1.2.1.2.2.1.6");
        oidMap.put(OID_NAME_IF_ADMIN_STATUS, "1.3.6.1.2.1.2.2.1.7");
        oidMap.put(OID_NAME_IF_OPER_STATUS, "1.3.6.1.2.1.2.2.1.8");
        oidMap.put(OID_NAME_IF_HIGH_SPEED, "1.3.6.1.2.1.31.1.1.1.15");
        oidMap.put(OID_NAME_IF_DESCRIPTION, "1.3.6.1.2.1.31.1.1.1.18");
        oidMap.put(OID_NAME_IP_ADDRESS_ENTRY, "1.3.6.1.2.1.4.20.1.1");
        oidMap.put(OID_NAME_IP_ADDRESS_ENTRY_INDEX, "1.3.6.1.2.1.4.20.1.2");
        oidMap.put(OID_NAME_IF_ADDRESS_ENTRY_NETMASK, "1.3.6.1.2.1.4.20.1.3");
        log.warn("기본 OID Map으로 초기화됨");
    }

    /**
     * OID 가져오기 (없으면 null)
     */
    private String getOid(String oidName) {
        return oidMap.get(oidName);
    }

    /**
     * SNMP로 장비 시스템 정보 조회
     */
    public Map<String, String> getDeviceSystemInfo(String ipAddress, int snmpVersion, int snmpPort,
                                                     String community, String user, String authProtocol,
                                                     String authPassword, String privProtocol, String privPassword) {
        Map<String, String> result = new HashMap<>();

        try {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            Target target;
            PDU pdu;

            if (snmpVersion == 3) {
                // SNMPv3
                target = createUserTarget(ipAddress, snmpPort, user, authProtocol, authPassword, privProtocol, privPassword, snmp);
                pdu = new ScopedPDU();
            } else {
                // SNMPv1 or SNMPv2c
                target = createCommunityTarget(ipAddress, snmpPort, community, snmpVersion);
                pdu = new PDU();
            }

            // OID 추가
            pdu.add(new VariableBinding(new OID(DEFAULT_OID_SYS_DESCR)));
            pdu.add(new VariableBinding(new OID(DEFAULT_OID_SYS_OBJECT_ID)));
            pdu.add(new VariableBinding(new OID(DEFAULT_OID_SYS_NAME)));
            pdu.setType(PDU.GET);

            // SNMP GET 요청
            ResponseEvent response = snmp.get(pdu, target);

            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();

                if (responsePDU.getErrorStatus() == PDU.noError) {
                    result.put("sysDescr", responsePDU.get(0).getVariable().toString());
                    result.put("sysObjectId", responsePDU.get(1).getVariable().toString());
                    result.put("sysName", responsePDU.get(2).getVariable().toString());
                } else {
                    log.error("SNMP Error: {}", responsePDU.getErrorStatusText());
                    throw new RuntimeException("SNMP Error: " + responsePDU.getErrorStatusText());
                }
            } else {
                log.error("SNMP Timeout or no response from {}", ipAddress);
                throw new RuntimeException("SNMP Timeout or no response from " + ipAddress);
            }

            snmp.close();

        } catch (IOException e) {
            log.error("SNMP IOException: {}", e.getMessage());
            throw new RuntimeException("SNMP request failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * SNMPv1/v2c CommunityTarget 생성
     */
    private CommunityTarget createCommunityTarget(String ipAddress, int port, String community, int version) {
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ipAddress + "/" + port));
        target.setRetries(2);
        target.setTimeout(1000);

        if (version == 1) {
            target.setVersion(SnmpConstants.version1);
        } else {
            target.setVersion(SnmpConstants.version2c);
        }

        return target;
    }

    /**
     * SNMP로 장비 포트(인터페이스) 정보 조회
     */
    public List<PortVO> getDevicePortInfo(String ipAddress, int snmpVersion, int snmpPort,
                                           String community, String user, String authProtocol,
                                           String authPassword, String privProtocol, String privPassword) {
        List<PortVO> ports = new ArrayList<>();

        try {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            Target target;
            if (snmpVersion == 3) {
                target = createUserTarget(ipAddress, snmpPort, user, authProtocol, authPassword, privProtocol, privPassword, snmp);
            } else {
                target = createCommunityTarget(ipAddress, snmpPort, community, snmpVersion);
            }

            // 인터페이스 인덱스 목록 먼저 수집
            Map<Integer, PortVO> portMap = new LinkedHashMap<>();
            String oidIfIndex = getOid(OID_NAME_IF_INDEX);
            if (oidIfIndex == null) {
                log.error("IF_INDEX OID가 DB에 없습니다.");
                snmp.close();
                return ports;
            }

            walkTable(snmp, target, snmpVersion, oidIfIndex, (index, value) -> {
                int ifIndex = Integer.parseInt(value);
                PortVO port = new PortVO();
                port.setIF_INDEX(ifIndex);
                portMap.put(ifIndex, port);
            });

            if (portMap.isEmpty()) {
                log.warn("No interfaces found on {}", ipAddress);
                snmp.close();
                return ports;
            }

            // 각 OID 테이블 WALK (DB에서 가져온 OID 사용)
            String oidIfDescr = getOid(OID_NAME_IF_DESCR);
            if (oidIfDescr != null) {
                walkTable(snmp, target, snmpVersion, oidIfDescr, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_DESCR(CommonUtil.parseOctetString(value));
                });
            }

            String oidIfType = getOid(OID_NAME_IF_TYPE);
            if (oidIfType != null) {
                walkTable(snmp, target, snmpVersion, oidIfType, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_TYPE(parseInteger(value));
                });
            }

            String oidIfMtu = getOid(OID_NAME_IF_MTU);
            if (oidIfMtu != null) {
                walkTable(snmp, target, snmpVersion, oidIfMtu, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_MTU(parseInteger(value));
                });
            }

            String oidIfSpeed = getOid(OID_NAME_IF_SPEED);
            if (oidIfSpeed != null) {
                walkTable(snmp, target, snmpVersion, oidIfSpeed, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_SPEED(parseLong(value));
                });
            }

            String oidIfMacAddress = getOid(OID_NAME_IF_MAC_ADDRESS);
            if (oidIfMacAddress != null) {
                walkTable(snmp, target, snmpVersion, oidIfMacAddress, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_MAC_ADDRESS(parseMacAddress(value));
                });
            }

            // IF_ADMIN_STATUS 수집
            String oidIfAdminStatus = getOid(OID_NAME_IF_ADMIN_STATUS);
            if (oidIfAdminStatus != null) {
                walkTable(snmp, target, snmpVersion, oidIfAdminStatus, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_ADMIN_STATUS(parseInteger(value));
                });
            }

            // IF_OPER_STATUS 수집
            String oidIfOperStatus = getOid(OID_NAME_IF_OPER_STATUS);
            if (oidIfOperStatus != null) {
                walkTable(snmp, target, snmpVersion, oidIfOperStatus, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_OPER_STATUS(parseInteger(value));
                });
            }

            // IF-MIB 확장 테이블
            String oidIfHighSpeed = getOid(OID_NAME_IF_HIGH_SPEED);
            if (oidIfHighSpeed != null) {
                walkTable(snmp, target, snmpVersion, oidIfHighSpeed, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_HIGH_SPEED(parseInteger(value));
                });
            }

            // IF_DESCRIPTION
            String oidIfDescription = getOid(OID_NAME_IF_DESCRIPTION);
            if (oidIfDescription != null) {
                walkTable(snmp, target, snmpVersion, oidIfDescription, (index, value) -> {
                    PortVO p = portMap.get(index);
                    if (p != null) p.setIF_DESCRIPTION(CommonUtil.parseOctetString(value));
                });
            }

            // IP 주소 정보 수집
            collectIpAddressInfo(snmp, target, snmpVersion, portMap);

            snmp.close();
            ports.addAll(portMap.values());
            log.info("포트 정보 수집 완료 - IP: {}, 포트 수: {}", ipAddress, ports.size());

        } catch (IOException e) {
            log.error("SNMP 포트 조회 실패: {}", e.getMessage());
            throw new RuntimeException("SNMP port query failed: " + e.getMessage());
        }

        return ports;
    }

    /**
     * IP 주소 정보 수집 (ipAddrTable)
     * IP 주소와 해당 인터페이스 인덱스, 서브넷 마스크를 매핑
     */
    private void collectIpAddressInfo(Snmp snmp, Target target, int snmpVersion, Map<Integer, PortVO> portMap) {
        String oidIpAddrEntry = getOid(OID_NAME_IP_ADDRESS_ENTRY);
        String oidIpAddrIndex = getOid(OID_NAME_IP_ADDRESS_ENTRY_INDEX);
        String oidIpAddrNetmask = getOid(OID_NAME_IF_ADDRESS_ENTRY_NETMASK);

        if (oidIpAddrEntry == null || oidIpAddrIndex == null) {
            log.debug("IP 주소 관련 OID가 DB에 없습니다.");
            return;
        }

        // IP 주소 -> 인터페이스 인덱스 매핑
        Map<String, Integer> ipToIfIndex = new HashMap<>();
        // IP 주소 -> 서브넷 마스크 매핑
        Map<String, String> ipToNetmask = new HashMap<>();

        // IP 주소 목록 수집 (OID 마지막 부분이 IP 주소)
        walkIpTable(snmp, target, snmpVersion, oidIpAddrEntry, (ipAddr, value) -> {
            // value는 IP 주소 자체
        });

        // IP 주소 -> ifIndex 매핑 수집
        walkIpTable(snmp, target, snmpVersion, oidIpAddrIndex, (ipAddr, value) -> {
            Integer ifIndex = parseInteger(value);
            if (ifIndex != null) {
                ipToIfIndex.put(ipAddr, ifIndex);
            }
        });

        // IP 주소 -> 서브넷 마스크 매핑 수집
        if (oidIpAddrNetmask != null) {
            walkIpTable(snmp, target, snmpVersion, oidIpAddrNetmask, (ipAddr, value) -> {
                ipToNetmask.put(ipAddr, value);
            });
        }

        // 포트에 IP 주소 정보 설정 (첫 번째 IP 주소만)
        for (Map.Entry<String, Integer> entry : ipToIfIndex.entrySet()) {
            String ipAddr = entry.getKey();
            Integer ifIndex = entry.getValue();
            PortVO port = portMap.get(ifIndex);

            if (port != null && port.getIF_IP_ADDRESS() == null) {
                port.setIF_IP_ADDRESS(ipAddr);
                String netmask = ipToNetmask.get(ipAddr);
                if (netmask != null) {
                    port.setIF_IP_NETMASK(netmask);
                }
            }
        }
    }

    /**
     * IP 테이블 Walk (OID 마지막 부분이 IP 주소인 경우)
     */
    private void walkIpTable(Snmp snmp, Target target, int snmpVersion, String baseOid, IpTableWalkCallback callback) {
        try {
            OID rootOid = new OID(baseOid);
            OID currentOid = rootOid;

            while (true) {
                PDU pdu = (snmpVersion == 3) ? new ScopedPDU() : new PDU();
                pdu.add(new VariableBinding(currentOid));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent response = snmp.getNext(pdu, target);

                if (response == null || response.getResponse() == null) {
                    break;
                }

                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() != PDU.noError) {
                    break;
                }

                VariableBinding vb = responsePDU.get(0);
                OID responseOid = vb.getOid();

                // 다른 OID 트리로 넘어가면 종료
                if (!responseOid.startsWith(rootOid)) {
                    break;
                }

                // IP 주소 추출 (OID의 마지막 4옥텟)
                int oidSize = responseOid.size();
                if (oidSize >= 4) {
                    String ipAddr = responseOid.get(oidSize - 4) + "." +
                                   responseOid.get(oidSize - 3) + "." +
                                   responseOid.get(oidSize - 2) + "." +
                                   responseOid.get(oidSize - 1);
                    String value = vb.getVariable().toString();
                    callback.onValue(ipAddr, value);
                }

                currentOid = responseOid;
            }
        } catch (IOException e) {
            log.error("SNMP IP Table Walk 오류: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface IpTableWalkCallback {
        void onValue(String ipAddress, String value);
    }

    /**
     * SNMP Table Walk 수행
     */
    private void walkTable(Snmp snmp, Target target, int snmpVersion, String baseOid, TableWalkCallback callback) {
        try {
            OID rootOid = new OID(baseOid);
            OID currentOid = rootOid;

            while (true) {
                PDU pdu = (snmpVersion == 3) ? new ScopedPDU() : new PDU();
                pdu.add(new VariableBinding(currentOid));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent response = snmp.getNext(pdu, target);

                if (response == null || response.getResponse() == null) {
                    break;
                }

                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() != PDU.noError) {
                    break;
                }

                VariableBinding vb = responsePDU.get(0);
                OID responseOid = vb.getOid();

                // 다른 OID 트리로 넘어가면 종료
                if (!responseOid.startsWith(rootOid)) {
                    break;
                }

                // 인덱스 추출 (OID의 마지막 부분)
                int index = responseOid.get(responseOid.size() - 1);
                String value = vb.getVariable().toString();

                callback.onValue(index, value);

                currentOid = responseOid;
            }
        } catch (IOException e) {
            log.error("SNMP Walk 오류: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface TableWalkCallback {
        void onValue(int index, String value);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * MAC 주소 파싱 및 형식화
     * SNMP4j OctetString에서 MAC 주소를 추출하여 XX:XX:XX:XX:XX:XX 형식으로 변환
     */
    private String parseMacAddress(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 콜론이나 하이픈 제거하고 순수 헥사만 추출
        String cleanHex = value.replace(":", "").replace("-", "");

        // 이미 출력 가능한 문자열이 아닌 경우 (바이너리 데이터)
        // SNMP4j가 바이너리를 문자열로 변환할 때 특수문자가 포함됨
        if (!cleanHex.matches("^[0-9A-Fa-f]*$")) {
            // 바이트 단위로 헥사 변환
            StringBuilder hexBuilder = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                hexBuilder.append(String.format("%02X", (int) value.charAt(i) & 0xFF));
            }
            cleanHex = hexBuilder.toString();
        }

        // MAC 주소 길이가 아니면 원본 반환
        if (cleanHex.length() != 12) {
            return cleanHex.isEmpty() ? null : value;
        }

        // XX:XX:XX:XX:XX:XX 형식으로 포맷
        return CommonUtil.formatAsMacAddress(cleanHex);
    }

    /**
     * SNMPv3 UserTarget 생성
     */
    private UserTarget createUserTarget(String ipAddress, int port, String user, String authProtocol,
                                         String authPassword, String privProtocol, String privPassword, Snmp snmp) {
        UserTarget target = new UserTarget();
        target.setAddress(new UdpAddress(ipAddress + "/" + port));
        target.setRetries(2);
        target.setTimeout(10000);
        target.setVersion(SnmpConstants.version3);
        target.setSecurityName(new OctetString(user));

        // 보안 레벨 동적 결정
        boolean hasAuth = authProtocol != null && !authProtocol.isEmpty() && authPassword != null && !authPassword.isEmpty();
        boolean hasPriv = privProtocol != null && !privProtocol.isEmpty() && privPassword != null && !privPassword.isEmpty();

        if (hasAuth && hasPriv) {
            target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        } else if (hasAuth) {
            target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
        } else {
            target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
        }

        // USM (User-based Security Model) 설정
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        // AuthProtocol 설정
        OID authProtocolOID = null;
        if (hasAuth) {
            switch (authProtocol.toUpperCase()) {
                case "SHA":
                case "SHA1":
                    authProtocolOID = AuthSHA.ID;
                    break;
                case "SHA224":
                    authProtocolOID = AuthHMAC128SHA224.ID;
                    break;
                case "SHA256":
                    authProtocolOID = AuthHMAC192SHA256.ID;
                    break;
                case "SHA384":
                    authProtocolOID = AuthHMAC256SHA384.ID;
                    break;
                case "SHA512":
                    authProtocolOID = AuthHMAC384SHA512.ID;
                    break;
                case "MD5":
                default:
                    authProtocolOID = AuthMD5.ID;
                    break;
            }
        }

        // PrivProtocol 설정
        OID privProtocolOID = null;
        if (hasPriv) {
            switch (privProtocol.toUpperCase()) {
                case "3DES":
                    privProtocolOID = Priv3DES.ID;
                    break;
                case "AES":
                case "AES128":
                    privProtocolOID = PrivAES128.ID;
                    break;
                case "AES192":
                    privProtocolOID = PrivAES192.ID;
                    break;
                case "AES256":
                    privProtocolOID = PrivAES256.ID;
                    break;
                case "DES":
                default:
                    privProtocolOID = PrivDES.ID;
                    break;
            }
        }

        // UsmUser 추가 (보안 레벨에 따라 다른 파라미터)
        OctetString authPass = hasAuth ? new OctetString(authPassword) : null;
        OctetString privPass = hasPriv ? new OctetString(privPassword) : null;

        snmp.getUSM().addUser(
                new OctetString(user),
                new UsmUser(
                        new OctetString(user),
                        authProtocolOID,
                        authPass,
                        privProtocolOID,
                        privPass
                )
        );

        return target;
    }
}
