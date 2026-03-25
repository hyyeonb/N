package dev3.nms.vo.mgmt;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentVO {
    private Long ENV_ID;
    private Integer DEVICE_ID;
    private String METRIC_CODE;
    private Double VALUE;
    private LocalDateTime COLLECTED_AT;
}
