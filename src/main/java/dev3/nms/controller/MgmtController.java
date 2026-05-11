package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.config.ViewLog;
import dev3.nms.config.RequireEditPermission;
import dev3.nms.config.RequireGroupAccess;
import dev3.nms.mapper.GroupMapper;
import dev3.nms.mapper.ModelMapper;
import dev3.nms.mapper.VendorMapper;
import dev3.nms.service.DevCodeService;
import dev3.nms.service.DeviceService;
import dev3.nms.service.IcmpService;
import dev3.nms.vo.mgmt.IcmpVO;
import dev3.nms.service.GroupService;
import dev3.nms.service.MiddlewareClient;
import dev3.nms.service.PermissionService;
import dev3.nms.service.PortService;
import dev3.nms.service.TempDeviceService;
import dev3.nms.service.TrafficService;
import dev3.nms.vo.mgmt.TrafficVO;
import dev3.nms.vo.mgmt.CpuMemVO;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.DeviceRegistrationResultVO;
import dev3.nms.vo.mgmt.DeviceScopeVO;
import dev3.nms.vo.mgmt.DeviceSshVO;
import dev3.nms.vo.mgmt.DeviceVO;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.mgmt.ModelVO;
import dev3.nms.vo.mgmt.PortVO;
import dev3.nms.vo.mgmt.TempDeviceVO;
import dev3.nms.vo.mgmt.ConnectivityCheckVO;
import dev3.nms.vo.mgmt.DevCodeVO;
import dev3.nms.vo.mgmt.VendorVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mgmt")
@RequiredArgsConstructor
public class MgmtController {

    private final GroupService groupService;
    private final TempDeviceService tempDeviceService;
    private final DeviceService deviceService;
    private final PortService portService;
    private final TrafficService trafficService;
    private final IcmpService icmpService;
    private final DevCodeService devCodeService;
    private final GroupMapper groupMapper;
    private final VendorMapper vendorMapper;
    private final ModelMapper modelMapper;
    private final MiddlewareClient middlewareClient;
    private final dev3.nms.mapper.MetricTypeMapper metricTypeMapper;
    private final dev3.nms.mapper.EnvironmentMapper environmentMapper;
    private final PermissionService permissionService;
    private final dev3.nms.mapper.DeviceSshMapper deviceSshMapper;

    /**
     * 그룹 계층 구조 조회 API (접근 가능 그룹만)
     */
    @GetMapping("/groups")
    public ResponseEntity<ResVO<List<GroupVO>>> getGroupHierarchy(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        List<Long> accessibleGroupIds = permissionService.getAccessibleAssetGroupIds(userId);
        List<GroupVO> groupHierarchy = groupService.getGroupHierarchy(accessibleGroupIds);
        ResVO<List<GroupVO>> response = new ResVO<>(200, "조회 성공", groupHierarchy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 그룹 트리 조회 API (프론트엔드 호환용)
     */
    @GetMapping("/groups/tree")
    public ResponseEntity<ResVO<List<GroupVO>>> getGroupTree(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        List<Long> accessibleGroupIds = permissionService.getAccessibleAssetGroupIds(userId);
        List<GroupVO> groupHierarchy = groupService.getGroupHierarchy(accessibleGroupIds);
        ResVO<List<GroupVO>> response = new ResVO<>(200, "조회 성공", groupHierarchy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 특정 그룹 정보 조회 API
     */
    @RequireGroupAccess(groupType = "ASSET", action = "VIEW")
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
    @AuditLog(actionType = "CREATE", targetType = "GROUP", pageCode = "group_mgmt")
    @RequireEditPermission("group_mgmt")
    @PostMapping("/groups")
    public ResponseEntity<ResVO<GroupVO>> createGroup(@RequestBody GroupVO group, HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            GroupVO createdGroup = groupService.createGroup(group, userId);
            ResVO<GroupVO> response = new ResVO<>(201, "그룹 생성 성공", createdGroup);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 그룹 정보 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "GROUP", pageCode = "group_mgmt")
    @RequireEditPermission("group_mgmt")
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ResVO<GroupVO>> updateGroup(@PathVariable Integer groupId, @RequestBody GroupVO groupUpdates, HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            GroupVO updatedGroup = groupService.updateGroup(groupId, groupUpdates, userId);
            ResVO<GroupVO> response = new ResVO<>(200, "그룹 수정 성공", updatedGroup);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 그룹 삭제 API
     */
    @AuditLog(actionType = "DELETE", targetType = "GROUP", pageCode = "group_mgmt")
    @RequireEditPermission("group_mgmt")
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ResVO<Void>> deleteGroup(@PathVariable Integer groupId, HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            groupService.deleteGroup(groupId, userId);
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
    @AuditLog(actionType = "UPDATE", targetType = "GROUP", pageCode = "group_mgmt")
    @RequireEditPermission("group_mgmt")
    @PatchMapping("/groups/{groupId}/move")
    public ResponseEntity<ResVO<GroupVO>> moveGroup(
            @PathVariable Integer groupId,
            @RequestBody GroupVO moveRequest,
            HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            GroupVO movedGroup = groupService.moveGroup(groupId, moveRequest.getPARENT_GROUP_ID(), userId);
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
    @AuditLog(actionType = "UPDATE", targetType = "GROUP", pageCode = "group_mgmt")
    @RequireEditPermission("group_mgmt")
    @PatchMapping("/groups/{groupId}/icon")
    public ResponseEntity<ResVO<GroupVO>> updateGroupIcon(
            @PathVariable Integer groupId,
            @RequestBody Map<String, String> iconRequest,
            HttpSession session) {
        try {
            Integer userId = getUserIdFromSession(session);
            String iconName = iconRequest.get("ICON_NAME");
            GroupVO updatedGroup = groupService.updateGroupIcon(groupId, iconName, userId);
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
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "모든 임시 장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 새 임시 장비 생성 API
     */
    @AuditLog(actionType = "CREATE", targetType = "TEMP_DEVICE", pageCode = "new_asset_mgmt")
    @RequireEditPermission("new_asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 생성 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "장비 검증 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 새 임시 장비 대량 생성 API (엑셀)
     */
    @AuditLog(actionType = "CREATE", targetType = "TEMP_DEVICE", pageCode = "new_asset_mgmt")
    @RequireEditPermission("new_asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 대량 생성 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비 정보 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "TEMP_DEVICE", pageCode = "new_asset_mgmt")
    @RequireEditPermission("new_asset_mgmt")
    @PutMapping("/temp-devices/{deviceId}")
    public ResponseEntity<ResVO<TempDeviceVO>> updateTempDevice(@PathVariable int deviceId, @RequestBody TempDeviceVO device) {
        try {
            TempDeviceVO updatedDevice = tempDeviceService.updateTempDevice(deviceId, device);
            ResVO<TempDeviceVO> response = new ResVO<>(200, "임시 장비 수정 성공", updatedDevice);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비 삭제 API (대량)
     */
    @AuditLog(actionType = "DELETE", targetType = "TEMP_DEVICE", pageCode = "new_asset_mgmt")
    @RequireEditPermission("new_asset_mgmt")
    @DeleteMapping("/temp-devices")
    public ResponseEntity<ResVO<Void>> deleteTempDevices(@RequestBody List<Integer> deviceIds) {
        try {
            tempDeviceService.deleteTempDevices(deviceIds);
            ResVO<Void> response = new ResVO<>(200, "임시 장비 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "임시 장비 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Device 관련 API ====================

    /**
     * 모든 장비 목록 조회 API
     */
    @GetMapping("/devices")
    public ResponseEntity<ResVO<List<DeviceVO>>> getAllDevices(HttpSession session) {
        try {
            List<DeviceVO> devices = deviceService.getAllDevices();
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                devices = devices.stream()
                        .filter(d -> accessibleDeviceIds.contains((long) d.getDEVICE_ID()))
                        .toList();
            }
            ResVO<List<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 그룹 및 하위 그룹의 장비 목록 조회 API
     */
    @PostMapping("/devices/search")
    public ResponseEntity<ResVO<List<DeviceVO>>> getDevicesByGroupIds(@RequestBody List<Integer> groupIds, HttpSession session) {
        try {
            List<DeviceVO> devices = deviceService.getDevicesByGroupIds(groupIds);
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                devices = devices.stream()
                        .filter(d -> accessibleDeviceIds.contains((long) d.getDEVICE_ID()))
                        .toList();
            }
            ResVO<List<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", devices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            @RequestParam(defaultValue = "asc") String order,
            HttpSession session) {
        try {
            PageVO<DeviceVO> pagedDevices = deviceService.getDevicesByGroupIdsPaged(groupIds, page, size, sort, order);
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && pagedDevices.getContent() != null) {
                // 권한 필터링 (페이지 내). 전체 페이지 개수는 원본 totalElements 유지.
                // TODO: 정확한 접근 가능 합계를 위해 mapper 레벨에서 IN 필터링하도록 추후 리팩터링.
                List<DeviceVO> filtered = pagedDevices.getContent().stream()
                        .filter(d -> accessibleDeviceIds.contains((long) d.getDEVICE_ID()))
                        .toList();
                pagedDevices = PageVO.of(filtered,
                        pagedDevices.getPage(),
                        pagedDevices.getSize(),
                        pagedDevices.getTotalElements()); // ✅ 원본 total 유지 (이전 버그: filtered.size() 사용)
            }
            ResVO<PageVO<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", pagedDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 그룹의 장비 목록 조회 API (하위 그룹 포함 옵션)
     * includeChildren=true면 해당 그룹 및 모든 하위 그룹의 장비 조회
     */
    @RequireGroupAccess(groupType = "ASSET", action = "VIEW")
    @GetMapping("/devices/by-group/{groupId}")
    public ResponseEntity<ResVO<PageVO<DeviceVO>>> getDevicesByGroupWithChildren(
            @PathVariable int groupId,
            @RequestParam(defaultValue = "true") boolean includeChildren,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DEVICE_ID") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceIp,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) Integer devCodeId) {
        try {
            List<Integer> groupIds;
            if (includeChildren) {
                // 선택한 그룹 + 모든 하위 그룹 ID 조회
                groupIds = groupMapper.findGroupIdWithDescendants(groupId);
            } else {
                // 선택한 그룹만
                groupIds = List.of(groupId);
            }

            PageVO<DeviceVO> pagedDevices = deviceService.getDevicesByGroupIdsPaged(
                    groupIds, page, size, sort, order, deviceName, deviceIp, groupName, devCodeId);
            ResVO<PageVO<DeviceVO>> response = new ResVO<>(200, "장비 목록 조회 성공", pagedDevices);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("장비 목록 조회 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(new ResVO<>(500, "장비 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 장비 조회 API
     */
    @ViewLog(targetType = "DEVICE", pageCode = "asset_mgmt")
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<DeviceVO>> getDeviceById(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            DeviceVO device = deviceService.getDeviceById(deviceId);
            if (device == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<DeviceVO> response = new ResVO<>(200, "장비 조회 성공", device);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 직접 등록 API (즉시 SNMP 검증)
     * 성공: r_device_t, 실패: r_temp_device_t
     */
    @AuditLog(actionType = "CREATE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "장비 등록 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 일괄 등록 API (엑셀 업로드 - 즉시 SNMP 검증)
     * 성공: r_device_t, 실패: r_temp_device_t
     */
    @AuditLog(actionType = "CREATE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "장비 일괄 등록 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비를 실제 장비로 등록 API (단건)
     */
    @AuditLog(actionType = "CREATE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "장비 등록 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 임시 장비를 실제 장비로 일괄 등록 API
     * 성공/실패 결과를 모두 반환
     */
    @AuditLog(actionType = "CREATE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "장비 일괄 등록 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 정보 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<DeviceVO>> updateDevice(@PathVariable int deviceId, @RequestBody DeviceVO device, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            DeviceVO updatedDevice = deviceService.updateDevice(deviceId, device);
            ResVO<DeviceVO> response = new ResVO<>(200, "장비 수정 성공", updatedDevice);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 삭제 API
     */
    @AuditLog(actionType = "DELETE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> deleteDevice(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            deviceService.deleteDevice(deviceId);
            ResVO<Void> response = new ResVO<>(200, "장비 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 일괄 삭제 API
     */
    @AuditLog(actionType = "DELETE", targetType = "DEVICE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @DeleteMapping("/devices")
    public ResponseEntity<ResVO<Void>> deleteDevices(@RequestBody Map<String, List<Integer>> requestBody, HttpSession session) {
        try {
            List<Integer> deviceIds = requestBody.get("deviceIds");
            if (deviceIds == null || deviceIds.isEmpty()) {
                return new ResponseEntity<>(new ResVO<>(400, "삭제할 장비 ID가 없습니다", null), HttpStatus.BAD_REQUEST);
            }
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                for (Integer id : deviceIds) {
                    if (id != null && !accessibleDeviceIds.contains((long) id)) {
                        return new ResponseEntity<>(new ResVO<>(403, "접근할 수 없는 장비가 포함되어 있습니다 (deviceId=" + id + ")", null), HttpStatus.FORBIDDEN);
                    }
                }
            }
            deviceService.deleteDevices(deviceIds);
            ResVO<Void> response = new ResVO<>(200, "장비 일괄 삭제 성공 (" + deviceIds.size() + "개)", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Port 관련 API ====================

    /**
     * 모든 포트 목록 조회 API
     */
    @GetMapping("/ports")
    public ResponseEntity<ResVO<List<PortVO>>> getAllPorts(HttpSession session) {
        try {
            List<PortVO> ports = portService.getAllPorts();
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                ports = ports.stream()
                        .filter(p -> accessibleDeviceIds.contains((long) p.getDEVICE_ID()))
                        .toList();
            }
            ResVO<List<PortVO>> response = new ResVO<>(200, "포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 장비의 포트 목록 조회 API
     * @param type "all"이면 전체 포트, 기본값은 Ethernet 포트만
     */
    @GetMapping("/devices/{deviceId}/ports")
    public ResponseEntity<ResVO<List<PortVO>>> getPortsByDeviceId(
            @PathVariable Integer deviceId,
            @RequestParam(required = false, defaultValue = "ethernet") String type,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            List<PortVO> ports = "all".equalsIgnoreCase(type)
                    ? portService.getAllPortsByDeviceId(deviceId)
                    : portService.getPortsByDeviceId(deviceId);
            ResVO<List<PortVO>> response = new ResVO<>(200, "포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 포트 조회 API
     */
    @GetMapping("/devices/{deviceId}/ports/{ifIndex}")
    public ResponseEntity<ResVO<PortVO>> getPort(@PathVariable Integer deviceId, @PathVariable Integer ifIndex, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            PortVO port = portService.getPort(deviceId, ifIndex);
            if (port == null) {
                return new ResponseEntity<>(new ResVO<>(404, "포트를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<PortVO> response = new ResVO<>(200, "포트 조회 성공", port);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 감시 대상 포트만 조회 API
     */
    @GetMapping("/ports/monitored")
    public ResponseEntity<ResVO<List<PortVO>>> getMonitoredPorts(HttpSession session) {
        try {
            List<PortVO> ports = portService.getMonitoredPorts();
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                ports = ports.stream()
                        .filter(p -> accessibleDeviceIds.contains((long) p.getDEVICE_ID()))
                        .toList();
            }
            ResVO<List<PortVO>> response = new ResVO<>(200, "감시 대상 포트 목록 조회 성공", ports);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "감시 대상 포트 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 감시 설정 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "PORT", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PatchMapping("/devices/{deviceId}/ports/{ifIndex}/monitor")
    public ResponseEntity<ResVO<Void>> updatePortMonitorSettings(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex,
            @RequestBody Map<String, Boolean> monitorSettings,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            Boolean monitorAdmin = monitorSettings.get("monitorAdmin");
            Boolean monitorOper = monitorSettings.get("monitorOper");
            portService.updateMonitorSettings(deviceId, ifIndex, monitorAdmin, monitorOper);
            ResVO<Void> response = new ResVO<>(200, "포트 감시 설정 수정 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 감시 설정 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 정보 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "PORT", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PutMapping("/devices/{deviceId}/ports/{ifIndex}")
    public ResponseEntity<ResVO<Void>> updatePort(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex,
            @RequestBody PortVO port,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            port.setDEVICE_ID(deviceId);
            port.setIF_INDEX(ifIndex);
            portService.updatePort(port);
            ResVO<Void> response = new ResVO<>(200, "포트 정보 수정 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "포트 정보 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 세션에서 로그인한 사용자 ID를 가져오는 헬퍼 메소드
     */
    private Integer getUserIdFromSession(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return ((Long) userId).intValue();
        if (userId instanceof Number) return ((Number) userId).intValue();
        return null;
    }

    /**
     * 세션 사용자의 접근 가능 장비 ID 목록 조회 헬퍼
     * @return null = 전체 허용 (admin), List = 접근 가능 장비 ID (빈 리스트 = 접근 불가)
     */
    // ==================== 메트릭 유형 API ====================

    /** 전체 메트릭 유형 목록 */
    @GetMapping("/metric-types")
    public ResponseEntity<ResVO<List<dev3.nms.vo.mgmt.MetricTypeVO>>> getMetricTypes() {
        return new ResponseEntity<>(new ResVO<>(200, "조회 성공", metricTypeMapper.findAll()), HttpStatus.OK);
    }

    /** 모델의 수집 메트릭 목록 */
    @GetMapping("/models/{modelId}/metrics")
    public ResponseEntity<ResVO<List<String>>> getModelMetrics(@PathVariable Integer modelId) {
        return new ResponseEntity<>(new ResVO<>(200, "조회 성공", metricTypeMapper.findMetricsByModelId(modelId)), HttpStatus.OK);
    }

    /** 모델의 수집 메트릭 저장 */
    @AuditLog(actionType = "UPDATE", targetType = "MODEL_OID", pageCode = "model_mgmt")
    @PutMapping("/models/{modelId}/metrics")
    public ResponseEntity<ResVO<Void>> saveModelMetrics(@PathVariable Integer modelId, @RequestBody List<String> metricCodes) {
        metricTypeMapper.deleteModelMetrics(modelId);
        for (String code : metricCodes) {
            metricTypeMapper.insertModelMetric(modelId, code);
        }
        return new ResponseEntity<>(new ResVO<>(200, "저장 성공", null), HttpStatus.OK);
    }

    /** 장비의 수집 메트릭 목록 (모델 기반) */
    @GetMapping("/devices/{deviceId}/metrics")
    public ResponseEntity<ResVO<List<String>>> getDeviceMetrics(@PathVariable Integer deviceId, HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
            return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(new ResVO<>(200, "조회 성공", metricTypeMapper.findMetricsByDeviceId(deviceId)), HttpStatus.OK);
    }

    // ==================== 환경 데이터 API ====================

    /** 환경 데이터 최신값 (온도/습도 등) */
    @GetMapping("/devices/{deviceId}/environment")
    public ResponseEntity<ResVO<List<dev3.nms.vo.mgmt.EnvironmentVO>>> getEnvironmentLatest(@PathVariable Integer deviceId, HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
            return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(new ResVO<>(200, "조회 성공", environmentMapper.findLatest(deviceId)), HttpStatus.OK);
    }

    /** 환경 데이터 시계열 */
    @GetMapping("/devices/{deviceId}/environment/history")
    public ResponseEntity<ResVO<List<dev3.nms.vo.mgmt.EnvironmentVO>>> getEnvironmentHistory(
            @PathVariable Integer deviceId,
            @RequestParam String metricCode,
            @RequestParam(defaultValue = "60") Integer minutes,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
            return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(new ResVO<>(200, "조회 성공", environmentMapper.findHistory(deviceId, metricCode, minutes)), HttpStatus.OK);
    }

    private List<Long> getAccessibleDeviceIds(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return List.of();
        return permissionService.getAccessibleDeviceIds(userId);
    }

    // ==================== Device Scope 관련 API ====================

    /**
     * 장비 관제 설정 조회 API
     */
    @GetMapping("/devices/{deviceId}/scope")
    public ResponseEntity<ResVO<DeviceScopeVO>> getDeviceScope(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            DeviceScopeVO scope = deviceService.getDeviceScope(deviceId);
            if (scope == null) {
                return new ResponseEntity<>(new ResVO<>(404, "관제 설정을 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<DeviceScopeVO> response = new ResVO<>(200, "관제 설정 조회 성공", scope);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("관제 설정 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "관제 설정 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 관제 설정 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "DEVICE_SCOPE", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PutMapping("/devices/{deviceId}/scope")
    public ResponseEntity<ResVO<DeviceScopeVO>> updateDeviceScope(@PathVariable int deviceId, @RequestBody DeviceScopeVO scope, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            scope.setDEVICE_ID(deviceId);
            DeviceScopeVO updatedScope = deviceService.updateDeviceScope(scope);
            ResVO<DeviceScopeVO> response = new ResVO<>(200, "관제 설정 수정 성공", updatedScope);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "관제 설정 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Device SSH (접속 정보) 관련 API ====================

    /**
     * 장비 접속 정보 조회 API
     */
    @GetMapping("/devices/{deviceId}/ssh")
    public ResponseEntity<ResVO<DeviceSshVO>> getDeviceSsh(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            DeviceSshVO ssh = deviceService.getDeviceSsh(deviceId);
            return new ResponseEntity<>(new ResVO<>(200, "접속 정보 조회 성공", ssh), HttpStatus.OK);
        } catch (Exception e) {
            log.error("접속 정보 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "접속 정보 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 접속 정보 저장/수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "DEVICE_SSH", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PutMapping("/devices/{deviceId}/ssh")
    public ResponseEntity<ResVO<DeviceSshVO>> saveDeviceSsh(@PathVariable int deviceId, @RequestBody DeviceSshVO ssh, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            ssh.setDEVICE_ID(deviceId);
            DeviceSshVO saved = deviceService.saveOrUpdateDeviceSsh(ssh);
            return new ResponseEntity<>(new ResVO<>(200, "접속 정보 저장 성공", saved), HttpStatus.OK);
        } catch (Exception e) {
            log.error("접속 정보 저장 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "접속 정보 저장 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비 접속 정보 삭제 API
     */
    @AuditLog(actionType = "DELETE", targetType = "DEVICE_SSH", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @DeleteMapping("/devices/{deviceId}/ssh")
    public ResponseEntity<ResVO<Void>> deleteDeviceSsh(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            deviceService.deleteDeviceSsh(deviceId);
            return new ResponseEntity<>(new ResVO<>(200, "접속 정보 삭제 성공", null), HttpStatus.OK);
        } catch (Exception e) {
            log.error("접속 정보 삭제 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "접속 정보 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            @RequestParam(required = false, defaultValue = "60") Integer minutes,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            Map<String, Object> trafficData = trafficService.getTrafficChartData(deviceId, minutes);
            ResVO<Map<String, Object>> response = new ResVO<>(200, "트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("트래픽 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            @RequestParam(required = false, defaultValue = "60") Integer minutes,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            List<TrafficVO> trafficData = trafficService.getRecentTraffic(deviceId, minutes, startDate, endDate);
            ResVO<List<TrafficVO>> response = new ResVO<>(200, "트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("트래픽 원시 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * CPU/MEM 데이터 조회 API (최신값)
     * @param deviceId 장비 ID
     */
    @GetMapping("/devices/{deviceId}/cpu-mem")
    public ResponseEntity<ResVO<CpuMemVO>> getDeviceCpuMem(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            CpuMemVO cpuMem = deviceService.getLatestCpuMem(deviceId);
            ResVO<CpuMemVO> response = new ResVO<>(200, "CPU/MEM 데이터 조회 성공", cpuMem);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("CPU/MEM 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "CPU/MEM 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * CPU/MEM 시계열 데이터 조회 API
     * @param deviceId 장비 ID
     * @param minutes 최근 N분 (기본 60분)
     */
    @GetMapping("/devices/{deviceId}/cpu-mem/history")
    public ResponseEntity<ResVO<List<CpuMemVO>>> getDeviceCpuMemHistory(
            @PathVariable int deviceId,
            @RequestParam(required = false, defaultValue = "60") Integer minutes,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            List<CpuMemVO> cpuMemData = deviceService.getRecentCpuMem(deviceId, minutes, startDate, endDate);
            ResVO<List<CpuMemVO>> response = new ResVO<>(200, "CPU/MEM 시계열 데이터 조회 성공", cpuMemData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("CPU/MEM 시계열 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "CPU/MEM 시계열 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * ICMP(Ping) 시계열 데이터 조회 API
     * @param deviceId 장비 ID
     * @param minutes 최근 N분 (기본 60분, startDate/endDate 우선)
     * @param startDate 시작 일시 ISO (yyyy-MM-ddTHH:mm)
     * @param endDate 종료 일시 ISO
     * @param granularity raw|5min|30min|1hour|auto (기본 auto)
     */
    @GetMapping("/devices/{deviceId}/icmp/history")
    public ResponseEntity<ResVO<List<IcmpVO>>> getDeviceIcmpHistory(
            @PathVariable int deviceId,
            @RequestParam(required = false, defaultValue = "60") Integer minutes,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "auto") String granularity,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            List<IcmpVO> icmpData = icmpService.getHistory(deviceId, minutes, startDate, endDate, granularity);
            ResVO<List<IcmpVO>> response = new ResVO<>(200, "ICMP 시계열 데이터 조회 성공", icmpData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("ICMP 시계열 데이터 조회 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "ICMP 시계열 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            @RequestParam(required = false, defaultValue = "60") Integer minutes,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            List<TrafficVO> trafficData = trafficService.getPortTraffic(deviceId, ifIndex, minutes);
            ResVO<List<TrafficVO>> response = new ResVO<>(200, "포트 트래픽 데이터 조회 성공", trafficData);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("포트 트래픽 데이터 조회 실패 - deviceId: {}, ifIndex: {}", deviceId, ifIndex, e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 데이터 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SNMP 수집 시도 후 장비 정보 업데이트 API
     * SNMP 정보를 입력받아 수집 시도 후 성공하면 장비 정보 업데이트, 실패하면 오류 반환
     */
    @AuditLog(actionType = "UPDATE", targetType = "DEVICE_SNMP", pageCode = "asset_mgmt")
    @RequireEditPermission("asset_mgmt")
    @PostMapping("/devices/{deviceId}/snmp-collect")
    public ResponseEntity<ResVO<DeviceVO>> collectSnmpAndUpdate(
            @PathVariable int deviceId,
            @RequestBody Map<String, Object> snmpConfig,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
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
            return new ResponseEntity<>(new ResVO<>(500, "SNMP 수집 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("SNMP 수집 중 오류 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "SNMP 수집 중 오류", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 상태 실시간 체크 (SNMP GET - AdminStatus / OperStatus)
     */
    @GetMapping("/devices/{deviceId}/ports/{ifIndex}/check")
    public ResponseEntity<ResVO<MiddlewareClient.PortStatusResponse>> checkPortStatus(
            @PathVariable Integer deviceId,
            @PathVariable Integer ifIndex,
            HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            DeviceVO device = deviceService.getDeviceById(deviceId);
            if (device == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }

            MiddlewareClient.SnmpRequest snmpReq = new MiddlewareClient.SnmpRequest();
            snmpReq.setIpAddress(device.getDEVICE_IP());
            snmpReq.setSnmpVersion(device.getSNMP_VERSION() != null ? device.getSNMP_VERSION() : 2);
            snmpReq.setSnmpPort(device.getSNMP_PORT() != null ? device.getSNMP_PORT() : 161);
            snmpReq.setCommunity(device.getSNMP_COMMUNITY());
            snmpReq.setUser(device.getSNMP_USER());
            snmpReq.setAuthProtocol(device.getSNMP_AUTH_PROTOCOL());
            snmpReq.setAuthPassword(device.getSNMP_AUTH_PASSWORD());
            snmpReq.setPrivProtocol(device.getSNMP_PRIV_PROTOCOL());
            snmpReq.setPrivPassword(device.getSNMP_PRIV_PASSWORD());

            MiddlewareClient.PortStatusResponse result = middlewareClient.getPortStatus(snmpReq, ifIndex, deviceId);
            return new ResponseEntity<>(new ResVO<>(200, "포트 상태 조회 완료", result), HttpStatus.OK);
        } catch (Exception e) {
            log.error("포트 상태 체크 실패 - deviceId: {}, ifIndex: {}", deviceId, ifIndex, e);
            return new ResponseEntity<>(new ResVO<>(500, "포트 상태 체크 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 연결성 체크 API (PING → SNMP → SSH 순차 체크)
     */
    @PostMapping("/devices/{deviceId}/connectivity-check")
    public ResponseEntity<ResVO<ConnectivityCheckVO>> connectivityCheck(@PathVariable int deviceId, HttpSession session) {
        try {
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
                return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
            }
            ConnectivityCheckVO result = deviceService.connectivityCheck(deviceId);
            return new ResponseEntity<>(new ResVO<>(200, "연결성 체크 완료", result), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new ResVO<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("연결성 체크 실패 - deviceId: {}", deviceId, e);
            return new ResponseEntity<>(new ResVO<>(500, "연결성 체크 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "벤더 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "벤더 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "모델 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
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
            return new ResponseEntity<>(new ResVO<>(500, "모델 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 생성 API
     * OID가 입력되면 자동으로 벤더를 매핑합니다.
     * Enterprise OID 형식: .1.3.6.1.4.1.{벤더번호}.xxx
     * DB의 VENDOR_BASE_OID는 1.3.6.1.4.1.{벤더번호} 형식 (앞 . 없음)
     */
    @AuditLog(actionType = "CREATE", targetType = "MODEL", pageCode = "model_mgmt")
    @RequireEditPermission("model_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "모델 생성 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "MODEL", pageCode = "model_mgmt")
    @RequireEditPermission("model_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "모델 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 모델 삭제 API (소프트 삭제)
     */
    @AuditLog(actionType = "DELETE", targetType = "MODEL", pageCode = "model_mgmt")
    @RequireEditPermission("model_mgmt")
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
            return new ResponseEntity<>(new ResVO<>(500, "모델 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== DevCode (장비군) 관련 API ====================

    /**
     * 장비군 코드 트리 조회 API
     */
    @GetMapping("/dev-codes/tree")
    public ResponseEntity<ResVO<List<DevCodeVO>>> getDevCodeTree() {
        try {
            List<DevCodeVO> tree = devCodeService.getDevCodeTree();
            ResVO<List<DevCodeVO>> response = new ResVO<>(200, "장비군 트리 조회 성공", tree);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 트리 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비군 코드 목록 조회 API (플랫 리스트)
     */
    @GetMapping("/dev-codes")
    public ResponseEntity<ResVO<List<DevCodeVO>>> getAllDevCodes() {
        try {
            List<DevCodeVO> codes = devCodeService.getAllDevCodes();
            ResVO<List<DevCodeVO>> response = new ResVO<>(200, "장비군 목록 조회 성공", codes);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 목록 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 장비군 코드 조회 API
     */
    @GetMapping("/dev-codes/{devCodeId}")
    public ResponseEntity<ResVO<DevCodeVO>> getDevCodeById(@PathVariable Long devCodeId) {
        try {
            DevCodeVO code = devCodeService.getDevCodeById(devCodeId);
            if (code == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비군 코드를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            ResVO<DevCodeVO> response = new ResVO<>(200, "장비군 조회 성공", code);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비군 코드 생성 API
     */
    @AuditLog(actionType = "CREATE", targetType = "DEV_CODE", pageCode = "model_mgmt")
    @PostMapping("/dev-codes")
    public ResponseEntity<ResVO<DevCodeVO>> createDevCode(@RequestBody DevCodeVO devCode) {
        try {
            DevCodeVO created = devCodeService.createDevCode(devCode);
            ResVO<DevCodeVO> response = new ResVO<>(201, "장비군 생성 성공", created);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 생성 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비군 코드 수정 API
     */
    @AuditLog(actionType = "UPDATE", targetType = "DEV_CODE", pageCode = "model_mgmt")
    @PutMapping("/dev-codes/{devCodeId}")
    public ResponseEntity<ResVO<DevCodeVO>> updateDevCode(@PathVariable Long devCodeId, @RequestBody DevCodeVO devCode) {
        try {
            DevCodeVO existing = devCodeService.getDevCodeById(devCodeId);
            if (existing == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비군 코드를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            devCode.setDEV_CODE_ID(devCodeId);
            devCodeService.updateDevCode(devCode);
            DevCodeVO updated = devCodeService.getDevCodeById(devCodeId);
            ResVO<DevCodeVO> response = new ResVO<>(200, "장비군 수정 성공", updated);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 수정 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 장비군 코드 삭제 API (하위 코드 포함 삭제)
     */
    @AuditLog(actionType = "DELETE", targetType = "DEV_CODE", pageCode = "model_mgmt")
    @DeleteMapping("/dev-codes/{devCodeId}")
    public ResponseEntity<ResVO<Void>> deleteDevCode(@PathVariable Long devCodeId) {
        try {
            DevCodeVO existing = devCodeService.getDevCodeById(devCodeId);
            if (existing == null) {
                return new ResponseEntity<>(new ResVO<>(404, "장비군 코드를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
            }
            devCodeService.deleteDevCode(devCodeId);
            ResVO<Void> response = new ResVO<>(200, "장비군 삭제 성공", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "장비군 삭제 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== 네트워크 도구 API ====================

    /**
     * SSH 정보가 등록된 장비 목록 조회 (Traceroute 트리용)
     */
    @GetMapping("/devices/ssh-enabled")
    public ResponseEntity<ResVO<List<DeviceVO>>> getSshEnabledDevices(HttpSession session) {
        try {
            List<DeviceVO> devices = deviceSshMapper.findDevicesWithSsh();
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                devices = devices.stream()
                        .filter(d -> accessibleDeviceIds.contains((long) d.getDEVICE_ID()))
                        .toList();
            }
            return new ResponseEntity<>(new ResVO<>(200, "SSH 장비 목록 조회 성공", devices), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResVO<>(500, "조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Traceroute API
     * - sourceDeviceId: 출발 장비 (SSH로 해당 장비에서 traceroute 실행)
     * - targetDeviceId: 목적지 장비
     * - 각 홉 IP를 DB 등록 장비와 매핑하여 장비 정보 포함 반환
     */
    @PostMapping("/tools/traceroute")
    public ResponseEntity<ResVO<Map<String, Object>>> traceroute(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            Integer sourceDeviceId = params.get("sourceDeviceId") instanceof Number n ? n.intValue() : null;
            Integer targetDeviceId = params.get("targetDeviceId") instanceof Number n2 ? n2.intValue() : null;
            String targetIpDirect = (String) params.get("targetIp"); // 직접 IP 입력 폴백

            if (targetDeviceId == null && (targetIpDirect == null || targetIpDirect.isBlank())) {
                return new ResponseEntity<>(new ResVO<>(400, "목적지(targetDeviceId 또는 targetIp) 필수", null), HttpStatus.BAD_REQUEST);
            }

            // 권한 체크: source/target 장비
            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            if (accessibleDeviceIds != null) {
                if (sourceDeviceId != null && !accessibleDeviceIds.contains((long) sourceDeviceId)) {
                    return new ResponseEntity<>(new ResVO<>(403, "출발 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
                }
                if (targetDeviceId != null && !accessibleDeviceIds.contains((long) targetDeviceId)) {
                    return new ResponseEntity<>(new ResVO<>(403, "목적지 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
                }
            }

            int maxHops = params.containsKey("maxHops") ? ((Number) params.get("maxHops")).intValue() : 30;
            int timeout = params.containsKey("timeout") ? ((Number) params.get("timeout")).intValue() : 1000;

            // 목적지 IP 결정
            String targetIp = targetIpDirect;
            DeviceVO targetDevice = null;
            if (targetDeviceId != null) {
                targetDevice = deviceService.getDeviceById(targetDeviceId);
                if (targetDevice == null) {
                    return new ResponseEntity<>(new ResVO<>(404, "목적지 장비를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
                }
                targetIp = targetDevice.getDEVICE_IP();
            }

            // 출발 장비 결정
            DeviceVO sourceDevice = null;
            MiddlewareClient.TracerouteResponse trResult;

            if (sourceDeviceId != null) {
                sourceDevice = deviceService.getDeviceById(sourceDeviceId);
                if (sourceDevice == null) {
                    return new ResponseEntity<>(new ResVO<>(404, "출발 장비를 찾을 수 없습니다", null), HttpStatus.NOT_FOUND);
                }
                // SSH 자격증명 조회
                DeviceSshVO ssh = deviceService.getDeviceSsh(sourceDeviceId);
                if (ssh == null) {
                    return new ResponseEntity<>(new ResVO<>(400, "출발 장비에 SSH 접속 정보가 없습니다", null), HttpStatus.BAD_REQUEST);
                }
                int sshPort = ssh.getSSH_PORT() != null ? ssh.getSSH_PORT() : 22;
                // SSH 원격 traceroute
                trResult = middlewareClient.tracerouteFromDevice(
                        sourceDevice.getDEVICE_IP(), sshPort,
                        ssh.getSSH_USER(), ssh.getSSH_PASS(),
                        targetIp, maxHops, timeout);
            } else {
                // 출발지 미선택: Middleware 서버에서 직접 실행
                trResult = middlewareClient.traceroute(targetIp, maxHops, timeout);
            }

            if (!trResult.isSuccess()) {
                return new ResponseEntity<>(new ResVO<>(500, trResult.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 전체 장비 목록 → IP 기준 맵
            List<DeviceVO> allDevices = deviceService.getAllDevices();
            Map<String, DeviceVO> ipToDevice = allDevices.stream()
                    .collect(Collectors.toMap(DeviceVO::getDEVICE_IP, d -> d,
                            (existing, replacement) -> existing));

            // 홉 보강 (IP → 장비 정보)
            List<Map<String, Object>> enrichedHops = new ArrayList<>();
            List<MiddlewareClient.HopInfo> hops = trResult.getHops();
            if (hops == null) hops = java.util.Collections.emptyList();
            for (MiddlewareClient.HopInfo hop : hops) {
                Map<String, Object> enriched = new HashMap<>();
                enriched.put("hopNumber", hop.getHopNumber());
                enriched.put("ip", hop.getIp());
                enriched.put("rtts", hop.getRtts());

                if (!"*".equals(hop.getIp())) {
                    DeviceVO device = ipToDevice.get(hop.getIp());
                    if (device != null) {
                        Map<String, Object> di = new HashMap<>();
                        di.put("deviceId", device.getDEVICE_ID());
                        di.put("deviceName", device.getDEVICE_NAME());
                        di.put("deviceIp", device.getDEVICE_IP());
                        enriched.put("device", di);
                    }
                }
                enrichedHops.add(enriched);
            }

            // 출발/목적지 장비 정보
            Map<String, Object> data = new HashMap<>();
            data.put("targetIp", targetIp);
            data.put("hops", enrichedHops);
            if (sourceDevice != null) {
                Map<String, Object> sd = new HashMap<>();
                sd.put("deviceId", sourceDevice.getDEVICE_ID());
                sd.put("deviceName", sourceDevice.getDEVICE_NAME());
                sd.put("deviceIp", sourceDevice.getDEVICE_IP());
                data.put("sourceDevice", sd);
            }
            if (targetDevice != null) {
                Map<String, Object> td = new HashMap<>();
                td.put("deviceId", targetDevice.getDEVICE_ID());
                td.put("deviceName", targetDevice.getDEVICE_NAME());
                td.put("deviceIp", targetDevice.getDEVICE_IP());
                data.put("targetDevice", td);
            }

            return new ResponseEntity<>(new ResVO<>(200, "Traceroute 완료", data), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Traceroute API 오류", e);
            return new ResponseEntity<>(new ResVO<>(500, "Traceroute 실행 중 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== 장비별 이력 조회 ====================

    private final dev3.nms.service.ActivityLogService activityLogService;
    private final dev3.nms.service.SshSessionService sshSessionService;

    /**
     * 특정 장비의 변경 이력 조회
     */
    @GetMapping("/devices/{deviceId}/change-history")
    public ResponseEntity<ResVO<PageVO<dev3.nms.vo.auth.ActivityLogVO>>> getDeviceChangeHistory(
            @PathVariable int deviceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
            return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
        }
        var result = activityLogService.getActivitiesByDeviceId(String.valueOf(deviceId), page, size);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", result));
    }

    /**
     * 특정 장비의 SSH 접속 이력 조회 (장비 IP로 매칭)
     */
    @GetMapping("/devices/{deviceId}/ssh-history")
    public ResponseEntity<ResVO<PageVO<dev3.nms.vo.ssh.SshSessionDto.SessionRes>>> getDeviceSshHistory(
            @PathVariable int deviceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) deviceId)) {
            return new ResponseEntity<>(new ResVO<>(403, "해당 장비에 접근할 수 없습니다", null), HttpStatus.FORBIDDEN);
        }
        // 장비 IP 조회
        var device = deviceService.getDeviceById(deviceId);
        if (device == null) {
            return ResponseEntity.ok(new ResVO<>(404, "장비를 찾을 수 없습니다", null));
        }
        var result = sshSessionService.getSessionsByHost(device.getDEVICE_IP(), page, size);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", result));
    }

    // ==================== Batch Performance APIs (성능 감시 fan-out 통합) ====================

    /**
     * 요청 body에서 deviceIds 목록 추출 + 권한 필터링
     * 접근 불가 deviceId는 제외됨
     */
    @SuppressWarnings("unchecked")
    private List<Integer> extractAndFilterDeviceIds(Map<String, Object> req, HttpSession session) {
        Object raw = req.get("deviceIds");
        if (!(raw instanceof List)) return new ArrayList<>();
        List<?> list = (List<?>) raw;
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        List<Integer> result = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;
            Integer id;
            if (o instanceof Number) id = ((Number) o).intValue();
            else {
                try { id = Integer.parseInt(String.valueOf(o)); } catch (Exception e) { continue; }
            }
            if (accessibleDeviceIds == null || accessibleDeviceIds.contains((long) (int) id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    private static String asStr(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return s.isEmpty() ? null : s;
    }

    /**
     * 다중 장비 CPU/MEM 시계열 batch 조회
     * body: { deviceIds: [..], minutes?, startDate?, endDate?, granularity? }
     */
    @PostMapping("/devices/batch/cpu-mem/history")
    public ResponseEntity<ResVO<Map<String, List<CpuMemVO>>>> batchCpuMemHistory(
            @RequestBody Map<String, Object> req, HttpSession session) {
        try {
            List<Integer> deviceIds = extractAndFilterDeviceIds(req, session);
            Integer minutes = asInt(req.get("minutes"));
            String startDate = asStr(req.get("startDate"));
            String endDate = asStr(req.get("endDate"));
            String granularity = asStr(req.get("granularity"));

            Map<String, List<CpuMemVO>> data = deviceService.getRecentCpuMemBatch(deviceIds, minutes, startDate, endDate, granularity);
            return new ResponseEntity<>(new ResVO<>(200, "CPU/MEM batch 조회 성공", data), HttpStatus.OK);
        } catch (Exception e) {
            log.error("CPU/MEM batch 조회 실패", e);
            return new ResponseEntity<>(new ResVO<>(500, "CPU/MEM batch 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 다중 장비 트래픽 raw 시계열 batch 조회
     * body: { deviceIds: [..], minutes?, startDate?, endDate?, granularity? }
     */
    @PostMapping("/devices/batch/traffic/raw")
    public ResponseEntity<ResVO<Map<String, List<TrafficVO>>>> batchTrafficRaw(
            @RequestBody Map<String, Object> req, HttpSession session) {
        try {
            List<Integer> deviceIds = extractAndFilterDeviceIds(req, session);
            Integer minutes = asInt(req.get("minutes"));
            String startDate = asStr(req.get("startDate"));
            String endDate = asStr(req.get("endDate"));
            String granularity = asStr(req.get("granularity"));

            Map<String, List<TrafficVO>> data = trafficService.getRecentTrafficBatch(deviceIds, minutes, startDate, endDate, granularity);
            return new ResponseEntity<>(new ResVO<>(200, "트래픽 batch 조회 성공", data), HttpStatus.OK);
        } catch (Exception e) {
            log.error("트래픽 batch 조회 실패", e);
            return new ResponseEntity<>(new ResVO<>(500, "트래픽 batch 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 다중 장비 ICMP 시계열 batch 조회
     * body: { deviceIds: [..], minutes?, startDate?, endDate?, granularity? }
     */
    @PostMapping("/devices/batch/icmp/history")
    public ResponseEntity<ResVO<Map<String, List<IcmpVO>>>> batchIcmpHistory(
            @RequestBody Map<String, Object> req, HttpSession session) {
        try {
            List<Integer> deviceIds = extractAndFilterDeviceIds(req, session);
            Integer minutes = asInt(req.get("minutes"));
            String startDate = asStr(req.get("startDate"));
            String endDate = asStr(req.get("endDate"));
            String granularity = asStr(req.get("granularity"));

            Map<String, List<IcmpVO>> data = icmpService.getHistoryBatch(deviceIds, minutes, startDate, endDate, granularity);
            return new ResponseEntity<>(new ResVO<>(200, "ICMP batch 조회 성공", data), HttpStatus.OK);
        } catch (Exception e) {
            log.error("ICMP batch 조회 실패", e);
            return new ResponseEntity<>(new ResVO<>(500, "ICMP batch 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 다중 (장비,포트) 쌍의 트래픽 시계열 batch 조회
     * body: { ports: [{deviceId, ifIndex}, ...], minutes? }
     */
    @PostMapping("/ports/batch/traffic")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ResVO<Map<String, List<TrafficVO>>>> batchPortTraffic(
            @RequestBody Map<String, Object> req, HttpSession session) {
        try {
            Object rawPorts = req.get("ports");
            if (!(rawPorts instanceof List)) {
                return new ResponseEntity<>(new ResVO<>(400, "ports 필드는 배열이어야 합니다", null), HttpStatus.BAD_REQUEST);
            }
            Integer minutes = asInt(req.get("minutes"));
            if (minutes == null) minutes = 60;

            List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
            List<Map<String, Integer>> ports = new ArrayList<>();
            for (Object o : (List<?>) rawPorts) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) o;
                Integer did = asInt(m.get("deviceId"));
                Integer ifx = asInt(m.get("ifIndex"));
                if (did == null || ifx == null) continue;
                if (accessibleDeviceIds != null && !accessibleDeviceIds.contains((long) (int) did)) continue;
                Map<String, Integer> p = new HashMap<>();
                p.put("deviceId", did);
                p.put("ifIndex", ifx);
                ports.add(p);
            }

            Map<String, List<TrafficVO>> data = trafficService.getPortTrafficBatch(ports, minutes);
            return new ResponseEntity<>(new ResVO<>(200, "포트 트래픽 batch 조회 성공", data), HttpStatus.OK);
        } catch (Exception e) {
            log.error("포트 트래픽 batch 조회 실패", e);
            return new ResponseEntity<>(new ResVO<>(500, "포트 트래픽 batch 조회 실패", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


