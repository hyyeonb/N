package dev3.nms.service;

import dev3.nms.mapper.CpuMemMapper;
import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.DeviceSshMapper;
import dev3.nms.mapper.ErrorMapper;
import dev3.nms.mapper.MiddlewareMapper;
import dev3.nms.mapper.TrafficMapper;
import dev3.nms.mapper.ModelMapper;
import dev3.nms.mapper.PortMapper;
import dev3.nms.mapper.TempDeviceMapper;
import dev3.nms.mapper.VendorMapper;
import dev3.nms.mapper.WatchMapper;
import dev3.nms.mapper.ThresholdMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.mgmt.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    // 미들웨어 헬스 프로브 전용 스레드 풀 (공통 ForkJoinPool 블로킹 방지)
    private static final ExecutorService PROBE_EXECUTOR = Executors.newFixedThreadPool(4);

    private final DeviceMapper deviceMapper;
    private final DeviceSshMapper deviceSshMapper;
    private final TempDeviceMapper tempDeviceMapper;
    private final VendorMapper vendorMapper;
    private final ModelMapper modelMapper;
    private final CpuMemMapper cpuMemMapper;
    private final TrafficMapper trafficMapper;
    private final MiddlewareClient middlewareClient;
    private final MiddlewareMapper middlewareMapper;
    private final MiddlewareService middlewareService;
    private final PortService portService;
    private final ErrorMapper errorMapper;
    private final PortMapper portMapper;
    private final WatchMapper watchMapper;
    private final ThresholdMapper thresholdMapper;

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
        return getDevicesByGroupIdsPaged(groupIds, page, size, sort, order, null, null, null, null);
    }

    /**
     * 특정 그룹의 장비 목록 페이지네이션 + 정렬 + 검색 조회
     */
    public PageVO<DeviceVO> getDevicesByGroupIdsPaged(List<Integer> groupIds, int page, int size, String sort, String order,
                                                       String deviceName, String deviceIp, String groupName, Integer devCodeId) {
        int offset = (page - 1) * size;
        List<DeviceVO> devices = deviceMapper.findDevicesByGroupIdsPagedWithSearch(
                groupIds, size, offset, sort, order, deviceName, deviceIp, groupName, devCodeId);
        int totalCount = deviceMapper.countDevicesByGroupIdsWithSearch(groupIds, deviceName, deviceIp, groupName, devCodeId);
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
        boolean pingSuccess = pingTest(deviceInput.getDEVICE_IP(), 1000);

        try {
            // 모든 ACTIVE 미들웨어에 병렬 probe → 성공한 미들웨어 목록 + 시스템 정보
            MiddlewareClient.SnmpRequest snmpReq = new MiddlewareClient.SnmpRequest();
            snmpReq.setIpAddress(deviceInput.getDEVICE_IP());
            snmpReq.setSnmpVersion(deviceInput.getSNMP_VERSION());
            snmpReq.setSnmpPort(deviceInput.getSNMP_PORT());
            snmpReq.setCommunity(deviceInput.getSNMP_COMMUNITY());
            snmpReq.setUser(deviceInput.getSNMP_USER());
            snmpReq.setAuthProtocol(deviceInput.getSNMP_AUTH_PROTOCOL());
            snmpReq.setAuthPassword(deviceInput.getSNMP_AUTH_PASSWORD());
            snmpReq.setPrivProtocol(deviceInput.getSNMP_PRIV_PROTOCOL());
            snmpReq.setPrivPassword(deviceInput.getSNMP_PRIV_PASSWORD());

            MiddlewareClient.RegistrationProbe probe = middlewareClient.probeForRegistration(snmpReq);
            if (!probe.systemInfo.isSuccess()) {
                throw new RuntimeException(probe.systemInfo.getMessage() != null
                        ? probe.systemInfo.getMessage() : "SNMP 실패");
            }

            String sysDescr = probe.systemInfo.getSysDescr();
            String sysObjectId = dev3.nms.util.CommonUtil.normalizeOid(probe.systemInfo.getSysObjectId());
            String sysName = probe.systemInfo.getSysName();

            // 도달 가능 미들웨어 기반 할당 결정
            Integer assignedMiddlewareId;
            Integer middlewareFixed;
            if (probe.reachableMiddlewares.size() == 1) {
                assignedMiddlewareId = probe.reachableMiddlewares.get(0).getMIDDLEWARE_ID();
                middlewareFixed = 1;
                log.info("[등록 할당] {} → middlewareId={} (FIXED: 단일 도달)",
                        deviceInput.getDEVICE_IP(), assignedMiddlewareId);
            } else if (probe.reachableMiddlewares.size() >= 2) {
                Integer pick = null;
                long minLoad = Long.MAX_VALUE;
                for (dev3.nms.vo.mgmt.MiddlewareVO mw : probe.reachableMiddlewares) {
                    long load = middlewareMapper.countDevicesByMiddlewareId(mw.getMIDDLEWARE_ID());
                    if (load < minLoad) { minLoad = load; pick = mw.getMIDDLEWARE_ID(); }
                }
                assignedMiddlewareId = pick;
                middlewareFixed = 0;
                log.info("[등록 할당] {} → middlewareId={} (AUTO: {}개 중 최소 부하)",
                        deviceInput.getDEVICE_IP(), assignedMiddlewareId, probe.reachableMiddlewares.size());
            } else {
                assignedMiddlewareId = null;
                middlewareFixed = 0;
                log.warn("[등록 할당] {} 도달 가능 미들웨어 없음", deviceInput.getDEVICE_IP());
            }

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
                    .MIDDLEWARE_ID(assignedMiddlewareId)
                    .MIDDLEWARE_FIXED(middlewareFixed)
                    .CREATE_USER_ID(userId)
                    .build();

            // r_device_t에 저장
            deviceMapper.insertDevice(device);

            // r_device_snmp_t에 SNMP 정보 저장 (버전에 따라 불필요한 필드 null 처리)
            DeviceSnmpVO.DeviceSnmpVOBuilder snmpBuilder = DeviceSnmpVO.builder()
                    .DEVICE_ID(device.getDEVICE_ID())
                    .SNMP_VERSION(deviceInput.getSNMP_VERSION())
                    .SNMP_PORT(deviceInput.getSNMP_PORT());

            int snmpVer = parseSnmpVersion(deviceInput.getSNMP_VERSION());
            if (snmpVer == 3) {
                // v3: Community는 null, v3 필드만 저장
                snmpBuilder.SNMP_COMMUNITY(null)
                        .SNMP_USER(deviceInput.getSNMP_USER())
                        .SNMP_AUTH_PROTOCOL(deviceInput.getSNMP_AUTH_PROTOCOL())
                        .SNMP_AUTH_PASSWORD(deviceInput.getSNMP_AUTH_PASSWORD())
                        .SNMP_PRIV_PROTOCOL(deviceInput.getSNMP_PRIV_PROTOCOL())
                        .SNMP_PRIV_PASSWORD(deviceInput.getSNMP_PRIV_PASSWORD());
            } else {
                // v1/v2c: v3 필드는 null, Community만 저장
                snmpBuilder.SNMP_COMMUNITY(deviceInput.getSNMP_COMMUNITY())
                        .SNMP_USER(null)
                        .SNMP_AUTH_PROTOCOL(null)
                        .SNMP_AUTH_PASSWORD(null)
                        .SNMP_PRIV_PROTOCOL(null)
                        .SNMP_PRIV_PASSWORD(null);
            }
            deviceMapper.insertDeviceSnmp(snmpBuilder.build());

            // r_device_scope_t에 관제 설정 저장
            DeviceScopeVO scope = DeviceScopeVO.builder()
                    .DEVICE_ID(device.getDEVICE_ID())
                    .COLLECT_PING(true)  // PING은 항상 true
                    .COLLECT_SNMP(true)
                    .COLLECT_AGENT(deviceInput.getCOLLECT_AGENT() != null ? deviceInput.getCOLLECT_AGENT() : false)
                    .build();
            deviceMapper.insertDeviceScope(scope);

            // 포트 정보 수집 - 방금 할당된 미들웨어에서 조회 (deviceId 전달)
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
                        deviceInput.getSNMP_PRIV_PASSWORD(),
                        device.getDEVICE_ID()
                );

                if (!ports.isEmpty()) {
                    ports.forEach(port -> port.setDEVICE_ID(device.getDEVICE_ID()));
                    portService.createPorts(ports);

                    // PORT_COUNT 업데이트
                    device.setPORT_COUNT(ports.size());
                    deviceMapper.updateDevice(device);
                    log.info("포트 정보 수집 완료 - DeviceId: {}, middlewareId: {}, 포트 수: {}",
                            device.getDEVICE_ID(), assignedMiddlewareId, ports.size());
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
            boolean pingSuccess = pingTest(deviceInput.getDEVICE_IP(), 1000);

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

            // 자동 미들웨어 할당
            Integer assignedMiddlewareId = assignMiddleware();
            device.setMIDDLEWARE_ID(assignedMiddlewareId);

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

        // 등록 결과에 따라 재분배 트리거 (FIXED 장비 추가로 인한 AUTO 장비 재분배 기회)
        if (!result.getSuccessList().isEmpty()) {
            log.info("[등록 후] 재분배 트리거 - 성공 장비 {}대", result.getSuccessList().size());
            middlewareService.rebalanceAllDevices();
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

        // 2. SNMP probe - 모든 ACTIVE 미들웨어에 병렬 시도 → 성공한 미들웨어 목록 + sysInfo 반환
        MiddlewareClient.SnmpRequest snmpReq = new MiddlewareClient.SnmpRequest();
        snmpReq.setIpAddress(tempDevice.getDEVICE_IP());
        snmpReq.setSnmpVersion(tempDevice.getSNMP_VERSION());
        snmpReq.setSnmpPort(tempDevice.getSNMP_PORT());
        snmpReq.setCommunity(tempDevice.getSNMP_COMMUNITY());
        snmpReq.setUser(tempDevice.getSNMP_USER());
        snmpReq.setAuthProtocol(tempDevice.getSNMP_AUTH_PROTOCOL());
        snmpReq.setAuthPassword(tempDevice.getSNMP_AUTH_PASSWORD());
        snmpReq.setPrivProtocol(tempDevice.getSNMP_PRIV_PROTOCOL());
        snmpReq.setPrivPassword(tempDevice.getSNMP_PRIV_PASSWORD());

        MiddlewareClient.RegistrationProbe probe;
        try {
            probe = middlewareClient.probeForRegistration(snmpReq);
        } catch (Exception e) {
            log.error("SNMP probe 실패: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        if (!probe.systemInfo.isSuccess()) {
            throw new RuntimeException(probe.systemInfo.getMessage() != null
                    ? probe.systemInfo.getMessage() : "SNMP 실패");
        }

        String sysDescr = probe.systemInfo.getSysDescr();
        String sysObjectId = dev3.nms.util.CommonUtil.normalizeOid(probe.systemInfo.getSysObjectId());
        String sysName = probe.systemInfo.getSysName();

        log.info("장비 시스템 정보 - sysDescr: {}, sysObjectId: {}, sysName: {}", sysDescr, sysObjectId, sysName);

        // 2-1. 도달 가능 미들웨어 기반 할당 결정
        Integer assignedMiddlewareId;
        Integer middlewareFixed;
        if (probe.reachableMiddlewares.size() == 1) {
            // 한 미들웨어만 도달 가능 → 고정 할당
            assignedMiddlewareId = probe.reachableMiddlewares.get(0).getMIDDLEWARE_ID();
            middlewareFixed = 1;
            log.info("[등록 할당] deviceIp={} → middlewareId={} (FIXED: 단일 도달)",
                    tempDevice.getDEVICE_IP(), assignedMiddlewareId);
        } else if (probe.reachableMiddlewares.size() >= 2) {
            // 여러 미들웨어 도달 가능 → 최소 부하 선택 + 자동 분배
            Integer pick = null;
            long minLoad = Long.MAX_VALUE;
            for (dev3.nms.vo.mgmt.MiddlewareVO mw : probe.reachableMiddlewares) {
                long load = middlewareMapper.countDevicesByMiddlewareId(mw.getMIDDLEWARE_ID());
                if (load < minLoad) { minLoad = load; pick = mw.getMIDDLEWARE_ID(); }
            }
            assignedMiddlewareId = pick;
            middlewareFixed = 0;
            log.info("[등록 할당] deviceIp={} → middlewareId={} (AUTO: {}개 중 최소 부하)",
                    tempDevice.getDEVICE_IP(), assignedMiddlewareId, probe.reachableMiddlewares.size());
        } else {
            // 도달 가능 미들웨어 없음 (probe 성공해도 reachable이 비어있는 fallback 케이스)
            assignedMiddlewareId = null;
            middlewareFixed = 0;
            log.warn("[등록 할당] deviceIp={} 도달 가능 미들웨어 없음 (fallback)", tempDevice.getDEVICE_IP());
        }

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
                .MIDDLEWARE_ID(assignedMiddlewareId)
                .MIDDLEWARE_FIXED(middlewareFixed)
                .CREATE_USER_ID(userId)
                .build();

        // 6. r_device_t에 저장
        deviceMapper.insertDevice(device);

        // 7. r_device_snmp_t에 SNMP 정보 저장 (버전에 따라 불필요한 필드 null 처리)
        DeviceSnmpVO.DeviceSnmpVOBuilder snmpBuilder = DeviceSnmpVO.builder()
                .DEVICE_ID(device.getDEVICE_ID())
                .SNMP_VERSION(tempDevice.getSNMP_VERSION())
                .SNMP_PORT(tempDevice.getSNMP_PORT());

        int snmpVer = parseSnmpVersion(tempDevice.getSNMP_VERSION());
        if (snmpVer == 3) {
            // v3: Community는 null, v3 필드만 저장
            snmpBuilder.SNMP_COMMUNITY(null)
                    .SNMP_USER(tempDevice.getSNMP_USER())
                    .SNMP_AUTH_PROTOCOL(tempDevice.getSNMP_AUTH_PROTOCOL())
                    .SNMP_AUTH_PASSWORD(tempDevice.getSNMP_AUTH_PASSWORD())
                    .SNMP_PRIV_PROTOCOL(tempDevice.getSNMP_PRIV_PROTOCOL())
                    .SNMP_PRIV_PASSWORD(tempDevice.getSNMP_PRIV_PASSWORD());
        } else {
            // v1/v2c: v3 필드는 null, Community만 저장
            snmpBuilder.SNMP_COMMUNITY(tempDevice.getSNMP_COMMUNITY())
                    .SNMP_USER(null)
                    .SNMP_AUTH_PROTOCOL(null)
                    .SNMP_AUTH_PASSWORD(null)
                    .SNMP_PRIV_PROTOCOL(null)
                    .SNMP_PRIV_PASSWORD(null);
        }
        deviceMapper.insertDeviceSnmp(snmpBuilder.build());

        // 7-1. r_device_scope_t에 관제 설정 저장
        DeviceScopeVO scope = DeviceScopeVO.builder()
                .DEVICE_ID(device.getDEVICE_ID())
                .COLLECT_PING(tempDevice.getCOLLECT_PING() != null ? tempDevice.getCOLLECT_PING() : false)
                .COLLECT_SNMP(tempDevice.getCOLLECT_SNMP() != null ? tempDevice.getCOLLECT_SNMP() : false)
                .COLLECT_AGENT(tempDevice.getCOLLECT_AGENT() != null ? tempDevice.getCOLLECT_AGENT() : false)
                .build();
        deviceMapper.insertDeviceScope(scope);

        // 8. 포트 정보 수집 - 방금 할당된 미들웨어에서 조회 (deviceId 전달)
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
                    tempDevice.getSNMP_PRIV_PASSWORD(),
                    device.getDEVICE_ID()
            );

            if (!ports.isEmpty()) {
                ports.forEach(port -> port.setDEVICE_ID(device.getDEVICE_ID()));
                portService.createPorts(ports);

                device.setPORT_COUNT(ports.size());
                deviceMapper.updateDevice(device);
                log.info("포트 정보 수집 완료 - DeviceId: {}, middlewareId: {}, 포트 수: {}",
                        device.getDEVICE_ID(), assignedMiddlewareId, ports.size());
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
                boolean pingSuccess = pingTest(device.getDEVICE_IP(), 1000);

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

        if (!result.getSuccessList().isEmpty()) {
            log.info("[등록 후] 재분배 트리거 - 성공 장비 {}대", result.getSuccessList().size());
            middlewareService.rebalanceAllDevices();
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

        // 유저가 MIDDLEWARE_ID를 직접 변경하면 FIXED=1로 잠금 (재분배 제외)
        Integer newMwId = deviceUpdates.getMIDDLEWARE_ID();
        Integer oldMwId = existingDevice.getMIDDLEWARE_ID();
        boolean mwChanged = !java.util.Objects.equals(newMwId, oldMwId);
        if (mwChanged && newMwId != null && deviceUpdates.getMIDDLEWARE_FIXED() == null) {
            deviceUpdates.setMIDDLEWARE_FIXED(1);
            log.info("[미들웨어 변경] deviceId={} middlewareId {} → {} (FIXED=1 자동 설정)",
                    deviceId, oldMwId, newMwId);
        }

        deviceMapper.updateDevice(deviceUpdates);

        // IP 변경 시 활성 장애(f_error_t)만 현재 IP로 동기화
        // 이력(f_error_history_t)은 발생 당시 스냅샷 보존 (감사/포렌식 목적)
        String newIp = deviceUpdates.getDEVICE_IP();
        String oldIp = existingDevice.getDEVICE_IP();
        boolean ipChanged = newIp != null && !newIp.equals(oldIp);
        if (ipChanged) {
            int activeUpdated = errorMapper.updateErrorDeviceIp(deviceId, newIp);
            log.info("[장비 IP 변경] deviceId={} {} → {} (활성 장애 {}건 동기화, 이력은 보존)",
                    deviceId, oldIp, newIp, activeUpdated);
        }

        // SNMP 정보 업데이트 (존재하는 경우, 버전에 따라 불필요한 필드 null 처리)
        if (deviceUpdates.getSNMP_PORT() != null || deviceUpdates.getSNMP_COMMUNITY() != null ||
            deviceUpdates.getSNMP_VERSION() != null || deviceUpdates.getSNMP_USER() != null) {

            DeviceSnmpVO.DeviceSnmpVOBuilder snmpBuilder = DeviceSnmpVO.builder()
                    .DEVICE_ID(deviceId)
                    .SNMP_VERSION(deviceUpdates.getSNMP_VERSION())
                    .SNMP_PORT(deviceUpdates.getSNMP_PORT());

            int snmpVer = parseSnmpVersion(deviceUpdates.getSNMP_VERSION());
            if (snmpVer == 3) {
                // v3: Community는 null, v3 필드만 저장
                snmpBuilder.SNMP_COMMUNITY(null)
                        .SNMP_USER(deviceUpdates.getSNMP_USER())
                        .SNMP_AUTH_PROTOCOL(deviceUpdates.getSNMP_AUTH_PROTOCOL())
                        .SNMP_AUTH_PASSWORD(deviceUpdates.getSNMP_AUTH_PASSWORD())
                        .SNMP_PRIV_PROTOCOL(deviceUpdates.getSNMP_PRIV_PROTOCOL())
                        .SNMP_PRIV_PASSWORD(deviceUpdates.getSNMP_PRIV_PASSWORD());
            } else {
                // v1/v2c: v3 필드는 null, Community만 저장
                snmpBuilder.SNMP_COMMUNITY(deviceUpdates.getSNMP_COMMUNITY())
                        .SNMP_USER(null)
                        .SNMP_AUTH_PROTOCOL(null)
                        .SNMP_AUTH_PASSWORD(null)
                        .SNMP_PRIV_PROTOCOL(null)
                        .SNMP_PRIV_PASSWORD(null);
            }
            DeviceSnmpVO snmpUpdate = snmpBuilder.build();

            if (deviceMapper.existsDeviceSnmp(deviceId)) {
                deviceMapper.updateDeviceSnmp(snmpUpdate);
            } else {
                deviceMapper.insertDeviceSnmp(snmpUpdate);
            }
        }

        // IP 변경 시 수집 미들웨어 reachability 재검증 (@Async이므로 응답 지연 없음)
        if (ipChanged) {
            middlewareService.rebalanceAllDevices();
        }

        return deviceMapper.findDeviceById(deviceId);
    }

    /**
     * 장비 삭제 (논리 삭제 + 연관 데이터 캐스케이드 정리)
     *
     * 정리 순서:
     * 1. 활성 장애 → 이력 이관 후 삭제
     * 2. 관제 그룹에서 장비/인터페이스 제거
     * 3. 포트 소프트 삭제
     * 4. SSH 크리덴셜 삭제 (보안)
     * 5. SNMP 설정 삭제
     * 6. 수집 설정(Scope) 삭제
     * 7. 장비 본체 소프트 삭제
     */
    @Transactional
    public void deleteDevice(int deviceId) {
        DeviceVO existingDevice = deviceMapper.findDeviceById(deviceId);
        if (existingDevice == null) {
            throw new IllegalArgumentException("장비를 찾을 수 없습니다: " + deviceId);
        }

        cascadeDeleteDevice(deviceId);
        log.info("장비 삭제 완료 (cascade): deviceId={}", deviceId);
    }

    /**
     * 장비 일괄 삭제 (논리 삭제 + 연관 데이터 캐스케이드 정리)
     */
    @Transactional
    public void deleteDevices(List<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 장비 ID가 없습니다.");
        }
        for (int deviceId : deviceIds) {
            cascadeDeleteDevice(deviceId);
        }
        log.info("장비 일괄 삭제 완료 (cascade): {} 개", deviceIds.size());
    }

    /**
     * 단일 장비에 대한 캐스케이드 삭제 처리 (내부용)
     */
    private void cascadeDeleteDevice(int deviceId) {
        // 1. 활성 장애 + 장애 이력 삭제 (장비 삭제 시 이력 보존 불필요)
        int deletedErrors = errorMapper.deleteDeviceErrors(deviceId);
        int deletedHistory = errorMapper.deleteErrorHistoryByDeviceId(deviceId);
        if (deletedErrors > 0 || deletedHistory > 0) {
            log.info("  장비 {} 장애 정리: 활성={}건, 이력={}건 삭제", deviceId, deletedErrors, deletedHistory);
        }

        // 2. 성능 데이터 삭제 (트래픽, CPU/MEM, ICMP) + 임계치 오버라이드 삭제
        int delTraffic = trafficMapper.deleteByDeviceId(deviceId);
        int delCpuMem = cpuMemMapper.deleteByDeviceId(deviceId);
        int delIcmp = errorMapper.deleteIcmpByDeviceId(deviceId);
        thresholdMapper.deleteByDeviceId(String.valueOf(deviceId));
        if (delTraffic + delCpuMem + delIcmp > 0) {
            log.info("  장비 {} 성능 데이터 삭제: 트래픽={}건, CPU/MEM={}건, ICMP={}건",
                    deviceId, delTraffic, delCpuMem, delIcmp);
        }

        // 3. 관제 그룹에서 제거 (인터페이스 먼저, 장비 후)
        watchMapper.deleteDeviceInterfacesFromAllGroups(deviceId);
        watchMapper.deleteDeviceFromAllWatchGroups(deviceId);

        // 4. 포트 소프트 삭제
        portMapper.deletePortsByDeviceId(deviceId);

        // 5. SSH 크리덴셜 삭제
        deviceSshMapper.deleteByDeviceId(deviceId);

        // 6. SNMP 설정 삭제
        deviceMapper.deleteDeviceSnmp(deviceId);

        // 7. 수집 설정(Scope) 삭제
        deviceMapper.deleteDeviceScope(deviceId);

        // 8. 장비 본체 소프트 삭제
        deviceMapper.deleteDevice(deviceId);
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

        // 5. SNMP 정보 저장/업데이트 (버전에 따라 불필요한 필드 null 처리)
        DeviceSnmpVO.DeviceSnmpVOBuilder snmpBuilder = DeviceSnmpVO.builder()
                .DEVICE_ID(deviceId)
                .SNMP_VERSION(snmpVersion)
                .SNMP_PORT(snmpPort);

        if (snmpVersion == 3) {
            // v3: Community는 null, v3 필드만 저장
            snmpBuilder.SNMP_COMMUNITY(null)
                    .SNMP_USER(user)
                    .SNMP_AUTH_PROTOCOL(authProtocol)
                    .SNMP_AUTH_PASSWORD(authPassword)
                    .SNMP_PRIV_PROTOCOL(privProtocol)
                    .SNMP_PRIV_PASSWORD(privPassword);
        } else {
            // v1/v2c: v3 필드는 null, Community만 저장
            snmpBuilder.SNMP_COMMUNITY(community)
                    .SNMP_USER(null)
                    .SNMP_AUTH_PROTOCOL(null)
                    .SNMP_AUTH_PASSWORD(null)
                    .SNMP_PRIV_PROTOCOL(null)
                    .SNMP_PRIV_PASSWORD(null);
        }
        DeviceSnmpVO snmpVO = snmpBuilder.build();

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

    /**
     * SNMP 버전을 숫자로 변환
     * Integer, String 모두 지원
     */
    private int parseSnmpVersion(Object version) {
        if (version == null) return 2;
        if (version instanceof Integer) {
            return (Integer) version;
        }
        if (version instanceof String) {
            String str = ((String) version).toLowerCase();
            switch (str) {
                case "1": return 1;
                case "2c":
                case "2": return 2;
                case "3": return 3;
                default: return 2;
            }
        }
        return 2;
    }

    /**
     * 특정 장비의 최신 CPU/MEM 데이터 조회
     */
    public CpuMemVO getLatestCpuMem(int deviceId) {
        return cpuMemMapper.findLatestByDeviceId(deviceId);
    }

    /**
     * 특정 장비의 최근 CPU/MEM 데이터 조회 (시계열)
     */
    public List<CpuMemVO> getRecentCpuMem(int deviceId, int minutes, String startDate, String endDate) {
        return cpuMemMapper.findRecentByDeviceId(deviceId, minutes, startDate, endDate);
    }

    /**
     * 다중 장비의 CPU/MEM 시계열 조회 (batch)
     * granularity에 따라 raw/집계 자동 선택
     * 응답: {deviceId(String) → List<CpuMemVO>}
     */
    public java.util.Map<String, List<CpuMemVO>> getRecentCpuMemBatch(List<Integer> deviceIds, Integer minutes,
                                                                       String startDate, String endDate, String granularity) {
        java.util.Map<String, List<CpuMemVO>> result = new java.util.LinkedHashMap<>();
        if (deviceIds == null || deviceIds.isEmpty()) return result;
        for (Integer id : deviceIds) {
            result.put(String.valueOf(id), new ArrayList<>());
        }

        IcmpService.TimeRange range = IcmpService.resolveTimeRange(minutes, startDate, endDate);
        Integer intervalSec = IcmpService.resolveIntervalSec(granularity, range.start, range.end);

        List<CpuMemVO> rows;
        if (intervalSec == null || intervalSec <= 0) {
            rows = cpuMemMapper.findRecentBatchRaw(deviceIds, range.startStr, range.endStr);
        } else {
            rows = cpuMemMapper.findRecentBatchAggregated(deviceIds, range.startStr, range.endStr, intervalSec);
        }

        for (CpuMemVO vo : rows) {
            String key = String.valueOf(vo.getDEVICE_ID());
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 특정 장비의 코어별 CPU 데이터 조회
     */
    public List<CpuMemVO> getCpuCores(int deviceId) {
        return cpuMemMapper.findLatestCoresByDeviceId(deviceId);
    }

    // ==================== Device SSH (접속 정보) ====================

    /**
     * 장비 접속 정보 조회
     */
    public DeviceSshVO getDeviceSsh(int deviceId) {
        return deviceSshMapper.findByDeviceId(deviceId);
    }

    /**
     * 장비 접속 정보 저장/수정
     */
    @Transactional
    public DeviceSshVO saveOrUpdateDeviceSsh(DeviceSshVO ssh) {
        if (deviceSshMapper.existsByDeviceId(ssh.getDEVICE_ID())) {
            deviceSshMapper.updateDeviceSsh(ssh);
        } else {
            deviceSshMapper.insertDeviceSsh(ssh);
        }
        return deviceSshMapper.findByDeviceId(ssh.getDEVICE_ID());
    }

    /**
     * 장비 접속 정보 삭제 (soft delete)
     */
    @Transactional
    public void deleteDeviceSsh(int deviceId) {
        deviceSshMapper.deleteByDeviceId(deviceId);
    }

    // ==================== 연결성 체크 ====================

    /**
     * PING → SNMP → SSH 순차 연결성 체크 (Go Middleware API 활용)
     */
    public ConnectivityCheckVO connectivityCheck(int deviceId) {
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("장비를 찾을 수 없습니다: " + deviceId);
        }

        ConnectivityCheckVO result = new ConnectivityCheckVO();

        // 1. PING 체크 — Middleware /api/check/ping
        MiddlewareClient.PingResponse pingResp = middlewareClient.pingCheck(device.getDEVICE_IP(), deviceId);
        result.setPingSuccess(pingResp.isSuccess());
        result.setPingResponseTimeMs(Math.round(pingResp.getResponseTimeMs()));
        result.setPingMessage(pingResp.getMessage());

        // 2. SNMP 체크 — Middleware /api/snmp/system-info (설정이 있는 경우만)
        if (device.getSNMP_VERSION() != null && device.getSNMP_PORT() != null) {
            result.setSnmpConfigured(true);
            try {
                Map<String, String> sysInfo = middlewareClient.getDeviceSystemInfo(
                        device.getDEVICE_IP(),
                        device.getSNMP_VERSION(),
                        device.getSNMP_PORT(),
                        device.getSNMP_COMMUNITY(),
                        device.getSNMP_USER(),
                        device.getSNMP_AUTH_PROTOCOL(),
                        device.getSNMP_AUTH_PASSWORD(),
                        device.getSNMP_PRIV_PROTOCOL(),
                        device.getSNMP_PRIV_PASSWORD(),
                        deviceId
                );
                result.setSnmpSuccess(true);
                result.setSysName(sysInfo.get("sysName"));
                result.setSysDescr(sysInfo.get("sysDescr"));
                result.setSnmpMessage("SNMP 응답 성공 (v" + device.getSNMP_VERSION() + ", port " + device.getSNMP_PORT() + ")");
            } catch (Exception e) {
                result.setSnmpSuccess(false);
                result.setSnmpMessage(e.getMessage());
                log.warn("SNMP 체크 실패 - deviceId: {}, IP: {}, error: {}", deviceId, device.getDEVICE_IP(), e.getMessage());
            }
        } else {
            result.setSnmpConfigured(false);
            result.setSnmpMessage("SNMP 설정 없음");
        }

        // 3. SSH 포트 체크 — Middleware /api/check/ssh (설정이 있는 경우만)
        DeviceSshVO ssh = deviceSshMapper.findByDeviceId(deviceId);
        if (ssh != null) {
            result.setSshConfigured(true);
            int sshPort = ssh.getSSH_PORT() != null ? ssh.getSSH_PORT() : 22;
            result.setSshPort(sshPort);
            MiddlewareClient.SshCheckResponse sshResp = middlewareClient.sshCheck(device.getDEVICE_IP(), sshPort, deviceId);
            result.setSshSuccess(sshResp.isSuccess());
            result.setSshMessage(sshResp.getMessage());
        } else {
            result.setSshConfigured(false);
            result.setSshMessage("SSH 접속 정보 없음");
        }

        return result;
    }

    // ==================== 미들웨어 자동 할당 ====================

    /**
     * 활성 미들웨어 중 최적 미들웨어를 자동 선택하여 ID를 반환한다.
     * 선택 기준:
     *   1. ACTIVE 상태 미들웨어만 대상
     *   2. /health 엔드포인트 응답 성공 여부 (5초 타임아웃, 병렬 프로브)
     *   3. 응답 성공 중 최소 응답시간 기준 200ms 이내 = "비슷"
     *   4. 비슷한 후보 중 할당 장비 수 가장 적은 것
     *   5. 장비 수 동일 시 MIDDLEWARE_ID 낮은 것
     * @return 선택된 MIDDLEWARE_ID, 없으면 null
     */
    private Integer assignMiddleware() {
        List<MiddlewareVO> actives;
        try {
            actives = middlewareMapper.findAll().stream()
                    .filter(mw -> "ACTIVE".equals(mw.getSTATUS()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("미들웨어 목록 조회 실패: {}", e.getMessage());
            return null;
        }

        if (actives.isEmpty()) {
            log.warn("등록된 ACTIVE 미들웨어가 없습니다. MIDDLEWARE_ID=null 로 장비 등록.");
            return null;
        }
        if (actives.size() == 1) {
            return actives.get(0).getMIDDLEWARE_ID();
        }

        // N개: 병렬 헬스 프로브 후 최적 선택
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        record ProbeResult(MiddlewareVO mw, long responseMs) {}

        List<CompletableFuture<Optional<ProbeResult>>> futures = actives.stream()
                .map(mw -> CompletableFuture.supplyAsync(() -> {
                    String url = mw.getMIDDLEWARE_URL().replaceAll("/+$", "") + "/health";
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build();
                        long start = System.currentTimeMillis();
                        HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
                        long elapsed = System.currentTimeMillis() - start;
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            return Optional.of(new ProbeResult(mw, elapsed));
                        }
                        log.warn("미들웨어 헬스체크 비정상 응답 - id:{}, status:{}", mw.getMIDDLEWARE_ID(), resp.statusCode());
                        return Optional.<ProbeResult>empty();
                    } catch (Exception e) {
                        log.warn("미들웨어 헬스체크 실패 - id:{}, url:{}, error:{}", mw.getMIDDLEWARE_ID(), url, e.getMessage());
                        return Optional.<ProbeResult>empty();
                    }
                }, PROBE_EXECUTOR))
                .collect(Collectors.toList());

        List<ProbeResult> successful = futures.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (successful.isEmpty()) {
            // 모든 헬스체크 실패 - 장비 수 가장 적은 미들웨어에 로드 밸런싱 할당
            log.warn("모든 미들웨어 헬스체크 실패. 장비 수 기준 최소 부하 미들웨어에 할당.");
            return actives.stream()
                    .min(Comparator.comparingInt((MiddlewareVO m) -> m.getDEVICE_COUNT() != null ? m.getDEVICE_COUNT() : 0)
                            .thenComparingInt(MiddlewareVO::getMIDDLEWARE_ID))
                    .map(MiddlewareVO::getMIDDLEWARE_ID)
                    .orElse(null);
        }
        if (successful.size() == 1) {
            return successful.get(0).mw().getMIDDLEWARE_ID();
        }

        long minMs = successful.stream().mapToLong(ProbeResult::responseMs).min().orElse(0);

        // 최소 응답시간 기준 200ms 이내 후보 필터
        List<ProbeResult> candidates = successful.stream()
                .filter(p -> p.responseMs() - minMs <= 200)
                .collect(Collectors.toList());

        // 할당 장비 수 조회 후 정렬: 장비 수 오름차순 → MIDDLEWARE_ID 오름차순
        return candidates.stream()
                .min(Comparator
                        .comparingInt((ProbeResult p) -> {
                            try {
                                return middlewareMapper.countDevicesByMiddlewareId(p.mw().getMIDDLEWARE_ID());
                            } catch (Exception e) {
                                log.warn("장비 수 조회 실패 - middlewareId:{}", p.mw().getMIDDLEWARE_ID());
                                return Integer.MAX_VALUE;
                            }
                        })
                        .thenComparingInt(p -> p.mw().getMIDDLEWARE_ID()))
                .map(p -> p.mw().getMIDDLEWARE_ID())
                .orElse(null);
    }
}
