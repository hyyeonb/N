package dev3.nms.vo.mgmt;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class VendorVO {
    private Integer VENDOR_ID;
    private String VENDOR_NAME;
    private String VENDOR_BASE_OID;
    private Timestamp CREATE_AT;
}
