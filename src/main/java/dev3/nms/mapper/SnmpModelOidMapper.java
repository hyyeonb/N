package dev3.nms.mapper;

import dev3.nms.vo.mgmt.SnmpModelOidVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SnmpModelOidMapper {

    /**
     * 모델 ID로 OID 목록 조회 (메트릭 정보 포함)
     */
    List<SnmpModelOidVO> findByModelId(@Param("modelId") Integer modelId);

    /**
     * OID 저장 (INSERT or UPDATE)
     */
    int upsert(SnmpModelOidVO oid);

    /**
     * OID 삭제
     */
    int delete(@Param("oidId") Integer oidId);

    /**
     * 모델의 특정 메트릭 OID 삭제
     */
    int deleteByModelAndMetric(@Param("modelId") Integer modelId, @Param("metricId") Integer metricId);

    /**
     * 모델의 모든 OID 삭제
     */
    int deleteByModelId(@Param("modelId") Integer modelId);
}
