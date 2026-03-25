package dev3.nms.vo.auth;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMasterVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String PAGE_CODE;
    private String PAGE_NAME;
    private String PAGE_GROUP;
    private Integer SORT_ORDER;
    private String PAGE_PATH;
    private LocalDateTime CREATE_AT;
}
