package dev3.nms.vo.watch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 관제 히스토리 VO (Redis 시계열 데이터)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatchHistoryVO {
    private String time;
    private Double cpu;
    private Double mem;
    private List<WatchMetricsVO.InterfaceMetricsVO> interfaces;
}
