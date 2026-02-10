package dev3.nms.mapper;

import dev3.nms.vo.mgmt.DeviceSshVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceSshMapper {
    DeviceSshVO findByDeviceId(int deviceId);
    int insertDeviceSsh(DeviceSshVO ssh);
    int updateDeviceSsh(DeviceSshVO ssh);
    int deleteByDeviceId(int deviceId);
    boolean existsByDeviceId(int deviceId);
}
