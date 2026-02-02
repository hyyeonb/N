package dev3.nms.controller;

import dev3.nms.mapper.DeviceConfigMapper;
import dev3.nms.mapper.GroupMapper;
import dev3.nms.service.DeviceConfigService;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.dashboard.DashboardDto;
import dev3.nms.vo.deviceConfig.DeviceConfigDto;
import dev3.nms.vo.mgmt.DeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/device-config")
@RequiredArgsConstructor
public class DeviceConfigController {

    private final DeviceConfigService deviceConfigService;
    private final GroupMapper groupMapper;

    @GetMapping("/{deviceId}")
    public ResponseEntity<ResVO<DeviceConfigDto.DeviceConfigRes>> getWidgetsView(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DeviceConfigDto.DeviceConfigRes deviceConfig = deviceConfigService.getDeviceConfig(deviceId, date);
        ResVO<DeviceConfigDto.DeviceConfigRes> response = new ResVO<>(200, "장비 Config 조회 성공", deviceConfig);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 특정 그룹의 장비 목록 조회 API (하위 그룹 포함 옵션)
     * includeChildren=true면 해당 그룹 및 모든 하위 그룹의 장비 조회
     */
    @GetMapping("/devices/by-group/{groupId}")
    public ResponseEntity<ResVO<PageVO<DeviceConfigDto.DeviceConfigTableRes>>> getDevicesByGroupWithChildren(
            @PathVariable int groupId,
             @RequestParam(defaultValue = "true") boolean includeChildren,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DEVICE_ID") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String groupName) {
        try {
            List<Integer> groupIds;
            if (includeChildren) {
                // 선택한 그룹 + 모든 하위 그룹 ID 조회
                groupIds = groupMapper.findGroupIdWithDescendants(groupId);
            } else {
                // 선택한 그룹만
                groupIds = List.of(groupId);
            }

            PageVO<DeviceConfigDto.DeviceConfigTableRes> pagedDevices = deviceConfigService.getDevicesByGroupIdsPaged(
                    groupIds, page, size, sort, order, deviceName, deviceIp, groupName);
            ResVO<PageVO<DeviceConfigDto.DeviceConfigTableRes>> response = new ResVO<>(200, "장비 목록 조회 성공", pagedDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("장비 목록 조회 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
