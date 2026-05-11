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

    // 페이지네이션 + 정렬 + 검색 지원 조회
    List<DeviceVO> findDevicesByGroupIdsPagedWithSearch(@Param("groupIds") List<Integer> groupIds,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset,
                                                         @Param("sort") String sort,
                                                         @Param("order") String order,
                                                         @Param("deviceName") String deviceName,
                                                         @Param("deviceIp") String deviceIp,
                                                         @Param("groupName") String groupName,
                                                         @Param("devCodeId") Integer devCodeId);

    // 검색 조건 포함 총 개수 조회
    int countDevicesByGroupIdsWithSearch(@Param("groupIds") List<Integer> groupIds,
                                          @Param("deviceName") String deviceName,
                                          @Param("deviceIp") String deviceIp,
                                          @Param("groupName") String groupName,
                                          @Param("devCodeId") Integer devCodeId);

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

    /**
     * 그룹 ID 목록에 속한 장비 ID 조회 (권한 필터용)
     */
    List<Long> findDeviceIdsByGroupIds(@Param("groupIds") List<Long> groupIds);

    // 장비 삭제 시 연관 데이터 정리
    int deleteDeviceSnmp(int deviceId);

    int deleteDeviceScope(int deviceId);

    // ==================== 미들웨어 재분배용 ====================

    int bulkUpdateMiddlewareId(@Param("oldMiddlewareId") Integer oldMiddlewareId,
                               @Param("newMiddlewareId") Integer newMiddlewareId);

    int assignUnassignedDevices(@Param("middlewareId") Integer middlewareId);

    int clearAllMiddlewareAssignments();

    /** 고아 장비(MIDDLEWARE_ID=null, FIXED=0) 개수 */
    int countOrphanDevices();

    int bulkReassignDevices(@Param("middlewareId") Integer middlewareId,
                            @Param("deviceIds") List<Integer> deviceIds);

    List<java.util.Map<String, Object>> findAllActiveDeviceIdsWithIp();

}
