package dev3.nms.util;

import java.nio.charset.StandardCharsets;

/**
 * 공통 유틸리티 클래스
 */
public class CommonUtil {

    /**
     * 헥사 문자열을 바이트 배열로 변환
     */
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }

        // 콜론이나 하이픈 제거
        hexString = hexString.replace(":", "").replace("-", "");

        int length = hexString.length();
        if (length % 2 != 0) {
            return new byte[0];
        }

        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * 헥사 문자열을 일반 문자열로 변환
     * SNMP에서 가져온 OctetString을 사람이 읽을 수 있는 문자열로 변환
     */
    public static String convertHexToString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "";
        }

        // 콜론이나 하이픈 제거
        hex = hex.replace(":", "").replace("-", "");

        if (hex.length() % 2 != 0) {
            return hex; // 유효하지 않은 헥사면 원본 반환
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String s = hex.substring(i, i + 2);
            int n = Integer.valueOf(s, 16);
            builder.append((char) n);
        }

        // ISO-8859-1로 디코딩 후 UTF-8로 변환
        try {
            return new String(builder.toString().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return builder.toString();
        }
    }

    /**
     * 바이트 배열을 MAC 주소 형식(XX:XX:XX:XX:XX:XX)으로 변환
     */
    public static String bytesToMacAddress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * SNMP OctetString 값을 적절하게 파싱
     * MAC 주소인지, 일반 문자열인지 판단하여 변환
     */
    public static String parseOctetString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 콜론으로 구분된 헥사 문자열 패턴인지 먼저 확인 (예: "4d:69:63:72:...")
        if (value.matches("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2})+$")) {
            String cleanHex = value.replace(":", "");

            // MAC 주소 길이(12자, 6바이트)인 경우 MAC 형식으로 반환
            if (cleanHex.length() == 12) {
                return formatAsMacAddress(cleanHex);
            }

            // 그 외에는 문자열로 변환 시도
            String converted = convertHexToString(cleanHex);
            // NULL 문자(00) 제거
            converted = converted.replace("\0", "").trim();
            // 변환된 문자열이 출력 가능한 문자로만 구성되어 있으면 반환
            if (!converted.isEmpty() && converted.matches("^[\\x20-\\x7E]+$")) {
                return converted;
            }
        }

        // 이미 일반 문자열인 경우 (알파벳, 숫자, 공백, 일부 특수문자로만 구성)
        if (value.matches("^[\\x20-\\x7E]+$")) {
            return value;
        }

        // 헥사 문자열 형식인 경우 (예: "001122334455")
        String cleanHex = value.replace(":", "").replace("-", "");
        if (cleanHex.matches("^[0-9A-Fa-f]+$")) {
            // MAC 주소 길이(12자, 6바이트)인 경우 MAC 형식으로 반환
            if (cleanHex.length() == 12) {
                return formatAsMacAddress(cleanHex);
            }
            // 그 외에는 문자열로 변환 시도
            String converted = convertHexToString(cleanHex);
            // NULL 문자(00) 제거
            converted = converted.replace("\0", "").trim();
            // 변환된 문자열이 출력 가능한 문자로만 구성되어 있으면 반환
            if (!converted.isEmpty() && converted.matches("^[\\x20-\\x7E]+$")) {
                return converted;
            }
            // 그렇지 않으면 원본 반환
            return value;
        }

        return value;
    }

    /**
     * 헥사 문자열을 MAC 주소 형식으로 포맷
     */
    public static String formatAsMacAddress(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return null;
        }

        String cleanHex = hexString.replace(":", "").replace("-", "").toUpperCase();
        if (cleanHex.length() != 12) {
            return hexString;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(cleanHex.substring(i, i + 2));
        }
        return sb.toString();
    }

    /**
     * SNMP4j Variable의 값을 문자열로 안전하게 추출
     * OctetString의 경우 헥사 또는 일반 문자열로 적절히 변환
     */
    public static String extractStringValue(Object variable) {
        if (variable == null) {
            return null;
        }

        String value = variable.toString();

        // OctetString 타입의 헥사 표현인 경우 처리
        if (value.startsWith("0x") || value.matches("^[0-9A-Fa-f:]+$")) {
            if (value.startsWith("0x")) {
                value = value.substring(2);
            }
            return parseOctetString(value);
        }

        return value;
    }

    /**
     * OID 정규화 - 앞의 . 제거
     * 입력: .1.3.6.1.4.1.9.9.13.1.4.1.3.1004
     * 출력: 1.3.6.1.4.1.9.9.13.1.4.1.3.1004
     */
    public static String normalizeOid(String oid) {
        if (oid == null || oid.isEmpty()) {
            return oid;
        }
        return oid.startsWith(".") ? oid.substring(1) : oid;
    }

    /**
     * Enterprise OID에서 벤더 BASE_OID 추출
     * 입력: .1.3.6.1.4.1.9.9.13.1.4.1.3.1004
     * 출력: 1.3.6.1.4.1.9
     */
    public static String extractVendorBaseOid(String modelOid) {
        if (modelOid == null || modelOid.isEmpty()) {
            return null;
        }

        // Enterprise OID prefix
        String enterprisePrefix = "1.3.6.1.4.1.";

        // 앞의 . 제거
        String oid = normalizeOid(modelOid);

        if (!oid.startsWith(enterprisePrefix)) {
            return null;
        }

        // 1.3.6.1.4.1. 이후의 문자열에서 첫 번째 숫자(벤더 번호) 추출
        String afterPrefix = oid.substring(enterprisePrefix.length());
        int dotIndex = afterPrefix.indexOf('.');
        String vendorNumber = dotIndex > 0 ? afterPrefix.substring(0, dotIndex) : afterPrefix;

        return enterprisePrefix + vendorNumber;
    }
}
