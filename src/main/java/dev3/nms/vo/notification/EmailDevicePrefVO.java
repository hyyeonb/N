package dev3.nms.vo.notification;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailDevicePrefVO {
    private Long USER_ID;
    private Integer DEVICE_ID;
    private String ALERT_TYPE;
    private String SEVERITY;
    private String DELIVERY_MODE;
}
