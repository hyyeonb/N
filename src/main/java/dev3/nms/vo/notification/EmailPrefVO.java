package dev3.nms.vo.notification;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailPrefVO {
    private Long USER_ID;
    private Boolean EMAIL_ENABLED;
    private Integer DIGEST_MINUTES;
    private LocalDateTime MODIFY_AT;
}
