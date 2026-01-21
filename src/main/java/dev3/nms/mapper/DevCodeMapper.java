package dev3.nms.mapper;

import dev3.nms.vo.mgmt.DevCodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DevCodeMapper {

    /**
     * 모든 장비군 코드 조회 (삭제되지 않은 것만)
     * 트리 구조는 서비스에서 처리
     */
    List<DevCodeVO> findAllDevCodes();

    /**
     * ID로 장비군 코드 조회
     */
    Optional<DevCodeVO> findById(@Param("devCodeId") Long devCodeId);

    /**
     * 부모 ID로 자식 장비군 코드 조회
     */
    List<DevCodeVO> findByParentId(@Param("parentDevCodeId") Long parentDevCodeId);

    /**
     * 장비군 코드 등록
     */
    int insertDevCode(DevCodeVO devCode);

    /**
     * 장비군 코드 수정
     */
    int updateDevCode(DevCodeVO devCode);

    /**
     * 장비군 코드 삭제 (실제 삭제)
     */
    int deleteDevCode(@Param("devCodeId") Long devCodeId);

    /**
     * 특정 장비군 코드 및 모든 하위 코드 ID 목록 조회 (재귀)
     */
    List<Long> findDevCodeIdWithDescendants(@Param("devCodeId") Long devCodeId);
}
