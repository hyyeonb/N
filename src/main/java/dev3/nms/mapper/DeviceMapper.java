package dev3.nms.mapper;

import dev3.nms.vo.mgmt.DeviceScopeVO;
import dev3.nms.vo.mgmt.DeviceSnmpVO;
import dev3.nms.vo.mgmt.DeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {
    List<DeviceVO> findAllDevices();

    DeviceVO findDeviceById(int deviceId);

    List<DeviceVO> findDevicesByGroupIds(List<Integer> groupIds);

    // 페이지네이션 + 정렬 지원 조회
    List<DeviceVO> findDevicesByGroupIdsPaged(@Param("groupIds") List<Integer> groupIds,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset,
                                               @Param("sort") String sort,
                                               @Param("order") String order);

    // 총 개수 조회
    int countDevicesByGroupIds(@Param("groupIds") List<Integer> groupIds);

    int insertDevice(DeviceVO device);

    int insertDeviceSnmp(DeviceSnmpVO snmp);

    int updateDevice(DeviceVO device);

    int updateDeviceSnmp(DeviceSnmpVO snmp);

    int deleteDevice(int deviceId);

    // 일괄 삭제
    int deleteDevices(@Param("deviceIds") List<Integer> deviceIds);

    List<String> findDuplicateIps(List<String> ipAddresses);

    boolean existsDeviceSnmp(int deviceId);

    // 장비 관제 설정 (Scope)
    int insertDeviceScope(DeviceScopeVO scope);

    int updateDeviceScope(DeviceScopeVO scope);

    DeviceScopeVO findDeviceScopeById(int deviceId);

    boolean existsDeviceScope(int deviceId);
}
