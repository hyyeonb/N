package dev3.nms.vo.mgmt;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdVO {
    private String DEVICE_ID;
    private String TYPE;
    private Integer MAX_VALUE;
    private Integer CRITICAL;
    private Integer MAJOR;
    private Integer MINOR;
    private Integer WARNING;

    // JOIN fields
    private String DEVICE_NAME;
    private String DEVICE_IP;
}
