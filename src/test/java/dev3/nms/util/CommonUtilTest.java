package dev3.nms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilTest {

    @Test
    @DisplayName("hexStringToByteArray - Valid hex string")
    void hexStringToByteArray_ValidHex() {
        byte[] result = CommonUtil.hexStringToByteArray("4D6963726F");
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(0x4D, result[0] & 0xFF);
        assertEquals(0x69, result[1] & 0xFF);
    }

    @Test
    @DisplayName("hexStringToByteArray - With colons")
    void hexStringToByteArray_WithColons() {
        byte[] result = CommonUtil.hexStringToByteArray("4D:69:63:72:6F");
        assertEquals(5, result.length);
    }

    @Test
    @DisplayName("hexStringToByteArray - null/empty")
    void hexStringToByteArray_NullOrEmpty() {
        assertEquals(0, CommonUtil.hexStringToByteArray(null).length);
        assertEquals(0, CommonUtil.hexStringToByteArray("").length);
    }

    @Test
    @DisplayName("hexStringToByteArray - Odd length")
    void hexStringToByteArray_OddLength() {
        assertEquals(0, CommonUtil.hexStringToByteArray("4D6").length);
    }

    @Test
    @DisplayName("convertHexToString - Valid conversion")
    void convertHexToString_ValidHex() {
        String result = CommonUtil.convertHexToString("4D6963726F");
        assertEquals("Micro", result);
    }

    @Test
    @DisplayName("convertHexToString - null/empty")
    void convertHexToString_NullOrEmpty() {
        assertEquals("", CommonUtil.convertHexToString(null));
        assertEquals("", CommonUtil.convertHexToString(""));
    }

    @Test
    @DisplayName("bytesToMacAddress - Valid")
    void bytesToMacAddress_Valid() {
        byte[] bytes = new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        String result = CommonUtil.bytesToMacAddress(bytes);
        assertEquals("00:11:22:33:44:55", result);
    }

    @Test
    @DisplayName("bytesToMacAddress - null/empty")
    void bytesToMacAddress_NullOrEmpty() {
        assertNull(CommonUtil.bytesToMacAddress(null));
        assertNull(CommonUtil.bytesToMacAddress(new byte[0]));
    }

    @Test
    @DisplayName("formatAsMacAddress - Valid format")
    void formatAsMacAddress_Valid() {
        assertEquals("00:11:22:33:44:55", CommonUtil.formatAsMacAddress("001122334455"));
        assertEquals("AA:BB:CC:DD:EE:FF", CommonUtil.formatAsMacAddress("aabbccddeeff"));
    }

    @Test
    @DisplayName("formatAsMacAddress - With colons")
    void formatAsMacAddress_WithColons() {
        assertEquals("00:11:22:33:44:55", CommonUtil.formatAsMacAddress("00:11:22:33:44:55"));
    }

    @Test
    @DisplayName("formatAsMacAddress - Invalid length")
    void formatAsMacAddress_InvalidLength() {
        assertEquals("0011223344", CommonUtil.formatAsMacAddress("0011223344"));
    }

    @Test
    @DisplayName("formatAsMacAddress - null/empty")
    void formatAsMacAddress_NullOrEmpty() {
        assertNull(CommonUtil.formatAsMacAddress(null));
        assertNull(CommonUtil.formatAsMacAddress(""));
    }

    @Test
    @DisplayName("parseOctetString - MAC address hex")
    void parseOctetString_MacAddressHex() {
        assertEquals("00:11:22:33:44:55", CommonUtil.parseOctetString("00:11:22:33:44:55"));
    }

    @Test
    @DisplayName("parseOctetString - Text hex")
    void parseOctetString_TextHex() {
        String result = CommonUtil.parseOctetString("4d:69:63:72:6f");
        assertEquals("Micro", result);
    }

    @Test
    @DisplayName("parseOctetString - Already string")
    void parseOctetString_AlreadyString() {
        assertEquals("Hello World", CommonUtil.parseOctetString("Hello World"));
    }

    @Test
    @DisplayName("parseOctetString - null/empty")
    void parseOctetString_NullOrEmpty() {
        assertNull(CommonUtil.parseOctetString(null));
        assertNull(CommonUtil.parseOctetString(""));
    }

    @Test
    @DisplayName("normalizeOid - Remove leading dot")
    void normalizeOid_RemoveLeadingDot() {
        assertEquals("1.3.6.1.4.1.9", CommonUtil.normalizeOid(".1.3.6.1.4.1.9"));
    }

    @Test
    @DisplayName("normalizeOid - No leading dot")
    void normalizeOid_NoDot() {
        assertEquals("1.3.6.1.4.1.9", CommonUtil.normalizeOid("1.3.6.1.4.1.9"));
    }

    @Test
    @DisplayName("normalizeOid - null/empty")
    void normalizeOid_NullOrEmpty() {
        assertNull(CommonUtil.normalizeOid(null));
        assertEquals("", CommonUtil.normalizeOid(""));
    }

    @Test
    @DisplayName("extractVendorBaseOid - Cisco OID")
    void extractVendorBaseOid_Cisco() {
        String result = CommonUtil.extractVendorBaseOid(".1.3.6.1.4.1.9.1.1745");
        assertEquals("1.3.6.1.4.1.9", result);
    }

    @Test
    @DisplayName("extractVendorBaseOid - HP OID")
    void extractVendorBaseOid_HP() {
        String result = CommonUtil.extractVendorBaseOid(".1.3.6.1.4.1.11.2.3.7.11");
        assertEquals("1.3.6.1.4.1.11", result);
    }

    @Test
    @DisplayName("extractVendorBaseOid - Not enterprise")
    void extractVendorBaseOid_NotEnterprise() {
        assertNull(CommonUtil.extractVendorBaseOid(".1.3.6.1.2.1.1"));
    }

    @Test
    @DisplayName("extractVendorBaseOid - null/empty")
    void extractVendorBaseOid_NullOrEmpty() {
        assertNull(CommonUtil.extractVendorBaseOid(null));
        assertNull(CommonUtil.extractVendorBaseOid(""));
    }

    @Test
    @DisplayName("extractStringValue - Normal string")
    void extractStringValue_NormalString() {
        assertEquals("test", CommonUtil.extractStringValue("test"));
    }

    @Test
    @DisplayName("extractStringValue - 0x hex with colons")
    void extractStringValue_HexWith0x() {
        String result = CommonUtil.extractStringValue("0x74:65:73:74");
        assertEquals("test", result);
    }

    @Test
    @DisplayName("extractStringValue - null")
    void extractStringValue_Null() {
        assertNull(CommonUtil.extractStringValue(null));
    }
}
