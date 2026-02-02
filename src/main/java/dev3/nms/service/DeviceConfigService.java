package dev3.nms.service;

import dev3.nms.mapper.DashboardMapper;
import dev3.nms.mapper.DeviceConfigMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.dashboard.DashboardDto;
import dev3.nms.vo.deviceConfig.DeviceConfigDto;
import dev3.nms.vo.mgmt.DeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceConfigService {
    private final DashboardMapper dashboardMapper;
    private final DeviceConfigMapper deviceConfigMapper;

    public DeviceConfigDto.DeviceConfigRes getDeviceConfig(
            Long deviceId,
            LocalDate date
    ) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return deviceConfigMapper.getDeviceConfig(deviceId, dateStr);
    }

    /**
     * 특정 그룹의 장비 목록 페이지네이션 + 정렬 조회 (LIMIT OFFSET)
     */
    public PageVO<DeviceConfigDto.DeviceConfigTableRes> getDevicesByGroupIdsPaged(List<Integer> groupIds, int page, int size, String sort, String order) {
        return getDevicesByGroupIdsPaged(groupIds, page, size, sort, order, null, null, null);
    }

    /**
     * 특정 그룹의 장비 목록 페이지네이션 + 정렬 + 검색 조회
     */
    public PageVO<DeviceConfigDto.DeviceConfigTableRes> getDevicesByGroupIdsPaged(List<Integer> groupIds, int page, int size, String sort, String order,
                                                      String deviceName, String deviceIp, String groupName) {
        int offset = (page - 1) * size;

        // 네트워크, 정송 장비만 조회
        List<DashboardDto.DevCodeData> devCodeList = dashboardMapper.getDevCode();
        List<Long> devCodeIdList = new ArrayList<>();
        for (DashboardDto.DevCodeData devCode : devCodeList) {
            String name = devCode.getCodeNm();
            if ("네트워크".equals(name) || "전송".equals(name)) {
                List<Long> ids = dashboardMapper.getDevCodeAllId(devCode.getDevCodeId());
                devCodeIdList.addAll(ids);
            }
        }

        List<DeviceConfigDto.DeviceConfigTableRes> devices = new ArrayList<>();
        int totalCount = 0;

        if (devCodeIdList.size() > 0) {
            devices = deviceConfigMapper.findDevicesByGroupIdsPagedWithSearch(
                    groupIds, size, offset, sort, order, deviceName, deviceIp, groupName, devCodeIdList);
            totalCount = deviceConfigMapper.countDevicesByGroupIdsWithSearch(groupIds, deviceName, deviceIp, groupName, devCodeIdList);
        }

        return PageVO.of(devices, page, size, totalCount);
    }
}
