package dev3.nms.vo.fault;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 장애 이력 VO (f_error_history_t)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorHistoryVO {
    private Long ERROR_HISTORY_ID;
    private Long ERROR_ID;
    private Long DEVICE_ID;
    private Long ERROR_CODE_ID;
    private String ERROR_MESSAGE;
    private String ERROR_LEVEL;      // C / M / N / W
    private LocalDateTime OCCUR_AT;
    private LocalDateTime CLEAR_AT;
    private String USER_MESSAGE;
    private LocalDateTime UPDATE_AT;
    private String DEVICE_IP;

    // JOIN 필드
    private String DEVICE_NAME;
    private String ERROR_CODE_NAME;
    private String GROUP_NAME;
}
