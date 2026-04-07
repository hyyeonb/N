package dev3.nms.vo.notification;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailTypePrefVO {
    private Long USER_ID;
    private String ALERT_TYPE;
    private String SEVERITY;
    private String DELIVERY_MODE; // IMMEDIATE, DIGEST, OFF
}
