package dev3.nms.mapper;

import dev3.nms.vo.mgmt.MetricTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetricTypeMapper {

    /** 전체 메트릭 유형 목록 */
    List<MetricTypeVO> findAll();

    /** 모델의 수집 메트릭 코드 목록 */
    List<String> findMetricsByModelId(@Param("modelId") Integer modelId);

    /** 장비의 수집 메트릭 코드 목록 (모델 기반) */
    List<String> findMetricsByDeviceId(@Param("deviceId") Integer deviceId);

    /** 장비군의 메트릭 코드 목록 */
    List<String> findMetricsByDevCodeId(@Param("devCodeId") Long devCodeId);

    /** 모델 메트릭 매핑 저장 (기존 삭제 후 INSERT) */
    void deleteModelMetrics(@Param("modelId") Integer modelId);

    void insertModelMetric(@Param("modelId") Integer modelId, @Param("metricCode") String metricCode);
}
