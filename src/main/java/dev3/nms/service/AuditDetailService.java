package dev3.nms.service;

import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.GroupMapper;
import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.mapper.ModelMapper;
import dev3.nms.mapper.PortMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.mapper.WatchMapper;
import dev3.nms.vo.auth.LoginHistoryVO;
import dev3.nms.vo.auth.UserVO;
import dev3.nms.vo.mgmt.*;
import dev3.nms.vo.watch.WatchGroupDeviceVO;
import dev3.nms.vo.watch.WatchGroupIfVO;
import dev3.nms.vo.watch.WatchGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 감사 로그 상세 정보 빌드 서비스.
 * 변경 전/후 스냅샷 비교 → 사람이 읽기 좋은 한국어 변경 내역 생성.
 *
 * 지원하는 전체 TARGET_TYPE (22종):
 *   DEVICE, GROUP, PORT, DEVICE_SCOPE, DEVICE_SNMP, DEVICE_SSH,
 *   MODEL, DEV_CODE, TEMP_DEVICE, NOTICE, BOARD_FILE,
 *   THRESHOLD, DEVICE_THRESHOLD,
 *   WATCH_GROUP, WATCH_CONTROL,
 *   USER_PERMISSION, USER_STATUS, USER_SETTING, USER_REVIEW,
 *   ACCOUNT, PASSWORD, DASHBOARD, TOPOLOGY, USER_TOPOLOGY,
 *   ERROR, MODEL_OID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditDetailService {

    private final DeviceMapper deviceMapper;
    private final GroupMapper groupMapper;
    private final PortMapper portMapper;
    private final ModelMapper modelMapper;
    private final WatchMapper watchMapper;
    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;

    // ==================== 스냅샷 조회 ====================

    public Map<String, String> getSnapshot(String targetType, String targetId, Object[] methodArgs) {
        if (targetId == null) return null;
        try {
            return switch (targetType) {
                case "DEVICE" -> getDeviceSnapshot(Integer.parseInt(targetId));
                case "GROUP" -> getGroupSnapshot(Integer.parseInt(targetId));
                case "PORT" -> getPortSnapshot(methodArgs);
                case "DEVICE_SCOPE" -> getScopeSnapshot(Integer.parseInt(targetId));
                case "DEVICE_SNMP" -> getDeviceSnmpSnapshot(Integer.parseInt(targetId));
                case "DEVICE_SSH" -> getDeviceSshSnapshot(Integer.parseInt(targetId));
                case "MODEL" -> getModelSnapshot(Integer.parseInt(targetId));
                case "WATCH_GROUP" -> getWatchGroupSnapshot(Integer.parseInt(targetId));
                case "WATCH_CONTROL" -> getWatchControlSnapshot(Integer.parseInt(targetId));
                case "TEMP_DEVICE" -> getDeviceSnapshot(Integer.parseInt(targetId)); // 동일 구조
                case "DEVICE_THRESHOLD" -> getDeviceSnapshot(Integer.parseInt(targetId));
                default -> null;
            };
        } catch (Exception e) {
            log.debug("[AuditDetail] 스냅샷 조회 실패: type={}, id={}", targetType, targetId);
            return null;
        }
    }

    // ==================== 새 값 추출 ====================

    public Map<String, String> extractNewValues(String targetType, Object requestBody) {
        if (requestBody == null) return null;
        try {
            return switch (targetType) {
                case "DEVICE" -> extractDeviceValues(requestBody);
                case "GROUP" -> extractGroupValues(requestBody);
                case "PORT" -> extractPortValues(requestBody);
                case "DEVICE_SCOPE" -> extractScopeValues(requestBody);
                case "DEVICE_SSH" -> extractSshValues(requestBody);
                case "MODEL" -> extractModelValues(requestBody);
                case "WATCH_GROUP" -> extractWatchGroupValues(requestBody);
                default -> extractMapValues(requestBody);
            };
        } catch (Exception e) {
            log.debug("[AuditDetail] 새 값 추출 실패: type={}", targetType);
            return null;
        }
    }

    // ==================== DETAIL 빌드 ====================

    public String buildCreateDetail(String targetType, Object requestBody) {
        String label = getTargetLabel(targetType);

        if (requestBody instanceof DeviceVO d) {
            return label + " 등록 - " + safe(d.getDEVICE_NAME()) + optIp(d.getDEVICE_IP());
        }
        if (requestBody instanceof TempDeviceVO t) {
            return label + " 등록 - " + safe(t.getDEVICE_NAME()) + optIp(t.getDEVICE_IP());
        }
        if (requestBody instanceof GroupVO g) {
            return label + " 생성 - " + safe(g.getGROUP_NAME());
        }
        if (requestBody instanceof ModelVO m) {
            return label + " 등록 - " + safe(m.getMODEL_NAME());
        }
        if (requestBody instanceof List<?> list) {
            return label + " 일괄 등록 (" + list.size() + "건)";
        }

        // Map 타입 (WatchGroup, 기타)
        if (requestBody instanceof Map<?,?> map) {
            // WATCH_GROUP: { group: { GROUP_NAME: ... }, devices: [...] }
            Object groupObj = map.get("group");
            if (groupObj instanceof Map<?,?> gm) {
                String groupName = (String) gm.get("GROUP_NAME");
                return buildWatchGroupCreateDetail(label, groupName, map);
            }
            // 일반 Map
            Object name = map.get("groupName");
            if (name == null) name = map.get("name");
            if (name == null) name = map.get("GROUP_NAME");
            if (name != null) return label + " 생성 - " + name;

            // groupIds (import)
            Object groupIds = map.get("groupIds");
            if (groupIds instanceof List<?> ids) {
                return label + " 가져오기 (" + ids.size() + "개 그룹)";
            }
        }

        return label + " 등록";
    }

    public String buildDeleteDetail(String targetType, Map<String, String> beforeSnapshot) {
        String label = getTargetLabel(targetType);
        if (beforeSnapshot == null) return label + " 삭제";
        String displayName = beforeSnapshot.get("_displayName");

        // WATCH_GROUP 삭제: 포함된 장비/포트 정보도 표시
        if ("WATCH_GROUP".equals(targetType)) {
            Set<Integer> deviceIds = extractDeviceIds(beforeSnapshot);
            if (!deviceIds.isEmpty()) {
                List<String> deviceDetails = new ArrayList<>();
                for (Integer devId : deviceIds) {
                    String devName = beforeSnapshot.getOrDefault("dev:" + devId, "장비#" + devId);
                    String portsStr = beforeSnapshot.getOrDefault("dev:" + devId + ":ports", "");
                    List<String> portNames = parsePortNames(portsStr);
                    if (!portNames.isEmpty()) {
                        deviceDetails.add(devName + "[" + String.join(", ", portNames) + "]");
                    } else {
                        deviceDetails.add(devName);
                    }
                }
                return label + " 삭제 - " + safe(displayName)
                        + " (장비: " + String.join(", ", deviceDetails) + ")";
            }
        }

        return displayName != null ? label + " 삭제 - " + displayName : label + " 삭제";
    }

    /**
     * WATCH_GROUP CREATE 시 장비/포트 상세 포함
     */
    @SuppressWarnings("unchecked")
    private String buildWatchGroupCreateDetail(String label, String groupName, Map<?,?> requestBody) {
        StringBuilder sb = new StringBuilder(label + " 생성 - " + safe(groupName));

        Object devicesObj = requestBody.get("devices");
        if (devicesObj instanceof List<?> deviceList && !deviceList.isEmpty()) {
            List<String> deviceDetails = new ArrayList<>();
            for (Object d : deviceList) {
                if (!(d instanceof Map<?,?> dm)) continue;
                Integer deviceId = (Integer) dm.get("DEVICE_ID");
                if (deviceId == null) continue;

                DeviceVO device = deviceMapper.findDeviceById(deviceId);
                String devName = device != null
                        ? safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP())
                        : "장비#" + deviceId;

                Object ifs = dm.get("interfaces");
                if (ifs instanceof List<?> ifList && !ifList.isEmpty()) {
                    List<String> portNames = new ArrayList<>();
                    for (Object ifObj : ifList) {
                        if (ifObj instanceof Map<?,?> im) {
                            Integer ifIndex = (Integer) im.get("IF_INDEX");
                            if (ifIndex != null) {
                                PortVO port = portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);
                                portNames.add(port != null && port.getIF_NAME() != null
                                        ? port.getIF_NAME() : "ifIndex:" + ifIndex);
                            }
                        }
                    }
                    deviceDetails.add(devName + "[" + String.join(", ", portNames) + "]");
                } else {
                    deviceDetails.add(devName);
                }
            }

            if (!deviceDetails.isEmpty()) {
                sb.append(" | 장비: ").append(String.join(", ", deviceDetails));
            }
        }

        return sb.toString();
    }

    public String buildUpdateDetail(String targetType, Map<String, String> before, Map<String, String> after) {
        if (before == null || after == null) return null;

        String displayName = before.get("_displayName");
        Map<String, String> fieldLabels = getFieldLabels(targetType);
        List<String> changes = new ArrayList<>();

        for (Map.Entry<String, String> entry : after.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;

            String lbl = fieldLabels.getOrDefault(key, key);
            String oldVal = before.getOrDefault(key, "");
            String newVal = entry.getValue() != null ? entry.getValue() : "";

            if (!Objects.equals(oldVal, newVal) && !newVal.isEmpty()) {
                if (oldVal.isEmpty()) {
                    changes.add(lbl + ": " + newVal);
                } else {
                    changes.add(lbl + ": " + oldVal + " → " + newVal);
                }
            }
        }

        if (changes.isEmpty()) {
            return displayName != null ? displayName + " 설정 변경" : getTargetLabel(targetType) + " 변경";
        }

        StringBuilder sb = new StringBuilder();
        if (displayName != null) sb.append(displayName).append(" - ");
        sb.append(String.join(", ", changes));
        return sb.toString();
    }

    // ==================== 장비 (DEVICE) ====================

    private Map<String, String> getDeviceSnapshot(int deviceId) {
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        if (device == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP()));
        map.put("DEVICE_NAME", safe(device.getDEVICE_NAME()));
        map.put("DEVICE_IP", safe(device.getDEVICE_IP()));
        map.put("DEVICE_DESC", safe(device.getDEVICE_DESC()));
        map.put("GROUP_ID", intStr(device.getGROUP_ID()));
        map.put("MODEL_ID", intStr(device.getMODEL_ID()));
        return map;
    }

    private Map<String, String> extractDeviceValues(Object body) {
        if (body instanceof DeviceVO d) {
            Map<String, String> map = new LinkedHashMap<>();
            if (d.getDEVICE_NAME() != null) map.put("DEVICE_NAME", d.getDEVICE_NAME());
            if (d.getDEVICE_IP() != null) map.put("DEVICE_IP", d.getDEVICE_IP());
            if (d.getDEVICE_DESC() != null) map.put("DEVICE_DESC", d.getDEVICE_DESC());
            if (d.getGROUP_ID() != null) map.put("GROUP_ID", String.valueOf(d.getGROUP_ID()));
            if (d.getMODEL_ID() != null) map.put("MODEL_ID", String.valueOf(d.getMODEL_ID()));
            return map;
        }
        return extractMapValues(body);
    }

    // ==================== 그룹 (GROUP) ====================

    private Map<String, String> getGroupSnapshot(int groupId) {
        GroupVO group = groupMapper.findGroupById(groupId).orElse(null);
        if (group == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(group.getGROUP_NAME()));
        map.put("GROUP_NAME", safe(group.getGROUP_NAME()));
        map.put("ADDRESS", safe(group.getADDRESS()));
        map.put("PHONE", safe(group.getPHONE()));
        return map;
    }

    private Map<String, String> extractGroupValues(Object body) {
        if (body instanceof GroupVO g) {
            Map<String, String> map = new LinkedHashMap<>();
            if (g.getGROUP_NAME() != null) map.put("GROUP_NAME", g.getGROUP_NAME());
            if (g.getADDRESS() != null) map.put("ADDRESS", g.getADDRESS());
            if (g.getPHONE() != null) map.put("PHONE", g.getPHONE());
            return map;
        }
        return extractMapValues(body);
    }

    // ==================== 포트 (PORT) ====================

    private Map<String, String> getPortSnapshot(Object[] methodArgs) {
        Integer deviceId = null, ifIndex = null;
        for (Object arg : methodArgs) {
            if (arg instanceof Integer val) {
                if (deviceId == null) deviceId = val;
                else ifIndex = val;
            }
        }
        if (deviceId == null || ifIndex == null) return null;

        PortVO port = portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);
        if (port == null) return null;

        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        String deviceDisplay = device != null
                ? safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP())
                : "장비#" + deviceId;

        Map<String, String> map = new LinkedHashMap<>();
        String portName = port.getIF_NAME() != null ? port.getIF_NAME() : "ifIndex:" + ifIndex;
        map.put("_displayName", deviceDisplay + " [" + portName + "]");
        map.put("IF_OPER_FLAG", boolStr(port.getIF_OPER_FLAG(), "활성", "비활성"));
        map.put("IF_PERF_FLAG", boolStr(port.getIF_PERF_FLAG(), "활성", "비활성"));
        map.put("IF_DESCRIPTION", safe(port.getIF_DESCRIPTION()));
        return map;
    }

    private Map<String, String> extractPortValues(Object body) {
        if (body instanceof PortVO p) {
            Map<String, String> map = new LinkedHashMap<>();
            if (p.getIF_OPER_FLAG() != null) map.put("IF_OPER_FLAG", boolStr(p.getIF_OPER_FLAG(), "활성", "비활성"));
            if (p.getIF_PERF_FLAG() != null) map.put("IF_PERF_FLAG", boolStr(p.getIF_PERF_FLAG(), "활성", "비활성"));
            if (p.getIF_DESCRIPTION() != null) map.put("IF_DESCRIPTION", p.getIF_DESCRIPTION());
            return map;
        }
        if (body instanceof Map<?,?> raw) {
            Map<String, String> map = new LinkedHashMap<>();
            Object admin = raw.get("monitorAdmin");
            Object oper = raw.get("monitorOper");
            if (admin != null) map.put("IF_PERF_FLAG", boolStr(admin, "활성", "비활성"));
            if (oper != null) map.put("IF_OPER_FLAG", boolStr(oper, "활성", "비활성"));
            return map;
        }
        return null;
    }

    // ==================== 수집 설정 (DEVICE_SCOPE) ====================

    private Map<String, String> getScopeSnapshot(int deviceId) {
        DeviceScopeVO scope = deviceMapper.findDeviceScopeById(deviceId);
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        String display = device != null
                ? safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP())
                : "장비#" + deviceId;

        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", display);
        if (scope != null) {
            map.put("COLLECT_PING", boolStr(scope.getCOLLECT_PING(), "ON", "OFF"));
            map.put("COLLECT_SNMP", boolStr(scope.getCOLLECT_SNMP(), "ON", "OFF"));
            map.put("COLLECT_AGENT", boolStr(scope.getCOLLECT_AGENT(), "ON", "OFF"));
        }
        return map;
    }

    private Map<String, String> extractScopeValues(Object body) {
        if (!(body instanceof DeviceScopeVO s)) return null;
        Map<String, String> map = new LinkedHashMap<>();
        if (s.getCOLLECT_PING() != null) map.put("COLLECT_PING", boolStr(s.getCOLLECT_PING(), "ON", "OFF"));
        if (s.getCOLLECT_SNMP() != null) map.put("COLLECT_SNMP", boolStr(s.getCOLLECT_SNMP(), "ON", "OFF"));
        if (s.getCOLLECT_AGENT() != null) map.put("COLLECT_AGENT", boolStr(s.getCOLLECT_AGENT(), "ON", "OFF"));
        return map;
    }

    // ==================== SNMP 설정 (DEVICE_SNMP) ====================

    private Map<String, String> getDeviceSnmpSnapshot(int deviceId) {
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        if (device == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP()));
        map.put("SNMP_VERSION", device.getSNMP_VERSION() != null ? "v" + device.getSNMP_VERSION() : "");
        map.put("SNMP_PORT", intStr(device.getSNMP_PORT()));
        map.put("SNMP_COMMUNITY", safe(device.getSNMP_COMMUNITY()));
        return map;
    }

    // ==================== SSH 설정 (DEVICE_SSH) ====================

    private Map<String, String> getDeviceSshSnapshot(int deviceId) {
        DeviceVO device = deviceMapper.findDeviceById(deviceId);
        Map<String, String> map = new LinkedHashMap<>();
        String display = device != null
                ? safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP())
                : "장비#" + deviceId;
        map.put("_displayName", display);
        return map;
    }

    private Map<String, String> extractSshValues(Object body) {
        if (!(body instanceof DeviceSshVO s)) return null;
        Map<String, String> map = new LinkedHashMap<>();
        if (s.getSSH_USER() != null) map.put("SSH_USER", s.getSSH_USER());
        if (s.getSSH_PORT() != null) map.put("SSH_PORT", String.valueOf(s.getSSH_PORT()));
        if (s.getCONNECT_AS() != null) map.put("CONNECT_AS", s.getCONNECT_AS());
        return map;
    }

    // ==================== 모델 (MODEL) ====================

    private Map<String, String> getModelSnapshot(int modelId) {
        ModelVO model = modelMapper.findById(modelId).orElse(null);
        if (model == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(model.getMODEL_NAME()));
        map.put("MODEL_NAME", safe(model.getMODEL_NAME()));
        map.put("MODEL_OID", safe(model.getMODEL_OID()));
        return map;
    }

    private Map<String, String> extractModelValues(Object body) {
        if (!(body instanceof ModelVO m)) return null;
        Map<String, String> map = new LinkedHashMap<>();
        if (m.getMODEL_NAME() != null) map.put("MODEL_NAME", m.getMODEL_NAME());
        if (m.getMODEL_OID() != null) map.put("MODEL_OID", m.getMODEL_OID());
        return map;
    }

    // ==================== 관제 그룹 (WATCH_GROUP) ====================
    // 스냅샷 키 구조:
    //   GROUP_NAME, INTERVAL_SEC - 그룹 레벨
    //   dev:{deviceId}           - 장비 표시명 (예: "코스콤서버(192.168.3.13)")
    //   dev:{deviceId}:ports     - 포트 목록 (예: "1:GigabitEthernet0/1,5:GigabitEthernet0/5")

    private Map<String, String> getWatchGroupSnapshot(int watchGroupId) {
        WatchGroupVO group = watchMapper.findGroupById(watchGroupId);
        if (group == null) return null;

        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(group.getGROUP_NAME()));
        map.put("GROUP_NAME", safe(group.getGROUP_NAME()));
        map.put("INTERVAL_SEC", group.getINTERVAL_SEC() != null ? group.getINTERVAL_SEC() + "초" : "");

        // 장비 레벨
        List<WatchGroupDeviceVO> devices = watchMapper.findDevicesByGroupId(watchGroupId);
        // 포트 레벨 (전체 일괄 조회)
        List<WatchGroupIfVO> allInterfaces = watchMapper.findInterfacesByGroupId(watchGroupId);
        // 포트를 deviceId별로 그룹핑
        Map<Integer, List<WatchGroupIfVO>> ifByDevice = allInterfaces != null
                ? allInterfaces.stream().collect(Collectors.groupingBy(WatchGroupIfVO::getDEVICE_ID))
                : Map.of();

        if (devices != null) {
            for (WatchGroupDeviceVO d : devices) {
                String devKey = "dev:" + d.getDEVICE_ID();
                map.put(devKey, safe(d.getDEVICE_NAME()) + optIp(d.getDEVICE_IP()));

                // 해당 장비의 포트 목록
                List<WatchGroupIfVO> ports = ifByDevice.getOrDefault(d.getDEVICE_ID(), List.of());
                if (!ports.isEmpty()) {
                    String portStr = ports.stream()
                            .map(p -> {
                                // IF_NAME 우선, 없으면 IF_DESCR, 없으면 portMapper로 조회
                                String name = p.getIF_NAME();
                                if (name == null || name.isEmpty()) name = p.getIF_DESCR();
                                if (name == null || name.isEmpty()) {
                                    PortVO portVO = portMapper.findByDeviceIdAndIfIndex(d.getDEVICE_ID(), p.getIF_INDEX());
                                    if (portVO != null) name = portVO.getIF_NAME();
                                }
                                if (name == null || name.isEmpty()) name = "ifIndex:" + p.getIF_INDEX();
                                return p.getIF_INDEX() + ":" + name;
                            })
                            .collect(Collectors.joining(","));
                    map.put(devKey + ":ports", portStr);
                }
            }
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractWatchGroupValues(Object body) {
        if (!(body instanceof Map<?,?> raw)) return null;
        Map<String, String> map = new LinkedHashMap<>();

        // 그룹 레벨
        Object groupObj = raw.get("group");
        if (groupObj instanceof Map<?,?> gm) {
            Object name = gm.get("GROUP_NAME");
            Object interval = gm.get("INTERVAL_SEC");
            if (name != null) map.put("GROUP_NAME", String.valueOf(name));
            if (interval != null) map.put("INTERVAL_SEC", interval + "초");
        }

        // 장비 + 포트 레벨
        Object devicesObj = raw.get("devices");
        if (devicesObj instanceof List<?> deviceList) {
            for (Object d : deviceList) {
                if (!(d instanceof Map<?,?> dm)) continue;
                Integer deviceId = (Integer) dm.get("DEVICE_ID");
                if (deviceId == null) continue;

                String devKey = "dev:" + deviceId;

                // 장비명 조회
                DeviceVO device = deviceMapper.findDeviceById(deviceId);
                map.put(devKey, device != null
                        ? safe(device.getDEVICE_NAME()) + optIp(device.getDEVICE_IP())
                        : "장비#" + deviceId);

                // 포트 목록
                Object ifs = dm.get("interfaces");
                if (ifs instanceof List<?> ifList && !ifList.isEmpty()) {
                    List<String> portEntries = new ArrayList<>();
                    for (Object ifObj : ifList) {
                        if (ifObj instanceof Map<?,?> im) {
                            Integer ifIndex = (Integer) im.get("IF_INDEX");
                            if (ifIndex != null) {
                                // 포트명 조회
                                PortVO port = portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);
                                String portName = port != null && port.getIF_NAME() != null
                                        ? port.getIF_NAME() : "ifIndex:" + ifIndex;
                                portEntries.add(ifIndex + ":" + portName);
                            }
                        }
                    }
                    if (!portEntries.isEmpty()) {
                        map.put(devKey + ":ports", String.join(",", portEntries));
                    }
                }
            }
        }

        // 이동/아이콘 단순 변경
        Object parentId = raw.get("PARENT_GROUP_ID");
        if (parentId != null) map.put("PARENT_GROUP_ID", String.valueOf(parentId));
        Object iconName = raw.get("ICON_NAME");
        if (iconName != null) map.put("ICON_NAME", String.valueOf(iconName));

        return map;
    }

    // ==================== 관제 시작/중지 (WATCH_CONTROL) ====================

    private Map<String, String> getWatchControlSnapshot(int watchGroupId) {
        WatchGroupVO group = watchMapper.findGroupById(watchGroupId);
        if (group == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("_displayName", safe(group.getGROUP_NAME()));
        return map;
    }

    // ==================== 범용 Map 추출 ====================

    @SuppressWarnings("unchecked")
    private Map<String, String> extractMapValues(Object body) {
        if (!(body instanceof Map)) return null;
        Map<String, Object> raw = (Map<String, Object>) body;
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (e.getValue() != null && !(e.getValue() instanceof Map) && !(e.getValue() instanceof List)) {
                map.put(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        return map;
    }

    // ==================== WATCH_GROUP UPDATE 전용 DETAIL 빌드 ====================

    /**
     * WATCH_GROUP UPDATE 시 그룹/장비/포트 3레벨 변경분 추적
     *
     * 출력 예시:
     *   1층 네트워크 - 그룹명: 1층 네트워크 → 1층 스위치,
     *   장비 추가: 방화벽(10.0.0.2),
     *   장비 제거: 프린터(10.0.0.5),
     *   코스콤서버(192.168.3.13) 포트 추가: GigabitEthernet0/3,
     *   L3스위치(10.0.0.1) 포트 제거: Fa0/5
     */
    public String buildWatchGroupUpdateDetail(Map<String, String> before, Map<String, String> after) {
        if (before == null || after == null) return null;

        String displayName = before.get("_displayName");
        List<String> changes = new ArrayList<>();

        // ── 1. 그룹 레벨 변경 ──
        diffField(before, after, "GROUP_NAME", "그룹명", changes);
        diffField(before, after, "INTERVAL_SEC", "수집 주기", changes);

        String newIcon = after.getOrDefault("ICON_NAME", "");
        if (!newIcon.isEmpty()) changes.add("아이콘: " + newIcon);

        String parentId = after.getOrDefault("PARENT_GROUP_ID", "");
        if (!parentId.isEmpty()) changes.add("그룹 이동");

        // ── 2. 장비 레벨 변경 (dev:* 키 비교) ──
        Set<Integer> oldDeviceIds = extractDeviceIds(before);
        Set<Integer> newDeviceIds = extractDeviceIds(after);

        Set<Integer> addedDevices = new LinkedHashSet<>(newDeviceIds);
        addedDevices.removeAll(oldDeviceIds);

        Set<Integer> removedDevices = new LinkedHashSet<>(oldDeviceIds);
        removedDevices.removeAll(newDeviceIds);

        Set<Integer> keptDevices = new LinkedHashSet<>(oldDeviceIds);
        keptDevices.retainAll(newDeviceIds);

        if (!addedDevices.isEmpty()) {
            String names = addedDevices.stream()
                    .map(id -> after.getOrDefault("dev:" + id, "장비#" + id))
                    .collect(Collectors.joining(", "));
            changes.add("장비 추가: " + names);
        }

        if (!removedDevices.isEmpty()) {
            String names = removedDevices.stream()
                    .map(id -> before.getOrDefault("dev:" + id, "장비#" + id))
                    .collect(Collectors.joining(", "));
            changes.add("장비 제거: " + names);
        }

        // ── 3. 포트 레벨 변경 (유지된 장비 + 추가된 장비의 포트) ──
        // 추가된 장비의 포트 (전체가 신규)
        for (Integer devId : addedDevices) {
            String devName = after.getOrDefault("dev:" + devId, "장비#" + devId);
            String newPorts = after.getOrDefault("dev:" + devId + ":ports", "");
            if (!newPorts.isEmpty()) {
                List<String> portNames = parsePortNames(newPorts);
                changes.add(devName + " 포트 지정: " + String.join(", ", portNames));
            }
        }

        // 유지된 장비의 포트 변경분
        for (Integer devId : keptDevices) {
            String devName = before.getOrDefault("dev:" + devId, after.getOrDefault("dev:" + devId, "장비#" + devId));
            String oldPortsStr = before.getOrDefault("dev:" + devId + ":ports", "");
            String newPortsStr = after.getOrDefault("dev:" + devId + ":ports", "");

            if (Objects.equals(oldPortsStr, newPortsStr)) continue;

            Set<String> oldIfIndexes = parseIfIndexes(oldPortsStr);
            Set<String> newIfIndexes = parseIfIndexes(newPortsStr);
            Map<String, String> oldPortNameMap = parsePortNameMap(oldPortsStr);
            Map<String, String> newPortNameMap = parsePortNameMap(newPortsStr);

            Set<String> addedPorts = new LinkedHashSet<>(newIfIndexes);
            addedPorts.removeAll(oldIfIndexes);

            Set<String> removedPorts = new LinkedHashSet<>(oldIfIndexes);
            removedPorts.removeAll(newIfIndexes);

            if (!addedPorts.isEmpty()) {
                String names = addedPorts.stream()
                        .map(idx -> newPortNameMap.getOrDefault(idx, "ifIndex:" + idx))
                        .collect(Collectors.joining(", "));
                changes.add(devName + " 포트 추가: " + names);
            }

            if (!removedPorts.isEmpty()) {
                String names = removedPorts.stream()
                        .map(idx -> oldPortNameMap.getOrDefault(idx, "ifIndex:" + idx))
                        .collect(Collectors.joining(", "));
                changes.add(devName + " 포트 제거: " + names);
            }
        }

        if (changes.isEmpty()) {
            return displayName != null ? displayName + " 설정 변경" : "관제 그룹 변경";
        }

        StringBuilder sb = new StringBuilder();
        if (displayName != null) sb.append(displayName).append(" - ");
        sb.append(String.join(", ", changes));
        return sb.toString();
    }

    // ── WATCH_GROUP 헬퍼 ──

    /** Map에서 "dev:{id}" 키의 deviceId 추출 */
    private Set<Integer> extractDeviceIds(Map<String, String> map) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (String key : map.keySet()) {
            if (key.startsWith("dev:") && !key.contains(":ports")) {
                try { ids.add(Integer.parseInt(key.substring(4))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return ids;
    }

    /** "1:GigabitEthernet0/1,5:GigabitEthernet0/5" → ifIndex Set {"1","5"} */
    private Set<String> parseIfIndexes(String portsStr) {
        if (portsStr == null || portsStr.isEmpty()) return Set.of();
        Set<String> set = new LinkedHashSet<>();
        for (String entry : portsStr.split(",")) {
            int colon = entry.indexOf(':');
            if (colon > 0) set.add(entry.substring(0, colon));
        }
        return set;
    }

    /** "1:GigabitEthernet0/1,5:GigabitEthernet0/5" → {"1" → "GigabitEthernet0/1", ...} */
    private Map<String, String> parsePortNameMap(String portsStr) {
        if (portsStr == null || portsStr.isEmpty()) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        for (String entry : portsStr.split(",")) {
            int colon = entry.indexOf(':');
            if (colon > 0) map.put(entry.substring(0, colon), entry.substring(colon + 1));
        }
        return map;
    }

    /** "1:GigabitEthernet0/1,5:GigabitEthernet0/5" → ["GigabitEthernet0/1","GigabitEthernet0/5"] */
    private List<String> parsePortNames(String portsStr) {
        if (portsStr == null || portsStr.isEmpty()) return List.of();
        List<String> names = new ArrayList<>();
        for (String entry : portsStr.split(",")) {
            int colon = entry.indexOf(':');
            names.add(colon > 0 ? entry.substring(colon + 1) : entry);
        }
        return names;
    }

    /** 단순 필드 비교 → changes 리스트에 추가 */
    private void diffField(Map<String, String> before, Map<String, String> after,
                           String key, String label, List<String> changes) {
        String oldVal = before.getOrDefault(key, "");
        String newVal = after.getOrDefault(key, "");
        if (!newVal.isEmpty() && !Objects.equals(oldVal, newVal)) {
            if (oldVal.isEmpty()) {
                changes.add(label + ": " + newVal);
            } else {
                changes.add(label + ": " + oldVal + " → " + newVal);
            }
        }
    }

    // ==================== 필드 라벨 ====================

    private Map<String, String> getFieldLabels(String targetType) {
        Map<String, String> labels = new HashMap<>();
        switch (targetType) {
            case "DEVICE", "TEMP_DEVICE" -> {
                labels.put("DEVICE_NAME", "장비명");
                labels.put("DEVICE_IP", "IP");
                labels.put("DEVICE_DESC", "설명");
                labels.put("GROUP_ID", "그룹");
                labels.put("MODEL_ID", "모델");
            }
            case "GROUP" -> {
                labels.put("GROUP_NAME", "그룹명");
                labels.put("ADDRESS", "주소");
                labels.put("PHONE", "연락처");
                labels.put("ICON_NAME", "아이콘");
                labels.put("PARENT_GROUP_ID", "상위 그룹");
            }
            case "PORT" -> {
                labels.put("IF_OPER_FLAG", "Oper 감시");
                labels.put("IF_PERF_FLAG", "성능 감시");
                labels.put("IF_DESCRIPTION", "설명");
            }
            case "DEVICE_SCOPE" -> {
                labels.put("COLLECT_PING", "Ping 수집");
                labels.put("COLLECT_SNMP", "SNMP 수집");
                labels.put("COLLECT_AGENT", "Agent 수집");
            }
            case "DEVICE_SNMP" -> {
                labels.put("SNMP_VERSION", "SNMP 버전");
                labels.put("SNMP_PORT", "SNMP 포트");
                labels.put("SNMP_COMMUNITY", "커뮤니티");
            }
            case "DEVICE_SSH" -> {
                labels.put("SSH_USER", "SSH 사용자");
                labels.put("SSH_PORT", "SSH 포트");
                labels.put("CONNECT_AS", "접속 방식");
            }
            case "MODEL" -> {
                labels.put("MODEL_NAME", "모델명");
                labels.put("MODEL_OID", "모델 OID");
            }
            case "WATCH_GROUP" -> {
                labels.put("GROUP_NAME", "그룹명");
                labels.put("INTERVAL_SEC", "수집 주기");
                labels.put("ICON_NAME", "아이콘");
                labels.put("PARENT_GROUP_ID", "상위 그룹");
            }
            case "ACCOUNT" -> {
                labels.put("NAME", "이름");
                labels.put("EMAIL", "이메일");
                labels.put("PHONE", "전화번호");
            }
            case "USER_STATUS" -> {
                labels.put("STATUS", "계정 상태");
            }
            case "USER_SETTING" -> {
                labels.put("ALL_GROUP_VIEW", "전체 그룹 조회");
            }
            default -> {
                labels.put("groupName", "그룹명");
                labels.put("name", "이름");
                labels.put("intervalSec", "수집 주기(초)");
            }
        }
        return labels;
    }

    // ==================== TARGET_TYPE → 한국어 라벨 ====================

    /**
     * targetType + targetId로 DB에서 실제 표시명을 조회한다.
     * ViewLogAspect에서 VIEW 액션 기록 시 사용.
     *
     * NEW 엔티티 추가 시 이 메서드에 케이스를 반드시 추가해야 한다.
     * (docs/RULES.md 참고)
     */
    public String getDisplayName(String targetType, String targetId) {
        if (targetType == null || targetId == null) return null;
        try {
            long id = Long.parseLong(targetId);
            return switch (targetType) {
                case "DEVICE", "TEMP_DEVICE", "DEVICE_SCOPE", "DEVICE_SNMP", "DEVICE_SSH", "DEVICE_THRESHOLD" -> {
                    DeviceVO d = deviceMapper.findDeviceById((int) id);
                    yield d != null ? safe(d.getDEVICE_NAME()) + optIp(d.getDEVICE_IP()) : null;
                }
                case "GROUP" -> {
                    GroupVO g = groupMapper.findGroupById((int) id).orElse(null);
                    yield g != null ? g.getGROUP_NAME() : null;
                }
                case "MODEL", "MODEL_OID" -> {
                    ModelVO m = modelMapper.findById((int) id).orElse(null);
                    yield m != null ? m.getMODEL_NAME() : null;
                }
                case "WATCH_GROUP", "WATCH_CONTROL" -> {
                    WatchGroupVO w = watchMapper.findGroupById((int) id);
                    yield w != null ? w.getGROUP_NAME() : null;
                }
                case "USER_PERMISSION", "USER_STATUS", "USER_SETTING", "USER_REVIEW", "ACCOUNT", "PASSWORD" -> {
                    UserVO u = userMapper.findById(id).orElse(null);
                    yield u != null ? u.getNAME() + "(" + safe(u.getLOGIN_ID()) + ")" : null;
                }
                case "LOGIN_HISTORY" -> {
                    LoginHistoryVO h = loginHistoryMapper.findById(id);
                    yield h != null ? h.getUSER_NAME() + " 세션(" + h.getHISTORY_ID() + ")" : null;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.debug("[AuditDetail] 표시명 조회 실패: type={}, id={}", targetType, targetId);
            return null;
        }
    }

    public String getTargetLabel(String targetType) {
        return switch (targetType) {
            case "DEVICE" -> "장비";
            case "GROUP" -> "그룹";
            case "PORT" -> "포트 설정";
            case "DEVICE_SCOPE" -> "수집 설정";
            case "DEVICE_SNMP" -> "SNMP 설정";
            case "DEVICE_SSH" -> "SSH 접속 정보";
            case "MODEL" -> "모델";
            case "DEV_CODE" -> "장비 분류";
            case "TEMP_DEVICE" -> "신규 자산";
            case "NOTICE" -> "공지사항";
            case "BOARD_FILE" -> "자료실";
            case "THRESHOLD" -> "시스템 임계치";
            case "DEVICE_THRESHOLD" -> "장비별 임계치";
            case "WATCH_GROUP" -> "관제 그룹";
            case "WATCH_CONTROL" -> "관제 수집";
            case "ERROR" -> "장애 인지";
            case "USER_PERMISSION" -> "사용자 권한";
            case "USER_STATUS" -> "사용자 상태";
            case "USER_SETTING" -> "사용자 설정";
            case "USER_REVIEW" -> "가입 심사";
            case "ACCOUNT" -> "계정 정보";
            case "PASSWORD" -> "비밀번호";
            case "DASHBOARD" -> "대시보드";
            case "TOPOLOGY" -> "토폴로지";
            case "USER_TOPOLOGY" -> "사용자 토폴로지";
            case "MODEL_OID" -> "모델 OID";
            case "MIDDLEWARE" -> "미들웨어";
            default -> targetType;
        };
    }

    // ==================== 유틸 ====================

    private String safe(String val) { return val != null ? val : ""; }
    private String optIp(String ip) { return ip != null && !ip.isEmpty() ? "(" + ip + ")" : ""; }
    private String intStr(Integer val) { return val != null ? String.valueOf(val) : ""; }

    private String boolStr(Object val, String trueLabel, String falseLabel) {
        if (val == null) return "";
        if (val instanceof Boolean b) return b ? trueLabel : falseLabel;
        return String.valueOf(val);
    }
}
