package dev3.nms.mapper;

import dev3.nms.vo.mgmt.CommonOidVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommonOidMapper {

    /**
     * 모든 OID 조회
     */
    List<CommonOidVO> findAll();

    /**
     * OID 이름으로 OID 조회
     */
    CommonOidVO findByOidName(@Param("oidName") String oidName);

    /**
     * OID 이름 목록으로 OID Map 조회
     * Key: OID_NAME, Value: OID
     */
    List<CommonOidVO> findByOidNames(@Param("oidNames") List<String> oidNames);
}
