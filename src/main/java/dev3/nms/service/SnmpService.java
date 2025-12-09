package dev3.nms.service;

import dev3.nms.util.CommonUtil;
import dev3.nms.vo.mgmt.PortVO;
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

@Slf4j
@Service
public class SnmpService {

    // SNMP OID - 시스템 정보
    private static final String OID_SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    private static final String OID_SYS_OBJECT_ID = "1.3.6.1.2.1.1.2.0";
    private static final String OID_SYS_NAME = "1.3.6.1.2.1.1.5.0";

    // SNMP OID - 인터페이스 정보 (IF-MIB)
    private static final String OID_IF_INDEX = "1.3.6.1.2.1.2.2.1.1";
    private static final String OID_IF_DESCR = "1.3.6.1.2.1.2.2.1.2";
    private static final String OID_IF_TYPE = "1.3.6.1.2.1.2.2.1.3";
    private static final String OID_IF_MTU = "1.3.6.1.2.1.2.2.1.4";
    private static final String OID_IF_SPEED = "1.3.6.1.2.1.2.2.1.5";
    private static final String OID_IF_PHYS_ADDRESS = "1.3.6.1.2.1.2.2.1.6";
    private static final String OID_IF_LAST_CHANGE = "1.3.6.1.2.1.2.2.1.9";

    // IF-MIB
    private static final String OID_IF_NAME = "1.3.6.1.2.1.31.1.1.1.1";
    private static final String OID_IF_HIGH_SPEED = "1.3.6.1.2.1.31.1.1.1.15";
    private static final String OID_IF_ALIAS = "1.3.6.1.2.1.31.1.1.1.18";

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
            pdu.add(new VariableBinding(new OID(OID_SYS_DESCR)));
            pdu.add(new VariableBinding(new OID(OID_SYS_OBJECT_ID)));
            pdu.add(new VariableBinding(new OID(OID_SYS_NAME)));
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
            walkTable(snmp, target, snmpVersion, OID_IF_INDEX, (index, value) -> {
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

            // 각 OID 테이블 WALK
            walkTable(snmp, target, snmpVersion, OID_IF_DESCR, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_DESCR(CommonUtil.parseOctetString(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_TYPE, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_TYPE(parseInteger(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_MTU, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_MTU(parseInteger(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_SPEED, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_SPEED(parseLong(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_PHYS_ADDRESS, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_PHYS_ADDRESS(parseMacAddress(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_LAST_CHANGE, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_LAST_CHANGE(parseLong(value));
            });

            // IF-MIB 확장 테이블
            walkTable(snmp, target, snmpVersion, OID_IF_NAME, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_NAME(CommonUtil.parseOctetString(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_HIGH_SPEED, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_HIGH_SPEED(parseInteger(value));
            });

            walkTable(snmp, target, snmpVersion, OID_IF_ALIAS, (index, value) -> {
                PortVO p = portMap.get(index);
                if (p != null) p.setIF_ALIAS(CommonUtil.parseOctetString(value));
            });

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
        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        target.setSecurityName(new OctetString(user));

        // USM (User-based Security Model) 설정
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        // AuthProtocol 설정
        OID authProtocolOID = AuthMD5.ID;
        if (authProtocol != null) {
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
        OID privProtocolOID = PrivDES.ID;
        if (privProtocol != null) {
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

        // UsmUser 추가
        snmp.getUSM().addUser(
                new OctetString(user),
                new UsmUser(
                        new OctetString(user),
                        authProtocolOID,
                        new OctetString(authPassword),
                        privProtocolOID,
                        new OctetString(privPassword)
                )
        );

        return target;
    }
}
