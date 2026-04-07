package dev3.nms.vo.notification;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemEmailVO {
    private Integer EMAIL_ID;
    private String EMAIL_ADDRESS;
    private Boolean ENABLED;
    private LocalDateTime CREATE_AT;
}
