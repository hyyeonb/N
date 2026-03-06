package dev3.nms.mapper;

import dev3.nms.vo.mgmt.DeviceSshVO;
import dev3.nms.vo.mgmt.DeviceVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceSshMapper {
    DeviceSshVO findByDeviceId(int deviceId);
    int insertDeviceSsh(DeviceSshVO ssh);
    int updateDeviceSsh(DeviceSshVO ssh);
    int deleteByDeviceId(int deviceId);
    boolean existsByDeviceId(int deviceId);
    // SSH 정보가 등록된 장비 목록 조회 (Traceroute 트리용)
    List<DeviceVO> findDevicesWithSsh();
}
