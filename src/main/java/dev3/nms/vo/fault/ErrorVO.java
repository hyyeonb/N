package dev3.nms.vo.fault;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실시간 장애 VO (f_error_t)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorVO {
    private Long ERROR_ID;
    private Long DEVICE_ID;
    private Long ERROR_CODE_ID;
    private String ERROR_MESSAGE;
    private String ERROR_LEVEL;      // C / M / N / W
    private Integer ERROR_FLAG;       // 0: 장애, 1: 인지
    private LocalDateTime OCCUR_AT;
    private String USER_MESSAGE;
    private LocalDateTime UPDATE_AT;
    private String DEVICE_IP;
    private String IF_IP_ADDRESS;

    // JOIN 필드
    private String DEVICE_NAME;
    private String ERROR_CODE_NAME;
    private String GROUP_NAME;
}
