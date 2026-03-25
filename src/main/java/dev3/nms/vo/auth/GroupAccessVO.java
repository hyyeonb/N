package dev3.nms.vo.auth;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAccessVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long USER_ID;
    private String GROUP_TYPE;  // ASSET or WATCH
    private Long GROUP_ID;
    private Boolean CAN_VIEW;
    private Boolean CAN_EDIT;
    private Long MODIFY_USER_ID;
    private LocalDateTime MODIFY_AT;

    // JOIN fields
    private String GROUP_NAME;
}
