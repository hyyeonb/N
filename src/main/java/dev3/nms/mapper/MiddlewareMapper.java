package dev3.nms.mapper;

import dev3.nms.vo.mgmt.MiddlewareVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MiddlewareMapper {

    List<MiddlewareVO> findAll();

    MiddlewareVO findById(@Param("middlewareId") Integer middlewareId);

    MiddlewareVO findByDeviceId(@Param("deviceId") Integer deviceId);

    void insert(MiddlewareVO mw);

    void update(MiddlewareVO mw);

    void delete(@Param("middlewareId") Integer middlewareId);

    void updateHeartbeat(@Param("middlewareId") Integer middlewareId);

    int countDevicesByMiddlewareId(@Param("middlewareId") Integer middlewareId);

    int countFixedDevicesByMiddlewareId(@Param("middlewareId") Integer middlewareId);

    List<MiddlewareVO> findActiveMiddlewares();
}
