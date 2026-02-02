package dev3.nms.mapper;

import dev3.nms.vo.deviceConfig.DeviceConfigDto;
import dev3.nms.vo.mgmt.DeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceConfigMapper {
    DeviceConfigDto.DeviceConfigRes getDeviceConfig(@Param("deviceId") Long deviceId, @Param("dateStr") String dateStr);

    // 페이지네이션 + 정렬 + 검색 지원 조회
    List<DeviceConfigDto.DeviceConfigTableRes> findDevicesByGroupIdsPagedWithSearch(@Param("groupIds") List<Integer> groupIds,
                                                        @Param("limit") int limit,
                                                        @Param("offset") int offset,
                                                        @Param("sort") String sort,
                                                        @Param("order") String order,
                                                        @Param("deviceName") String deviceName,
                                                        @Param("deviceIp") String deviceIp,
                                                        @Param("groupName") String groupName,
                                                        @Param("devCodeIdList") List<Long> devCodeIdList);

    // 검색 조건 포함 총 개수 조회
    int countDevicesByGroupIdsWithSearch(@Param("groupIds") List<Integer> groupIds,
                                         @Param("deviceName") String deviceName,
                                         @Param("deviceIp") String deviceIp,
                                         @Param("groupName") String groupName,
                                         @Param("devCodeIdList") List<Long> devCodeIdList);
}
