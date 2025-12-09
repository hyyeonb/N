package dev3.nms.mapper;

import dev3.nms.vo.mgmt.ModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ModelMapper {
    /**
     * MODEL_OID와 VENDOR_ID로 모델 조회
     */
    Optional<ModelVO> findByOidAndVendorId(String modelOid, Integer vendorId);

    /**
     * MODEL_ID로 모델 조회
     */
    Optional<ModelVO> findById(Integer modelId);

    /**
     * 모든 모델 조회
     */
    List<ModelVO> findAll();

    /**
     * VENDOR_ID로 모델 조회
     */
    List<ModelVO> findByVendorId(@Param("vendorId") Integer vendorId);

    /**
     * 모델 등록
     */
    int insertModel(ModelVO model);

    /**
     * 모델 수정
     */
    int updateModel(ModelVO model);

    /**
     * 모델 삭제 (소프트 삭제)
     */
    int deleteModel(@Param("modelId") Integer modelId);
}
