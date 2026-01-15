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

}
