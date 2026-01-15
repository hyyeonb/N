package dev3.nms.controller;

import dev3.nms.mapper.GroupMapper;
import dev3.nms.mapper.ModelMapper;
import dev3.nms.mapper.VendorMapper;
import dev3.nms.service.AuthService;
import dev3.nms.service.DeviceService;
import dev3.nms.service.GroupService;
import dev3.nms.service.PortService;
import dev3.nms.service.TempDeviceService;
import dev3.nms.service.TrafficService;
import dev3.nms.vo.mgmt.TrafficVO;
import dev3.nms.vo.auth.UserVO;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.DeviceRegistrationResultVO;
import dev3.nms.vo.mgmt.DeviceScopeVO;
import dev3.nms.vo.mgmt.DeviceVO;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.mgmt.ModelVO;
import dev3.nms.vo.mgmt.PortVO;
import dev3.nms.vo.mgmt.TempDeviceVO;
import dev3.nms.vo.mgmt.VendorVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mgmt")
@RequiredArgsConstructor
public class MgmtController {

    private final GroupService groupService;
    private final TempDeviceService tempDeviceService;
    private final DeviceService deviceService;
    private final PortService portService;
    private final AuthService authService;
    private final TrafficService trafficService;
    private final GroupMapper groupMapper;
    private final VendorMapper vendorMapper;
    private final ModelMapper modelMapper;

    /**
     * 그룹 계층 구조 전체 조회 API
     */
    @GetMapping("/groups")
    public ResponseEntity<ResVO<List<GroupVO>>> getGroupHierarchy() {
        List<GroupVO> groupHierarchy = groupService.getGroupHierarchy();
        ResVO<List<GroupVO>> response = new ResVO<>(200, "조회 성공", groupHierarchy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 그룹 트리 조회 API (프론트엔드 호환용)
     */
    @GetMapping("/groups/tree")
    public ResponseEntity<ResVO<List<GroupVO>>> getGroupTree() {
        List<GroupVO> groupHierarchy = groupService.getGroupHierarchy();
        ResVO<List<GroupVO>> response = new ResVO<>(200, "조회 성공", groupHierarchy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 특정 그룹 정보 조회 API
     */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ResVO<GroupVO>> getGroupById(@PathVariable Integer groupId) {
        try {
            GroupVO group = groupService.getGroupById(groupId);
            ResVO<GroupVO> response = new ResVO<>(200, "조회 성공", group);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 새 그룹 생성 API
     */
    @PostMapping("/groups")
    public ResponseEntity<ResVO<GroupVO>> createGroup(@RequestBody GroupVO group) {
        try {
            GroupVO createdGroup = groupService.createGroup(group);
            ResVO<GroupVO> response = new ResVO<>(201, "그룹 생성 성공", createdGroup);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 그룹 정보 수정 API
     */
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ResVO<GroupVO>> updateGroup(@PathVariable Integer groupId, @RequestBody GroupVO groupUpdates) {
        try {
            GroupVO updatedGroup = groupService.updateGroup(groupId, groupUpdates);
            ResVO<GroupVO> response = new ResVO<>(200, "그룹 수정 성공", updatedGroup);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 그룹 삭제 API
     */
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ResVO<Void>> deleteGroup(@PathVariable Integer groupId) {
        try {
            groupService.deleteGroup(groupId);
            ResVO<Void> response = new ResVO<>(200, "그룹 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 그룹 이동 API (드래그 앤 드롭)
     * 특정 그룹의 부모를 변경합니다.
     */
    @PatchMapping("/groups/{groupId}/move")
    public ResponseEntity<ResVO<GroupVO>> moveGroup(
            @PathVariable Integer groupId,
            @RequestBody GroupVO moveRequest) {
        try {
            GroupVO movedGroup = groupService.moveGroup(groupId, moveRequest.getPARENT_GROUP_ID());
            ResVO<GroupVO> response = new ResVO<>(200, "그룹 이동 성공", movedGroup);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 그룹 아이콘 설정 API
     */
    @PatchMapping("/groups/{groupId}/icon")
    public ResponseEntity<ResVO<GroupVO>> updateGroupIcon(
            @PathVariable Integer groupId,
            @RequestBody Map<String, String> iconRequest) {
        try {
            String iconName = iconRequest.get("ICON_NAME");
            GroupVO updatedGroup = groupService.updateGroupIcon(groupId, iconName);
            ResVO<GroupVO> response = new ResVO<>(200, "아이콘 설정 성공", updatedGroup);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 특정 그룹의 전체 하위 그룹 개수 조회 API
     */
    @GetMapping("/groups/{groupId}/descendants/count")
    public ResponseEntity<ResVO<Integer>> countDescendants(@PathVariable Integer groupId) {
        try {
            int count = groupService.countAllDescendants(groupId);
            ResVO<Integer> response = new ResVO<>(200, "하위 그룹 개수 조회 성공", count);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 특정 그룹의 전체 하위 그룹 목록 조회 API
     */
    @GetMapping("/groups/{groupId}/descendants")
    public ResponseEntity<ResVO<List<GroupVO>>> getDescendants(@PathVariable Integer groupId) {
        try {
            List<GroupVO> descendants = groupService.getAllDescendants(groupId);
            ResVO<List<GroupVO>> response = new ResVO<>(200, "하위 그룹 목록 조회 성공", descendants);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 특정 그룹 및 하위 그룹에 속한 임시 장비 목록 조회 API
     */
    @PostMapping("/temp-devices/search")
    public ResponseEntity<ResVO<List<TempDeviceVO>>> getTempDevicesByGroupIds(@RequestBody List<Integer> groupIds) {
        try {
            List<TempDeviceVO> devices = tempDeviceService.findTempDevicesByGroupIds(groupIds);
            ResVO<List<TempDeviceVO>> response = new ResVO<>(200, "임시 장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모든 임시 장비 목록 조회 API (페이지네이션 포함)
     */
    @GetMapping("/temp-devices/all")
    public ResponseEntity<ResVO<List<TempDeviceVO>>> getAllTempDevices() {
        try {
            List<TempDeviceVO> devices = tempDeviceService.findAllTempDevices();
            ResVO<List<TempDeviceVO>> response = new ResVO<>(200, "모든 임시 장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모든 임시 장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 새 임시 장비 생성 API
     */
    @PostMapping("/temp-devices")
    public ResponseEntity<ResVO<TempDeviceVO>> createTempDevice(@RequestBody TempDeviceVO device, HttpSession session) {
        try {
            // 세션에서 실제 사용자 ID 가져오기
            Integer userId = getUserIdFromSession(session);
            if (userId == null) {
                return new ResponseEntity<>(new ResVO<>(401, "인증되지 않은 사용자입니다", null), HttpStatus.UNAUTHORIZED);
            }

            device.setCREATE_USER_ID(userId);

            TempDeviceVO createdDevice = tempDeviceService.createTempDevice(device);
            ResVO<TempDeviceVO> response = new ResVO<>(201, "임시 장비 생성 성공", createdDevice);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 생성 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 IP 중복 검증 API (엑셀 업로드 시 사용)
     * device 테이블과 temp_device 테이블에서 중복 IP 확인
     */
    @PostMapping("/devices/validate")
    public ResponseEntity<ResVO<List<TempDeviceVO>>> validateDevices(@RequestBody List<TempDeviceVO> devices) {
        try {
            List<TempDeviceVO> validDevices = deviceService.validateDevices(devices);
            ResVO<List<TempDeviceVO>> response = new ResVO<>(200, "장비 검증 완료", validDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 검증 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 새 임시 장비 대량 생성 API (엑셀)
     */
    @PostMapping("/temp-devices/bulk")
    public ResponseEntity<ResVO<List<TempDeviceVO>>> createTempDevices(@RequestBody List<TempDeviceVO> devices, HttpSession session) {
        try {
            // 세션에서 실제 사용자 ID 가져오기
            Integer userId = getUserIdFromSession(session);
            if (userId == null) {
                return new ResponseEntity<>(new ResVO<>(401, "인증되지 않은 사용자입니다", null), HttpStatus.UNAUTHORIZED);
            }

            // 모든 장비에 사용자 ID 설정
            devices.forEach(device -> device.setCREATE_USER_ID(userId));

            tempDeviceService.createTempDevices(devices);
            // 생성된 TEMP_DEVICE_ID를 포함한 devices 반환
            ResVO<List<TempDeviceVO>> response = new ResVO<>(201, "임시 장비 대량 생성 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 대량 생성 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비 정보 수정 API
     */
    @PutMapping("/temp-devices/{deviceId}")
    public ResponseEntity<ResVO<TempDeviceVO>> updateTempDevice(@PathVariable int deviceId, @RequestBody TempDeviceVO device) {
        try {
            TempDeviceVO updatedDevice = tempDeviceService.updateTempDevice(deviceId, device);
            ResVO<TempDeviceVO> response = new ResVO<>(200, "임시 장비 수정 성공", updatedDevice);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비 삭제 API (대량)
     */
    @DeleteMapping("/temp-devices")
    public ResponseEntity<ResVO<Void>> deleteTempDevices(@RequestBody List<Integer> deviceIds) {
        try {
            tempDeviceService.deleteTempDevices(deviceIds);
            ResVO<Void> response = new ResVO<>(200, "임시 장비 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 삭제 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Device 관련 API ====================

    /**
     * 모든 장비 목록 조회 API
     */
    @GetMapping("/devices")
    public ResponseEntity<ResVO<List<DeviceVO>>> getAllDevices() {
        try {
            List<DeviceVO> devices = deviceService.getAllDevices();
            ResVO<List<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 그룹 및 하위 그룹의 장비 목록 조회 API
     */
    @PostMapping("/devices/search")
    public ResponseEntity<ResVO<List<DeviceVO>>> getDevicesByGroupIds(@RequestBody List<Integer> groupIds) {
        try {
            List<DeviceVO> devices = deviceService.getDevicesByGroupIds(groupIds);
            ResVO<List<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 그룹 및 하위 그룹의 장비 목록 페이지네이션 + 정렬 조회 API (LIMIT OFFSET)
     */
    @PostMapping("/devices/search/paged")
    public ResponseEntity<ResVO<PageVO<DeviceVO>>> getDevicesByGroupIdsPaged(
            @RequestBody List<Integer> groupIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DEVICE_ID") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            PageVO<DeviceVO> pagedDevices = deviceService.getDevicesByGroupIdsPaged(groupIds, page, size, sort, order);
            ResVO<PageVO<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", pagedDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 그룹의 장비 목록 조회 API (하위 그룹 포함 옵션)
     * includeChildren=true면 해당 그룹 및 모든 하위 그룹의 장비 조회
     */
    @GetMapping("/devices/by-group/{groupId}")
    public ResponseEntity<ResVO<PageVO<DeviceVO>>> getDevicesByGroupWithChildren(
            @PathVariable int groupId,
            @RequestParam(defaultValue = "true") boolean includeChildren,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DEVICE_ID") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            List<Integer> groupIds;
            if (includeChildren) {
                // 선택한 그룹 + 모든 하위 그룹 ID 조회
                groupIds = groupMapper.findGroupIdWithDescendants(groupId);
            } else {
                // 선택한 그룹만
                groupIds = List.of(groupId);
            }

            PageVO<DeviceVO> pagedDevices = deviceService.getDevicesByGroupIdsPaged(groupIds, page, size, sort, order);
            ResVO<PageVO<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", pagedDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("장비 목록 조회 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 장비 조회 API
     */
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<DeviceVO>> getDeviceById(@PathVariable int deviceId) {
        try {
            DeviceVO device = deviceService.getDeviceById(deviceId);
            if (device == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<DeviceVO> response = new ResVO<>(200, "장비 조회 성공", device);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 직접 등록 API (즉시 SNMP 검증)
     * 성공: r_device_t, 실패: r_temp_device_t
     */
    @PostMapping("/devices/direct")
    public ResponseEntity<ResVO<DeviceRegistrationResultVO>> createDeviceDirectly(@RequestBody TempDeviceVO device, HttpSession session) {
        try {
            // 세션에서 실제 사용자 ID 가져오기
            Integer userId = getUserIdFromSession(session);
            if (userId == null) {
                return new ResponseEntity<>(new ResVO<>(401, "인증되지 않은 사용자입니다", null), HttpStatus.UNAUTHORIZED);
            }

            DeviceRegistrationResultVO result = deviceService.createDeviceDirectly(device, userId);

            String message = String.format("장비 등록 처리 완료 - 성공: %d개, 실패: %d개",
                    result.getSuccessList().size(),
                    result.getFailureList().size());

            ResVO<DeviceRegistrationResultVO> response = new ResVO<>(201, message, result);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 등록 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 일괄 등록 API (엑셀 업로드 - 즉시 SNMP 검증)
     * 성공: r_device_t, 실패: r_temp_device_t
     */
    @PostMapping("/devices/direct/bulk")
    public ResponseEntity<ResVO<DeviceRegistrationResultVO>> createDevicesBulkDirectly(@RequestBody List<TempDeviceVO> devices, HttpSession session) {
        try {
            // 세션에서 실제 사용자 ID 가져오기
            Integer userId = getUserIdFromSession(session);
            if (userId == null) {
                return new ResponseEntity<>(new ResVO<>(401, "인증되지 않은 사용자입니다", null), HttpStatus.UNAUTHORIZED);
            }

            DeviceRegistrationResultVO result = deviceService.createDevicesBulkDirectly(devices, userId);

            String message = String.format("장비 일괄 등록 처리 완료 - 성공: %d개, 실패: %d개",
                    result.getSuccessList().size(),
                    result.getFailureList().size());

            ResVO<DeviceRegistrationResultVO> response = new ResVO<>(201, message, result);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 일괄 등록 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비를 실제 장비로 등록 API (단건)
     */
    @PostMapping("/devices/register/{tempDeviceId}")
    public ResponseEntity<ResVO<DeviceVO>> registerDeviceFromTemp(@PathVariable int tempDeviceId, @RequestBody(required = false) Map<String, Integer> requestBody) {
        try {
            Integer userId = (requestBody != null) ? requestBody.get("userId") : null;
            DeviceVO device = deviceService.registerDeviceFromTemp(tempDeviceId, userId);
            ResVO<DeviceVO> response = new ResVO<>(201, "장비 등록 성공", device);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 등록 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비를 실제 장비로 일괄 등록 API
     * 성공/실패 결과를 모두 반환
     */
    @PostMapping("/devices/register/bulk")
    public ResponseEntity<ResVO<DeviceRegistrationResultVO>> registerDevicesFromTemp(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> tempDeviceIds = (List<Integer>) requestBody.get("tempDeviceIds");
            Integer userId = (Integer) requestBody.get("userId");

            if (tempDeviceIds == null || tempDeviceIds.isEmpty()) {
                return new ResponseEntity<>(new ResVO<>(400, "tempDeviceIds는 필수입니다", null), HttpStatus.BAD_REQUEST);
            }

            DeviceRegistrationResultVO result = deviceService.registerDevicesFromTemp(tempDeviceIds, userId);

            String message = String.format("장비 등록 완료 - 성공: %d개, 실패: %d개",
                    result.getSuccessList().size(),
                    result.getFailureList().size());

            ResVO<DeviceRegistrationResultVO> response = new ResVO<>(201, message, result);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 일괄 등록 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 정보 수정 API
     */
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<DeviceVO>> updateDevice(@PathVariable int deviceId, @RequestBody DeviceVO device) {
        try {
            DeviceVO updatedDevice = deviceService.updateDevice(deviceId, device);
            ResVO<DeviceVO> response = new ResVO<>(200, "장비 수정 성공", updatedDevice);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 삭제 API
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> deleteDevice(@PathVariable int deviceId) {
        try {
            deviceService.deleteDevice(deviceId);
            ResVO<Void> response = new ResVO<>(200, "장비 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 삭제 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 일괄 삭제 API
     */
    @DeleteMapping("/devices")
    public ResponseEntity<ResVO<Void>> deleteDevices(@RequestBody Map<String, List<Integer>> requestBody) {
        try {
            List<Integer> deviceIds = requestBody.get("deviceIds");
            if (deviceIds == null || deviceIds.isEmpty()) {
                return new ResponseEntity<>(new ResVO<>(400, "삭제할 장비 ID가 없습니다", null), HttpStatus.BAD_REQUEST);
            }
            deviceService.deleteDevices(deviceIds);
            ResVO<Void> response = new ResVO<>(200, "장비 일괄 삭제 성공 (" + deviceIds.size() + "개)", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 삭제 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Port 관련 API ====================

    /**
     * 모든 포트 목록 조회 API
     */
    @GetMapping("/ports")
    public ResponseEntity<ResVO<List<PortVO>>> getAllPorts() {
        try {
            List<PortVO> ports = portService.getAllPorts();
            ResVO<List<PortVO>> response = new ResVO<>(200, "포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 장비의 포트 목록 조회 API
     */
    @GetMapping("/devices/{deviceId}/ports")
    public ResponseEntity<ResVO<List<PortVO>>> getPortsByDeviceId(@PathVariable Integer deviceId) {
        try {
            List<PortVO> ports = portService.getPortsByDeviceId(deviceId);
            ResVO<List<PortVO>> response = new ResVO<>(200, "포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 포트 조회 API
     */
    @GetMapping("/devices/{deviceId}/ports/{ifIndex}")
    public ResponseEntity<ResVO<PortVO>> getPort(@PathVariable Integer deviceId, @PathVariable Integer ifIndex) {
        try {
            PortVO port = portService.getPort(deviceId, ifIndex);
            if (port == null) {
                return new ResponseEntity<>(new ResVO<>(404, "포트를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<PortVO> response = new ResVO<>(200, "포트 조회 성공", port);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 감시 대상 포트만 조회 API
     */
    @GetMapping("/ports/monitored")
    public ResponseEntity<ResVO<List<PortVO>>> getMonitoredPorts() {
        try {
            List<PortVO> ports = portService.getMonitoredPorts();
            ResVO<List<PortVO>> response = new ResVO<>(200, "감시 대상 포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "감시 대상 포트 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 감시 설정 수정 API
     */
    @PatchMapping("/devices/{deviceId}/ports/{ifIndex}/monitor")
    public ResponseEntity<ResVO<Void>> updatePortMonitorSettings(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex,
            @RequestBody Map<String, Boolean> monitorSettings) {
        try {
            Boolean monitorAdmin = monitorSettings.get("monitorAdmin");
            Boolean monitorOper = monitorSettings.get("monitorOper");
            portService.updateMonitorSettings(deviceId, ifIndex, monitorAdmin, monitorOper);
            ResVO<Void> response = new ResVO<>(200, "포트 감시 설정 수정 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 감시 설정 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 정보 수정 API
     */
    @PutMapping("/devices/{deviceId}/ports/{ifIndex}")
    public ResponseEntity<ResVO<Void>> updatePort(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex,
            @RequestBody PortVO port) {
        try {
            port.setDEVICE_ID(deviceId);
            port.setIF_INDEX(ifIndex);
            portService.updatePort(port);
            ResVO<Void> response = new ResVO<>(200, "포트 정보 수정 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 정보 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 차트 플래그 토글 API
     */
    @PatchMapping("/devices/{deviceId}/ports/{ifIndex}/chart-flag")
    public ResponseEntity<ResVO<Void>> togglePortChartFlag(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex) {
        try {
            portService.toggleChartFlag(deviceId, ifIndex);
            ResVO<Void> response = new ResVO<>(200, "차트 플래그 토글 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "차트 플래그 토글 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 차트 표시 포트 조회 API (없으면 TOP 5 자동 설정)
     */
    @GetMapping("/devices/{deviceId}/ports/chart-enabled")
    public ResponseEntity<ResVO<List<PortVO>>> getChartEnabledPorts(@PathVariable Integer deviceId) {
        try {
            List<PortVO> ports = portService.getChartEnabledPorts(deviceId);
            ResVO<List<PortVO>> response = new ResVO<>(200, "차트 표시 포트 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "차트 표시 포트 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 차트 플래그 초기화 (TOP 5 재설정) API
     */
    @PostMapping("/devices/{deviceId}/ports/chart-flag/reset")
    public ResponseEntity<ResVO<List<PortVO>>> resetChartFlags(@PathVariable Integer deviceId) {
        try {
            portService.initializeChartPorts(deviceId, 5);
            List<PortVO> ports = portService.getChartEnabledPorts(deviceId);
            ResVO<List<PortVO>> response = new ResVO<>(200, "차트 플래그 초기화 완료", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "차트 플래그 초기화 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 세션에서 로그인한 사용자 ID를 가져오는 헬퍼 메소드
     */
    private Integer getUserIdFromSession(HttpSession session) {
        String sessionId = (String) session.getAttribute("SESSION_ID");
        if (sessionId == null) return null;
        UserVO user = authService.getUserBySession(sessionId);
        return user != null ? user.getUSER_ID().intValue() : null;
    }

    // ==================== Device Scope 관련 API ====================

    /**
     * 장비 관제 설정 조회 API
     */
    @GetMapping("/devices/{deviceId}/scope")
    public ResponseEntity<ResVO<DeviceScopeVO>> getDeviceScope(@PathVariable int deviceId) {
        try {
            DeviceScopeVO scope = deviceService.getDeviceScope(deviceId);
            if (scope == null) {
                return new ResponseEntity<>(new ResVO<>(404, "관제 설정을 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<DeviceScopeVO> response = new ResVO<>(200, "관제 설정 조회 성공", scope);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("관제 설정 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "관제 설정 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 관제 설정 수정 API
     */
    @PutMapping("/devices/{deviceId}/scope")
    public ResponseEntity<ResVO<DeviceScopeVO>> updateDeviceScope(@PathVariable int deviceId, @RequestBody DeviceScopeVO scope) {
        try {
            scope.setDEVICE_ID(deviceId);
            DeviceScopeVO updatedScope = deviceService.updateDeviceScope(scope);
            ResVO<DeviceScopeVO> response = new ResVO<>(200, "관제 설정 수정 성공", updatedScope);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "관제 설정 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Traffic 관련 API ====================

    /**
     * 장비 트래픽 데이터 조회 API (차트용)
     * @param deviceId 장비 ID
     * @param minutes 최근 N분 (기본 60분)
     */
    @GetMapping("/devices/{deviceId}/traffic")
    public ResponseEntity<ResVO<Map<String, Object>>> getDeviceTraffic(
            @PathVariable int deviceId,
            @RequestParam(required = false, defaultValue = "60") Integer minutes) {
        try {
            Map<String, Object> trafficData = trafficService.getTrafficChartData(deviceId, minutes);
            ResVO<Map<String, Object>> response = new ResVO<>(200, "트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("트래픽 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 트래픽 원시 데이터 조회 API
     * @param deviceId 장비 ID
     * @param minutes 최근 N분 (기본 60분)
     */
    @GetMapping("/devices/{deviceId}/traffic/raw")
    public ResponseEntity<ResVO<List<TrafficVO>>> getDeviceTrafficRaw(
            @PathVariable int deviceId,
            @RequestParam(required = false, defaultValue = "60") Integer minutes) {
        try {
            List<TrafficVO> trafficData = trafficService.getRecentTraffic(deviceId, minutes);
            ResVO<List<TrafficVO>> response = new ResVO<>(200, "트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("트래픽 원시 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트별 트래픽 데이터 조회 API
     * @param deviceId 장비 ID
     * @param ifIndex 포트 인덱스
     * @param minutes 최근 N분 (기본 60분)
     */
    @GetMapping("/devices/{deviceId}/ports/{ifIndex}/traffic")
    public ResponseEntity<ResVO<List<TrafficVO>>> getPortTraffic(
            @PathVariable int deviceId,
            @PathVariable int ifIndex,
            @RequestParam(required = false, defaultValue = "60") Integer minutes) {
        try {
            List<TrafficVO> trafficData = trafficService.getPortTraffic(deviceId, ifIndex, minutes);
            ResVO<List<TrafficVO>> response = new ResVO<>(200, "포트 트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("포트 트래픽 데이터 조회 실패 - deviceId: {}, ifIndex: {}", deviceId, ifIndex, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SNMP 수집 시도 후 장비 정보 업데이트 API
     * SNMP 정보를 입력받아 수집 시도 후 성공하면 장비 정보 업데이트, 실패하면 오류 반환
     */
    @PostMapping("/devices/{deviceId}/snmp-collect")
    public ResponseEntity<ResVO<DeviceVO>> collectSnmpAndUpdate(
            @PathVariable int deviceId,
            @RequestBody Map<String, Object> snmpConfig) {
        try {
            // SNMP 설정 추출
            Integer snmpVersion = (Integer) snmpConfig.get("SNMP_VERSION");
            Integer snmpPort = (Integer) snmpConfig.get("SNMP_PORT");
            String snmpCommunity = (String) snmpConfig.get("SNMP_COMMUNITY");
            String snmpUser = (String) snmpConfig.get("SNMP_USER");
            String snmpAuthProtocol = (String) snmpConfig.get("SNMP_AUTH_PROTOCOL");
            String snmpAuthPassword = (String) snmpConfig.get("SNMP_AUTH_PASSWORD");
            String snmpPrivProtocol = (String) snmpConfig.get("SNMP_PRIV_PROTOCOL");
            String snmpPrivPassword = (String) snmpConfig.get("SNMP_PRIV_PASSWORD");

            if (snmpVersion == null || snmpPort == null) {
                return new ResponseEntity<>(new ResVO<>(400, "SNMP 버전과 포트는 필수입니다", null), HttpStatus.BAD_REQUEST);
            }

            DeviceVO updatedDevice = deviceService.collectSnmpAndUpdateDevice(
                    deviceId, snmpVersion, snmpPort, snmpCommunity,
                    snmpUser, snmpAuthProtocol, snmpAuthPassword, snmpPrivProtocol, snmpPrivPassword);

            ResVO<DeviceVO> response = new ResVO<>(200, "SNMP 수집 및 장비 정보 업데이트 성공", updatedDevice);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("SNMP 수집 실패 - deviceId: {}, error: {}", deviceId, e.getMessage());
            return new ResponseEntity<>(new ResVO<>(500, "SNMP 수집 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("SNMP 수집 중 오류 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "SNMP 수집 중 오류: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Vendor 관련 API ====================

    /**
     * 벤더 목록 조회 API
     */
    @GetMapping("/vendors")
    public ResponseEntity<ResVO<List<VendorVO>>> getAllVendors() {
        try {
            List<VendorVO> vendors = vendorMapper.findAllVendors();
            ResVO<List<VendorVO>> response = new ResVO<>(200, "벤더 목록 조회 성공", vendors);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "벤더 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 벤더 조회 API
     */
    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<ResVO<VendorVO>> getVendorById(@PathVariable int vendorId) {
        try {
            VendorVO vendor = vendorMapper.findVendorById(vendorId);
            if (vendor == null) {
                return new ResponseEntity<>(new ResVO<>(404, "벤더를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<VendorVO> response = new ResVO<>(200, "벤더 조회 성공", vendor);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "벤더 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 벤더 OID 매핑 테스트 API (디버그용)
     */
    @GetMapping("/vendors/test-oid")
    public ResponseEntity<ResVO<Map<String, Object>>> testVendorOidMapping(@RequestParam String oid) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("inputOid", oid);

        String vendorBaseOid = dev3.nms.util.CommonUtil.extractVendorBaseOid(oid);
        result.put("extractedBaseOid", vendorBaseOid);

        // 모든 벤더 목록 (필터 없이)
        List<VendorVO> allVendors = vendorMapper.findAllVendorsUnfiltered();
        result.put("allVendors", allVendors);

        if (vendorBaseOid != null) {
            VendorVO matchedVendor = vendorMapper.findVendorByBaseOid(vendorBaseOid);
            result.put("matchedVendor", matchedVendor);
        }

        return new ResponseEntity<>(new ResVO<>(200, "테스트 결과", result), HttpStatus.OK);
    }

    // ==================== Model 관련 API ====================

    /**
     * 모델 목록 조회 API (vendorId 파라미터로 필터링 가능)
     */
    @GetMapping("/models")
    public ResponseEntity<ResVO<List<ModelVO>>> getModels(@RequestParam(required = false) Integer vendorId) {
        try {
            List<ModelVO> models;
            if (vendorId != null) {
                models = modelMapper.findByVendorId(vendorId);
            } else {
                models = modelMapper.findAll();
            }
            ResVO<List<ModelVO>> response = new ResVO<>(200, "모델 목록 조회 성공", models);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모델 목록 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 모델 조회 API
     */
    @GetMapping("/models/{modelId}")
    public ResponseEntity<ResVO<ModelVO>> getModelById(@PathVariable int modelId) {
        try {
            ModelVO model = modelMapper.findById(modelId).orElse(null);
            if (model == null) {
                return new ResponseEntity<>(new ResVO<>(404, "모델을 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<ModelVO> response = new ResVO<>(200, "모델 조회 성공", model);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모델 조회 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 생성 API
     * OID가 입력되면 자동으로 벤더를 매핑합니다.
     * Enterprise OID 형식: .1.3.6.1.4.1.{벤더번호}.xxx
     * DB의 VENDOR_BASE_OID는 1.3.6.1.4.1.{벤더번호} 형식 (앞 . 없음)
     */
    @PostMapping("/models")
    public ResponseEntity<ResVO<ModelVO>> createModel(@RequestBody ModelVO model, HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            model.setCREATE_USER_ID(userId);

            // OID가 있으면 벤더 자동 매핑
            if (model.getMODEL_OID() != null && !model.getMODEL_OID().isEmpty()) {
                String vendorBaseOid = dev3.nms.util.CommonUtil.extractVendorBaseOid(model.getMODEL_OID());
                log.info("[벤더매핑] 입력 OID: {}, 추출된 BASE_OID: {}", model.getMODEL_OID(), vendorBaseOid);

                if (vendorBaseOid != null) {
                    // 모든 벤더 목록 조회해서 로그 출력
                    List<VendorVO> allVendors = vendorMapper.findAllVendors();
                    log.info("[벤더매핑] DB 벤더 목록:");
                    for (VendorVO v : allVendors) {
                        log.info("  - {} (BASE_OID: '{}')", v.getVENDOR_NAME(), v.getVENDOR_BASE_OID());
                    }

                    VendorVO matchedVendor = vendorMapper.findVendorByBaseOid(vendorBaseOid);
                    if (matchedVendor != null) {
                        model.setVENDOR_ID(matchedVendor.getVENDOR_ID());
                        log.info("[벤더매핑] 매칭 성공: {} (ID: {})", matchedVendor.getVENDOR_NAME(), matchedVendor.getVENDOR_ID());
                    } else {
                        model.setVENDOR_ID(null);
                        log.warn("[벤더매핑] 매칭 실패 - 검색한 BASE_OID: '{}'", vendorBaseOid);
                    }
                } else {
                    model.setVENDOR_ID(null);
                    log.warn("[벤더매핑] Enterprise OID 형식이 아님: {}", model.getMODEL_OID());
                }
            }

            modelMapper.insertModel(model);
            ModelVO createdModel = modelMapper.findById(model.getMODEL_ID()).orElse(null);
            ResVO<ModelVO> response = new ResVO<>(201, "모델 생성 성공", createdModel);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모델 생성 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 수정 API
     */
    @PutMapping("/models/{modelId}")
    public ResponseEntity<ResVO<ModelVO>> updateModel(@PathVariable int modelId, @RequestBody ModelVO model, HttpSession session) {
        try {
            ModelVO existingModel = modelMapper.findById(modelId).orElse(null);
            if (existingModel == null) {
                return new ResponseEntity<>(new ResVO<>(404, "모델을 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            Integer userId = getUserIdFromSession(session);
            model.setMODEL_ID(modelId);
            model.setMODIFY_USER_ID(userId);
            modelMapper.updateModel(model);
            ModelVO updatedModel = modelMapper.findById(modelId).orElse(null);
            ResVO<ModelVO> response = new ResVO<>(200, "모델 수정 성공", updatedModel);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모델 수정 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 삭제 API (소프트 삭제)
     */
    @DeleteMapping("/models/{modelId}")
    public ResponseEntity<ResVO<Void>> deleteModel(@PathVariable int modelId) {
        try {
            ModelVO existingModel = modelMapper.findById(modelId).orElse(null);
            if (existingModel == null) {
                return new ResponseEntity<>(new ResVO<>(404, "모델을 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            modelMapper.deleteModel(modelId);
            ResVO<Void> response = new ResVO<>(200, "모델 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "모델 삭제 실패: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


