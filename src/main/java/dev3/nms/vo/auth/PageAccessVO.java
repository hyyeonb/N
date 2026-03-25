package dev3.nms.vo.auth;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageAccessVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long USER_ID;
    private String PAGE_CODE;
    private Boolean CAN_VIEW;
    private Boolean CAN_EDIT;
    private Long MODIFY_USER_ID;
    private LocalDateTime MODIFY_AT;

    // JOIN fields
    private String PAGE_NAME;
    private String PAGE_GROUP;
    private Integer SORT_ORDER;
    private String PAGE_PATH;
}
