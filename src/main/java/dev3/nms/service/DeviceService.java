package dev3.nms.service;

import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.ModelMapper;
import dev3.nms.mapper.TempDeviceMapper;
import dev3.nms.mapper.VendorMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.mgmt.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final TempDeviceMapper tempDeviceMapper;
    private final VendorMapper vendorMapper;
    private final ModelMapper modelMapper;
    private final MiddlewareClient middlewareClient;
    private final PortService portService;

    /**
     * 모든 장비 조회
     */
    public List<DeviceVO> getAllDevices() {
        return deviceMapper.findAllDevices();
    }

    /**
     * 특정 그룹의 장비 목록 조회
     */
    public List<DeviceVO> getDevicesByGroupIds(List<Integer> groupIds) {
        return deviceMapper.findDevicesByGroupIds(groupIds);
    }

    /**
     * 특정 그룹의 장비 목록 페이지네이션 + 정렬 조회 (LIMIT OFFSET)
     */
    public PageVO<DeviceVO> getDevicesByGroupIdsPaged(List<Integer> groupIds, int page, int size, String sort, String order) {
        int offset = (page - 1) * size;
        List<DeviceVO> devices = deviceMapper.findDevicesByGroupIdsPaged(groupIds, size, offset, sort, order);
        int totalCount = deviceMapper.countDevicesByGroupIds(groupIds);
        return PageVO.of(devices, page, size, totalCount);
    }

    /**
     * 특정 장비 조회
     */
    public DeviceVO getDeviceById(int deviceId) {
        return deviceMapper.findDeviceById(deviceId);
    }

    /**
     * 장비 목록 검증 (중복 IP 체크)
     * device 테이블과 temp_device 테이블에서 중복 IP 확인
     */
    public List<TempDeviceVO> validateDevices(List<TempDeviceVO> devices) {
        if (devices == null || devices.isEmpty()) {
            return new ArrayList<>();
        }

        // 모든 IP 주소 추출
        List<String> ipAddresses = devices.stream()
                .map(TempDeviceVO::getDEVICE_IP)
                .toList();

        // device 테이블에서 중복 IP 확인
        List<String> duplicateDeviceIps = deviceMapper.findDuplicateIps(ipAddresses);

        // temp_device 테이블에서 중복 IP 확인
        List<String> duplicateTempIps = tempDeviceMapper.findDuplicateIps(ipAddresses);

        // 중복 IP 병합
        List<String> allDuplicates = new ArrayList<>();
        allDuplicates.addAll(duplicateDeviceIps);
        allDuplicates.addAll(duplicateTempIps);

        // 중복되지 않은 장비만 필터링
        return devices.stream()
                .filter(device -> !allDuplicates.contains(device.getDEVICE_IP()))
                .toList();
    }

    /**
     * Ping 테스트
     * @param ipAddress 테스트할 IP 주소
     * @param timeout 타임아웃 (밀리초)
     * @return Ping 성공 여부
     */
    public boolean pingTest(String ipAddress, int timeout) {
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            return inet.isReachable(timeout);
        } catch (Exception e) {
            log.warn("Ping 테스트 실패 - IP: {}, Error: {}", ipAddress, e.getMessage());
            return false;
        }
    }

    /**
     * 모델 조회 또는 생성
     * 기존 MODEL_OID(전체 SYSOID)가 있으면 해당 MODEL_ID 반환, 없으면 새로 생성
     * MODEL_OID에는 전체 SYSOID를 저장 (벤더 OID + 모델 OID)
     */
    private Integer getOrCreateModel(String sysObjectId, VendorVO vendor, Integer userId) {
        if (vendor == null || sysObjectId == null) {
            return null;
        }

        // MODEL_OID에 전체 SYSOID 저장
        String modelOid = sysObjectId;

        if (modelOid.isEmpty()) {
            return null;
        }

        // 기존 모델 조회 (전체 OID로 검색)
        Optional<ModelVO> existingModel = modelMapper.findByOidAndVendorId(modelOid, vendor.getVENDOR_ID());

        if (existingModel.isPresent()) {
            log.info("기존 모델 사용 - ModelId: {}, OID: {}", existingModel.get().getMODEL_ID(), modelOid);
            return existingModel.get().getMODEL_ID();
        }

        // 새 모델 생성
        ModelVO newModel = ModelVO.builder()
                .VENDOR_ID(vendor.getVENDOR_ID())
                .MODEL_OID(modelOid)
                .MODEL_NAME("자동 수집 모델")  // 기본 이름 하드코딩
                .CREATE_USER_ID(userId)
                .build();

        modelMapper.insertModel(newModel);
        log.info("새 모델 생성 - ModelId: {}, OID: {}, VendorId: {}",
                newModel.getMODEL_ID(), modelOid, vendor.getVENDOR_ID());

        return newModel.getMODEL_ID();
    }

    /**
     * 장비 정보를 직접 입력받아 검증 후 등록
     * COLLECT_SNMP=true: SNMP 검증 후 등록
     * COLLECT_SNMP=false: Ping 테스트만 수행 후 등록
     * 성공: r_device_t + r_device_snmp_t(SNMP인 경우) + r_device_scope_t에 저장
     * 실패: r_temp_device_t에 저장
     */
    @Transactional
    public DeviceRegistrationResultVO createDeviceDirectly(TempDeviceVO deviceInput, Integer userId) {
        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();

        // COLLECT_SNMP 여부 확인 (기본값: true)
        boolean collectSnmp = deviceInput.getCOLLECT_SNMP() != null ? deviceInput.getCOLLECT_SNMP() : true;

        if (collectSnmp) {
            // SNMP 검증 모드
            result = registerDeviceWithSnmp(deviceInput, userId);
        } else {
            // Ping 전용 모드
            result = registerDeviceWithPingOnly(deviceInput, userId);
        }

        return result;
    }

    /**
     * SNMP 검증을 통한 장비 등록
     */
    private DeviceRegistrationResultVO registerDeviceWithSnmp(TempDeviceVO deviceInput, Integer userId) {
        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();

        // Ping 테스트 먼저 수행
        boolean pingSuccess = pingTest(deviceInput.getDEVICE_IP(), 3000);

        try {
            // SNMP로 장비 시스템 정보 조회 (Middleware API 호출)
            Map<String, String> sysInfo = middlewareClient.getDeviceSystemInfo(
                    deviceInput.getDEVICE_IP(),
                    deviceInput.getSNMP_VERSION(),
                    deviceInput.getSNMP_PORT(),
                    deviceInput.getSNMP_COMMUNITY(),
                    deviceInput.getSNMP_USER(),
                    deviceInput.getSNMP_AUTH_PROTOCOL(),
                    deviceInput.getSNMP_AUTH_PASSWORD(),
                    deviceInput.getSNMP_PRIV_PROTOCOL(),
                    deviceInput.getSNMP_PRIV_PASSWORD()
            );

            String sysDescr = sysInfo.get("sysDescr");
            String sysObjectId = sysInfo.get("sysObjectId");
            String sysName = sysInfo.get("sysName");

            // 벤더 매칭
            VendorVO vendor = vendorMapper.findVendorByOid(sysObjectId);

            // 모델 조회 또는 생성
            Integer modelId = getOrCreateModel(sysObjectId, vendor, userId);

            // Device 객체 생성
            DeviceVO device = DeviceVO.builder()
                    .GROUP_ID(deviceInput.getGROUP_ID())
                    .DEVICE_NAME(deviceInput.getDEVICE_NAME())
                    .DEVICE_SYSTEM_NAME(sysName)
                    .DEVICE_IP(deviceInput.getDEVICE_IP())
                    .DEVICE_DESC(sysDescr)
                    .MODEL_ID(modelId)
                    .CREATE_USER_ID(userId)
                    .build();

            // r_device_t에 저장
            deviceMapper.insertDevice(device);

            // r_device_snmp_t에 SNMP 정보 저장
            DeviceSnmpVO snmp = DeviceSnmpVO.builder()
                    .DEVICE_ID(device.getDEVICE_ID())
                    .SNMP_VERSION(deviceInput.getSNMP_VERSION())
                    .SNMP_PORT(deviceInput.getSNMP_PORT())
                    .SNMP_COMMUNITY(deviceInput.getSNMP_COMMUNITY())
                    .SNMP_USER(deviceInput.getSNMP_USER())
                    .SNMP_AUTH_PROTOCOL(deviceInput.getSNMP_AUTH_PROTOCOL())
                    .SNMP_AUTH_PASSWORD(deviceInput.getSNMP_AUTH_PASSWORD())
                    .SNMP_PRIV_PROTOCOL(deviceInput.getSNMP_PRIV_PROTOCOL())
                    .SNMP_PRIV_PASSWORD(deviceInput.getSNMP_PRIV_PASSWORD())
                    .build();
            deviceMapper.insertDeviceSnmp(snmp);

            // r_device_scope_t에 관제 설정 저장
            DeviceScopeVO scope = DeviceScopeVO.builder()
                    .DEVICE_ID(device.getDEVICE_ID())
                    .COLLECT_PING(true)  // PING은 항상 true
                    .COLLECT_SNMP(true)
                    .COLLECT_AGENT(deviceInput.getCOLLECT_AGENT() != null ? deviceInput.getCOLLECT_AGENT() : false)
                    .build();
            deviceMapper.insertDeviceScope(scope);

            // 포트 정보 수집 및 저장 (Middleware API 호출)
            try {
                List<PortVO> ports = middlewareClient.getDevicePortInfo(
                        deviceInput.getDEVICE_IP(),
                        deviceInput.getSNMP_VERSION(),
                        deviceInput.getSNMP_PORT(),
                        deviceInput.getSNMP_COMMUNITY(),
                        deviceInput.getSNMP_USER(),
                        deviceInput.getSNMP_AUTH_PROTOCOL(),
                        deviceInput.getSNMP_AUTH_PASSWORD(),
                        deviceInput.getSNMP_PRIV_PROTOCOL(),
                        deviceInput.getSNMP_PRIV_PASSWORD()
                );

                if (!ports.isEmpty()) {
                    ports.forEach(port -> {
                        port.setDEVICE_ID(device.getDEVICE_ID());
                    });
                    portService.createPorts(ports);

                    // PORT_COUNT 업데이트
                    device.setPORT_COUNT(ports.size());
                    deviceMapper.updateDevice(device);
                    log.info("포트 정보 수집 완료 - DeviceId: {}, 포트 수: {}", device.getDEVICE_ID(), ports.size());
                }
            } catch (Exception e) {
                log.warn("포트 정보 수집 실패 (장비 등록은 성공) - DeviceId: {}, Error: {}",
                        device.getDEVICE_ID(), e.getMessage());
            }

            // r_temp_device_t에도 저장 (DELETE_AT 설정하여 soft delete)
            deviceInput.setCREATE_USER_ID(userId);
            deviceInput.setDELETE_USER_ID(userId);
            deviceInput.setDELETE_AT(new java.sql.Timestamp(System.currentTimeMillis()));
            tempDeviceMapper.insertTempDevice(deviceInput);

            // 성공 목록에 추가
            result.getSuccessList().add(new DeviceRegistrationResultVO.DeviceRegistrationSuccess(
                    deviceInput.getTEMP_DEVICE_ID(),
                    device.getDEVICE_ID(),
                    device.getDEVICE_NAME(),
                    device.getDEVICE_IP(),
                    sysName,
                    vendor != null ? vendor.getVENDOR_NAME() : "알 수 없음",
                    sysDescr,
                    pingSuccess,
                    "SNMP"
            ));

            log.info("장비 등록 성공 (SNMP) - DeviceId: {}, ModelId: {}, Name: {}, Ping: {}",
                    device.getDEVICE_ID(), modelId, device.getDEVICE_NAME(), pingSuccess);

        } catch (Exception e) {
            // SNMP 실패 시 temp_device에 저장
            TempDeviceVO existingTempDevice = tempDeviceMapper.findByIp(deviceInput.getDEVICE_IP());

            Integer tempDeviceId;
            if (existingTempDevice != null) {
                tempDeviceId = existingTempDevice.getTEMP_DEVICE_ID();
                log.info("장비 등록 실패 (기존 미등록 장비 존재) - TempDeviceId: {}, Name: {}, IP: {}, Error: {}",
                        tempDeviceId, deviceInput.getDEVICE_NAME(), deviceInput.getDEVICE_IP(), e.getMessage());
            } else {
                deviceInput.setCREATE_USER_ID(userId);
                tempDeviceMapper.insertTempDevice(deviceInput);
                tempDeviceId = deviceInput.getTEMP_DEVICE_ID();
                log.info("장비 등록 실패 (미등록 장비로 저장) - TempDeviceId: {}, Name: {}, IP: {}, Error: {}",
                        tempDeviceId, deviceInput.getDEVICE_NAME(), deviceInput.getDEVICE_IP(), e.getMessage());
            }

            result.getFailureList().add(new DeviceRegistrationResultVO.DeviceRegistrationFailure(
                    tempDeviceId,
                    deviceInput.getDEVICE_NAME(),
                    deviceInput.getDEVICE_IP(),
                    e.getMessage(),
                    "SNMP"
            ));
        }

        return result;
    }

    /**
     * Ping 테스트만 수행하여 장비 등록 (SNMP 정보 없이)
     */
    private DeviceRegistrationResultVO registerDeviceWithPingOnly(TempDeviceVO deviceInput, Integer userId) {
        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();

        try {
            // Ping 테스트 (3초 타임아웃)
            boolean pingSuccess = pingTest(deviceInput.getDEVICE_IP(), 3000);

            if (!pingSuccess) {
                throw new RuntimeException("Ping 테스트 실패 - 장비 응답 없음");
            }

            // Device 객체 생성 (SNMP 정보 없이)
            DeviceVO device = DeviceVO.builder()
                    .GROUP_ID(deviceInput.getGROUP_ID())
                    .DEVICE_NAME(deviceInput.getDEVICE_NAME())
                    .DEVICE_IP(deviceInput.getDEVICE_IP())
                    .CREATE_USER_ID(userId)
                    .build();

            // r_device_t에 저장
            deviceMapper.insertDevice(device);

            // r_device_scope_t에 관제 설정 저장 (Ping만 true)
            DeviceScopeVO scope = DeviceScopeVO.builder()
                    .DEVICE_ID(device.getDEVICE_ID())
                    .COLLECT_PING(true)
                    .COLLECT_SNMP(false)
                    .COLLECT_AGENT(deviceInput.getCOLLECT_AGENT() != null ? deviceInput.getCOLLECT_AGENT() : false)
                    .build();
            deviceMapper.insertDeviceScope(scope);

            // r_temp_device_t에도 저장 (DELETE_AT 설정하여 soft delete)
            deviceInput.setCREATE_USER_ID(userId);
            deviceInput.setDELETE_USER_ID(userId);
            deviceInput.setDELETE_AT(new java.sql.Timestamp(System.currentTimeMillis()));
            tempDeviceMapper.insertTempDevice(deviceInput);

            // 성공 목록에 추가
            result.getSuccessList().add(new DeviceRegistrationResultVO.DeviceRegistrationSuccess(
                    deviceInput.getTEMP_DEVICE_ID(),
                    device.getDEVICE_ID(),
                    device.getDEVICE_NAME(),
                    device.getDEVICE_IP(),
                    null,  // sysName 없음
                    "-",
                    "-",
                    true,  // Ping 성공했으므로 true
                    "PING"
            ));

            log.info("장비 등록 성공 (Ping 전용) - DeviceId: {}, Name: {}, IP: {}, Ping: true",
                    device.getDEVICE_ID(), device.getDEVICE_NAME(), device.getDEVICE_IP());

        } catch (Exception e) {
            // Ping 실패 시 temp_device에 저장
            TempDeviceVO existingTempDevice = tempDeviceMapper.findByIp(deviceInput.getDEVICE_IP());

            Integer tempDeviceId;
            if (existingTempDevice != null) {
                tempDeviceId = existingTempDevice.getTEMP_DEVICE_ID();
                log.info("장비 등록 실패 (기존 미등록 장비 존재) - TempDeviceId: {}, Name: {}, IP: {}, Error: {}",
                        tempDeviceId, deviceInput.getDEVICE_NAME(), deviceInput.getDEVICE_IP(), e.getMessage());
            } else {
                deviceInput.setCREATE_USER_ID(userId);
                tempDeviceMapper.insertTempDevice(deviceInput);
                tempDeviceId = deviceInput.getTEMP_DEVICE_ID();
                log.info("장비 등록 실패 (미등록 장비로 저장) - TempDeviceId: {}, Name: {}, IP: {}, Error: {}",
                        tempDeviceId, deviceInput.getDEVICE_NAME(), deviceInput.getDEVICE_IP(), e.getMessage());
            }

            result.getFailureList().add(new DeviceRegistrationResultVO.DeviceRegistrationFailure(
                    tempDeviceId,
                    deviceInput.getDEVICE_NAME(),
                    deviceInput.getDEVICE_IP(),
                    e.getMessage(),
                    "PING"
            ));
        }

        return result;
    }

    /**
     * 여러 장비를 일괄로 SNMP 검증 후 등록
     */
    @Transactional
    public DeviceRegistrationResultVO createDevicesBulkDirectly(List<TempDeviceVO> devices, Integer userId) {
        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();

        for (TempDeviceVO deviceInput : devices) {
            DeviceRegistrationResultVO singleResult = createDeviceDirectly(deviceInput, userId);
            result.getSuccessList().addAll(singleResult.getSuccessList());
            result.getFailureList().addAll(singleResult.getFailureList());
        }

        return result;
    }

    /**
     * TempDevice를 실제 Device로 등록
     */
    @Transactional
    public DeviceVO registerDeviceFromTemp(int tempDeviceId, Integer userId) {
        // 1. TempDevice 조회
        TempDeviceVO tempDevice = tempDeviceMapper.findById(tempDeviceId);
        if (tempDevice == null) {
            throw new IllegalArgumentException("임시 장비를 찾을 수 없습니다: " + tempDeviceId);
        }

        // 2. SNMP로 장비 시스템 정보 조회 (Middleware API 호출)
        Map<String, String> sysInfo;
        try {
            sysInfo = middlewareClient.getDeviceSystemInfo(
                    tempDevice.getDEVICE_IP(),
                    tempDevice.getSNMP_VERSION(),
                    tempDevice.getSNMP_PORT(),
                    tempDevice.getSNMP_COMMUNITY(),
                    tempDevice.getSNMP_USER(),
                    tempDevice.getSNMP_AUTH_PROTOCOL(),
                    tempDevice.getSNMP_AUTH_PASSWORD(),
                    tempDevice.getSNMP_PRIV_PROTOCOL(),
                    tempDevice.getSNMP_PRIV_PASSWORD()
            );
        } catch (Exception e) {
            log.error("SNMP 요청 실패: {}", e.getMessage());
            throw new RuntimeException("SNMP 요청 실패: " + e.getMessage());
        }

        String sysDescr = sysInfo.get("sysDescr");
        String sysObjectId = sysInfo.get("sysObjectId");
        String sysName = sysInfo.get("sysName");

        log.info("장비 시스템 정보 - sysDescr: {}, sysObjectId: {}, sysName: {}", sysDescr, sysObjectId, sysName);

        // 3. sysObjectId로 벤더 매칭
        log.info(sysObjectId);
        VendorVO vendor = vendorMapper.findVendorByOid(sysObjectId);

        // 4. 모델 조회 또는 생성
        Integer modelId = getOrCreateModel(sysObjectId, vendor, userId);

        // 5. Device 객체 생성 (SNMP 정보 제외)
        DeviceVO device = DeviceVO.builder()
                .GROUP_ID(tempDevice.getGROUP_ID())
                .DEVICE_NAME(tempDevice.getDEVICE_NAME())
                .DEVICE_SYSTEM_NAME(sysName)
                .DEVICE_IP(tempDevice.getDEVICE_IP())
                .DEVICE_DESC(sysDescr)
                .MODEL_ID(modelId)
                .CREATE_USER_ID(userId)
                .build();

        // 6. r_device_t에 저장
        deviceMapper.insertDevice(device);

        // 7. r_device_snmp_t에 SNMP 정보 저장
        DeviceSnmpVO snmp = DeviceSnmpVO.builder()
                .DEVICE_ID(device.getDEVICE_ID())
                .SNMP_VERSION(tempDevice.getSNMP_VERSION())
                .SNMP_PORT(tempDevice.getSNMP_PORT())
                .SNMP_COMMUNITY(tempDevice.getSNMP_COMMUNITY())
                .SNMP_USER(tempDevice.getSNMP_USER())
                .SNMP_AUTH_PROTOCOL(tempDevice.getSNMP_AUTH_PROTOCOL())
                .SNMP_AUTH_PASSWORD(tempDevice.getSNMP_AUTH_PASSWORD())
                .SNMP_PRIV_PROTOCOL(tempDevice.getSNMP_PRIV_PROTOCOL())
                .SNMP_PRIV_PASSWORD(tempDevice.getSNMP_PRIV_PASSWORD())
                .build();
        deviceMapper.insertDeviceSnmp(snmp);

        // 7-1. r_device_scope_t에 관제 설정 저장
        DeviceScopeVO scope = DeviceScopeVO.builder()
                .DEVICE_ID(device.getDEVICE_ID())
                .COLLECT_PING(tempDevice.getCOLLECT_PING() != null ? tempDevice.getCOLLECT_PING() : false)
                .COLLECT_SNMP(tempDevice.getCOLLECT_SNMP() != null ? tempDevice.getCOLLECT_SNMP() : false)
                .COLLECT_AGENT(tempDevice.getCOLLECT_AGENT() != null ? tempDevice.getCOLLECT_AGENT() : false)
                .build();
        deviceMapper.insertDeviceScope(scope);

        // 8. 포트 정보 수집 및 저장 (Middleware API 호출)
        try {
            List<PortVO> ports = middlewareClient.getDevicePortInfo(
                    tempDevice.getDEVICE_IP(),
                    tempDevice.getSNMP_VERSION(),
                    tempDevice.getSNMP_PORT(),
                    tempDevice.getSNMP_COMMUNITY(),
                    tempDevice.getSNMP_USER(),
                    tempDevice.getSNMP_AUTH_PROTOCOL(),
                    tempDevice.getSNMP_AUTH_PASSWORD(),
                    tempDevice.getSNMP_PRIV_PROTOCOL(),
                    tempDevice.getSNMP_PRIV_PASSWORD()
            );

            if (!ports.isEmpty()) {
                ports.forEach(port -> {
                    port.setDEVICE_ID(device.getDEVICE_ID());
                });
                portService.createPorts(ports);

                // PORT_COUNT 업데이트
                device.setPORT_COUNT(ports.size());
                deviceMapper.updateDevice(device);
                log.info("포트 정보 수집 완료 - DeviceId: {}, 포트 수: {}", device.getDEVICE_ID(), ports.size());
            }
        } catch (Exception e) {
            log.warn("포트 정보 수집 실패 (장비 등록은 성공) - DeviceId: {}, Error: {}",
                    device.getDEVICE_ID(), e.getMessage());
        }

        // 9. temp_device 삭제 (논리 삭제)
        tempDeviceMapper.deleteTempDevice(tempDeviceId);

        log.info("장비 등록 완료: {} (DeviceId: {}, ModelId: {})",
                device.getDEVICE_NAME(), device.getDEVICE_ID(), modelId);

        // 조회를 다시 해서 JOIN된 정보 포함하여 반환
        return deviceMapper.findDeviceById(device.getDEVICE_ID());
    }

    /**
     * 여러 TempDevice를 실제 Device로 일괄 등록
     */
    @Transactional
    public DeviceRegistrationResultVO registerDevicesFromTemp(List<Integer> tempDeviceIds, Integer userId) {
        DeviceRegistrationResultVO result = new DeviceRegistrationResultVO();

        for (Integer tempDeviceId : tempDeviceIds) {
            try {
                TempDeviceVO tempDevice = tempDeviceMapper.findById(tempDeviceId);
                if (tempDevice == null) {
                    throw new IllegalArgumentException("임시 장비를 찾을 수 없습니다");
                }

                DeviceVO device = registerDeviceFromTemp(tempDeviceId, userId);

                // Ping 테스트 수행
                boolean pingSuccess = pingTest(device.getDEVICE_IP(), 3000);

                result.getSuccessList().add(new DeviceRegistrationResultVO.DeviceRegistrationSuccess(
                        tempDeviceId,
                        device.getDEVICE_ID(),
                        device.getDEVICE_NAME(),
                        device.getDEVICE_IP(),
                        device.getDEVICE_SYSTEM_NAME(),
                        device.getVENDOR_NAME(),
                        device.getDEVICE_DESC(),
                        pingSuccess,
                        "SNMP"
                ));

                log.info("장비 등록 성공 - TempDeviceId: {}, DeviceId: {}, Name: {}",
                        tempDeviceId, device.getDEVICE_ID(), device.getDEVICE_NAME());

            } catch (Exception e) {
                TempDeviceVO tempDevice = null;
                try {
                    tempDevice = tempDeviceMapper.findById(tempDeviceId);
                } catch (Exception ex) {
                    log.error("TempDevice 조회 실패: {}", ex.getMessage());
                }

                String deviceName = (tempDevice != null) ? tempDevice.getDEVICE_NAME() : "알 수 없음";
                String deviceIp = (tempDevice != null) ? tempDevice.getDEVICE_IP() : "알 수 없음";

                result.getFailureList().add(new DeviceRegistrationResultVO.DeviceRegistrationFailure(
                        tempDeviceId,
                        deviceName,
                        deviceIp,
                        e.getMessage(),
                        "SNMP"
                ));

                log.error("장비 등록 실패 - TempDeviceId: {}, Name: {}, Error: {}",
                        tempDeviceId, deviceName, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 장비 수정
     */
    @Transactional
    public DeviceVO updateDevice(int deviceId, DeviceVO deviceUpdates) {
        DeviceVO existingDevice = deviceMapper.findDeviceById(deviceId);
        if (existingDevice == null) {
            throw new IllegalArgumentException("장비를 찾을 수 없습니다: " + deviceId);
        }

        deviceUpdates.setDEVICE_ID(deviceId);
        deviceMapper.updateDevice(deviceUpdates);

        // SNMP 정보 업데이트 (존재하는 경우)
        if (deviceUpdates.getSNMP_PORT() != null || deviceUpdates.getSNMP_COMMUNITY() != null || deviceUpdates.getSNMP_VERSION() != null) {
            DeviceSnmpVO snmpUpdate = DeviceSnmpVO.builder()
                    .DEVICE_ID(deviceId)
                    .SNMP_VERSION(deviceUpdates.getSNMP_VERSION())
                    .SNMP_PORT(deviceUpdates.getSNMP_PORT())
                    .SNMP_COMMUNITY(deviceUpdates.getSNMP_COMMUNITY())
                    .SNMP_USER(deviceUpdates.getSNMP_USER())
                    .SNMP_AUTH_PROTOCOL(deviceUpdates.getSNMP_AUTH_PROTOCOL())
                    .SNMP_AUTH_PASSWORD(deviceUpdates.getSNMP_AUTH_PASSWORD())
                    .SNMP_PRIV_PROTOCOL(deviceUpdates.getSNMP_PRIV_PROTOCOL())
                    .SNMP_PRIV_PASSWORD(deviceUpdates.getSNMP_PRIV_PASSWORD())
                    .build();

            if (deviceMapper.existsDeviceSnmp(deviceId)) {
                deviceMapper.updateDeviceSnmp(snmpUpdate);
            } else {
                deviceMapper.insertDeviceSnmp(snmpUpdate);
            }
        }

        return deviceMapper.findDeviceById(deviceId);
    }

    /**
     * 장비 삭제 (논리 삭제)
     */
    @Transactional
    public void deleteDevice(int deviceId) {
        DeviceVO existingDevice = deviceMapper.findDeviceById(deviceId);
        if (existingDevice == null) {
            throw new IllegalArgumentException("장비를 찾을 수 없습니다: " + deviceId);
        }

        deviceMapper.deleteDevice(deviceId);
    }

    /**
     * 장비 일괄 삭제 (논리 삭제)
     */
    @Transactional
    public void deleteDevices(List<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 장비 ID가 없습니다.");
        }
        deviceMapper.deleteDevices(deviceIds);
        log.info("장비 일괄 삭제 완료: {} 개", deviceIds.size());
    }

    /**
     * 장비 관제 설정 조회
     */
    public DeviceScopeVO getDeviceScope(int deviceId) {
        return deviceMapper.findDeviceScopeById(deviceId);
    }

    /**
     * 장비 관제 설정 수정 (부분 업데이트 지원)
     */
    @Transactional
    public DeviceScopeVO updateDeviceScope(DeviceScopeVO scope) {
        // 기존 설정 조회
        DeviceScopeVO existingScope = deviceMapper.findDeviceScopeById(scope.getDEVICE_ID());

        if (existingScope != null) {
            // 기존 값과 병합 - null이 아닌 값만 업데이트
            if (scope.getCOLLECT_PING() != null) {
                existingScope.setCOLLECT_PING(scope.getCOLLECT_PING());
            }
            if (scope.getCOLLECT_SNMP() != null) {
                existingScope.setCOLLECT_SNMP(scope.getCOLLECT_SNMP());
            }
            if (scope.getCOLLECT_AGENT() != null) {
                existingScope.setCOLLECT_AGENT(scope.getCOLLECT_AGENT());
            }
            deviceMapper.updateDeviceScope(existingScope);
        } else {
            // 새로 생성 - null인 필드는 false로 설정
            if (scope.getCOLLECT_PING() == null) scope.setCOLLECT_PING(false);
            if (scope.getCOLLECT_SNMP() == null) scope.setCOLLECT_SNMP(false);
            if (scope.getCOLLECT_AGENT() == null) scope.setCOLLECT_AGENT(false);
            deviceMapper.insertDeviceScope(scope);
        }
        return deviceMapper.findDeviceScopeById(scope.getDEVICE_ID());
    }

    /**
     * SNMP 수집 시도 후 장비 정보 업데이트
     * 성공 시: 장비 정보 + SNMP 설정 + 포트 정보 업데이트, COLLECT_SNMP 활성화
     * 실패 시: RuntimeException 발생
     */
    @Transactional
    public DeviceVO collectSnmpAndUpdateDevice(int deviceId, int snmpVersion, int snmpPort, String community,
                                                String user, String authProtocol, String authPassword,
                                                String privProtocol, String privPassword) {
        // 1. 기존 장비 조회
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("장비를 찾을 수 없습니다: " + deviceId);
        }

        // 2. SNMP로 장비 시스템 정보 조회 (Middleware API 호출, 실패 시 RuntimeException 발생)
        Map<String, String> sysInfo = middlewareClient.getDeviceSystemInfo(
                device.getDEVICE_IP(),
                snmpVersion,
                snmpPort,
                community,
                user,
                authProtocol,
                authPassword,
                privProtocol,
                privPassword
        );

        String sysDescr = sysInfo.get("sysDescr");
        String sysObjectId = sysInfo.get("sysObjectId");
        String sysName = sysInfo.get("sysName");

        log.info("SNMP 수집 성공 - deviceId: {}, sysName: {}, sysObjectId: {}", deviceId, sysName, sysObjectId);

        // 3. sysObjectId로 벤더/모델 매칭
        VendorVO vendor = vendorMapper.findVendorByOid(sysObjectId);
        Integer modelId = getOrCreateModel(sysObjectId, vendor, null);

        // 4. 장비 기본 정보 업데이트
        DeviceVO deviceUpdate = new DeviceVO();
        deviceUpdate.setDEVICE_ID(deviceId);
        deviceUpdate.setDEVICE_SYSTEM_NAME(sysName);
        deviceUpdate.setDEVICE_DESC(sysDescr);
        deviceUpdate.setMODEL_ID(modelId);
        deviceMapper.updateDevice(deviceUpdate);

        // 5. SNMP 정보 저장/업데이트
        DeviceSnmpVO snmpVO = DeviceSnmpVO.builder()
                .DEVICE_ID(deviceId)
                .SNMP_VERSION(snmpVersion)
                .SNMP_PORT(snmpPort)
                .SNMP_COMMUNITY(community)
                .SNMP_USER(user)
                .SNMP_AUTH_PROTOCOL(authProtocol)
                .SNMP_AUTH_PASSWORD(authPassword)
                .SNMP_PRIV_PROTOCOL(privProtocol)
                .SNMP_PRIV_PASSWORD(privPassword)
                .build();

        if (deviceMapper.existsDeviceSnmp(deviceId)) {
            deviceMapper.updateDeviceSnmp(snmpVO);
        } else {
            deviceMapper.insertDeviceSnmp(snmpVO);
        }

        // 6. COLLECT_SNMP 활성화
        DeviceScopeVO scopeUpdate = new DeviceScopeVO();
        scopeUpdate.setDEVICE_ID(deviceId);
        scopeUpdate.setCOLLECT_SNMP(true);
        updateDeviceScope(scopeUpdate);

        // 7. 포트 정보 수집 및 저장 (Middleware API 호출)
        try {
            List<PortVO> ports = middlewareClient.getDevicePortInfo(
                    device.getDEVICE_IP(),
                    snmpVersion,
                    snmpPort,
                    community,
                    user,
                    authProtocol,
                    authPassword,
                    privProtocol,
                    privPassword
            );

            if (!ports.isEmpty()) {
                portService.saveOrUpdatePorts(deviceId, ports);
                // 포트 수 업데이트
                DeviceVO portCountUpdate = new DeviceVO();
                portCountUpdate.setDEVICE_ID(deviceId);
                portCountUpdate.setPORT_COUNT(ports.size());
                deviceMapper.updateDevice(portCountUpdate);
                log.info("포트 정보 저장 완료 - deviceId: {}, 포트 수: {}", deviceId, ports.size());
            }
        } catch (Exception e) {
            log.warn("포트 정보 수집 실패 (장비 등록은 성공): {}", e.getMessage());
        }

        return deviceMapper.findDeviceById(deviceId);
    }
}
