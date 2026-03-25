package dev3.nms.vo.mgmt;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricTypeVO {
    private String METRIC_CODE;
    private String METRIC_NAME;
    private String UNIT;
    private String CATEGORY;
    private Boolean HAS_THRESHOLD;
    private Integer SORT_ORDER;
}
