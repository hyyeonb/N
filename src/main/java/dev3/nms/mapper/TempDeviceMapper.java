package dev3.nms.mapper;

import dev3.nms.vo.mgmt.TempDeviceVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TempDeviceMapper {
    List<TempDeviceVO> findTempDevicesByGroupIds(List<Integer> groupIds);

    int insertTempDevice(TempDeviceVO device);

    int updateTempDevice(TempDeviceVO device);

    int deleteTempDevice(int tempDeviceId);

    int deleteTempDevices(List<Integer> deviceIds);

    int insertTempDevices(List<TempDeviceVO> devices);

    List<TempDeviceVO> findAllTempDevices();

    List<String> findDuplicateIps(List<String> ipAddresses);

    TempDeviceVO findByIp(String deviceIp);

    TempDeviceVO findById(int tempDeviceId);
}
