package dev3.nms.mapper;

import dev3.nms.vo.mgmt.EnvironmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnvironmentMapper {

    /** 최신 데이터 (장비의 각 메트릭별 최신 1건) */
    List<EnvironmentVO> findLatest(@Param("deviceId") Integer deviceId);

    /** 시계열 데이터 (최근 N분) */
    List<EnvironmentVO> findHistory(@Param("deviceId") Integer deviceId,
                                     @Param("metricCode") String metricCode,
                                     @Param("minutes") Integer minutes);
}
