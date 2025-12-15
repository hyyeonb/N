package dev3.nms.mapper;

import dev3.nms.vo.mgmt.SnmpMetricVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SnmpMetricMapper {

    /**
     * 모든 메트릭 조회
     */
    List<SnmpMetricVO> findAll();

    /**
     * 타입별 메트릭 조회
     */
    List<SnmpMetricVO> findByType(String type);

    /**
     * 메트릭 ID로 조회
     */
    SnmpMetricVO findById(Integer metricId);
}
