package dev3.nms.mapper;

import dev3.nms.vo.mgmt.ThresholdVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ThresholdMapper {

    List<ThresholdVO> findAllBase();

    void updateBase(ThresholdVO threshold);

    List<ThresholdVO> findAllDevice();

    List<ThresholdVO> findByDeviceId(@Param("deviceId") String deviceId);

    void upsertDevice(ThresholdVO threshold);

    void deleteByDeviceId(@Param("deviceId") String deviceId);
}
