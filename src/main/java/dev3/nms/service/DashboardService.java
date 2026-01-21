package dev3.nms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev3.nms.mapper.DashboardMapper;
import dev3.nms.vo.dashboard.DashboardDto;
import dev3.nms.vo.topo.TopoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.tags.BindErrorsTag;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;


    public List<DashboardDto.WidgetsRes> getWidgets() {
        try {
            return dashboardMapper.getWidgets();
        } catch (Exception e) {
            log.warn("위젯 조회 실패 - {}", e.getMessage());
            return Collections.singletonList(DashboardDto.WidgetsRes.builder().build());
        }
    }

    public List<DashboardDto.DefaultWidgetRes> getDefaultWidget() {
        try {
            List<DashboardDto.DefaultWidgetRes> defaultWidgetList = dashboardMapper.getDefaultWidget();
            ObjectMapper om = new ObjectMapper();
            for (DashboardDto.DefaultWidgetRes defaultWidget : defaultWidgetList) {
                DashboardDto.UserWidgetConfig userWidgetConfig = om.readValue(defaultWidget.getConfig(), DashboardDto.UserWidgetConfig.class);

                // 그룹구분
                if ("CPU_MEM".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toCpuMemMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid CPU_MEM elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetCpuMemPieChartData(param);
                        defaultWidget.setChartData(pieChartData);
                    } else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetCpuMemLineChartData(param);
                        defaultWidget.setChartData(lineChartData);
                    } else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetCpuMemBarChartData(param);
                        defaultWidget.setChartData(barChartData);
                    }
                }else if ("FILE".equals(userWidgetConfig.getGroup())) {

                }else if ("PROCESS".equals(userWidgetConfig.getGroup())) {

                }else if ("TRAFFIC".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toTrafficMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);   // [{key=TRAFFIC_IN_BPS,col=...}, ...]
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetTrafficPieChartData(param);
                        defaultWidget.setChartData(pieChartData);
                    }else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetTrafficLineChartData(param);
                        defaultWidget.setChartData(lineChartData);
                    }else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetTrafficBarChartData(param);
                        defaultWidget.setChartData(barChartData);
                    }
                }else if ("ICMP".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);   // [{key=ICMP_MAX,col=...}, ...]
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetIcmpPieChartData(param);
                        defaultWidget.setChartData(pieChartData);
                    }else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetIcmpLineChartData(param);
                        defaultWidget.setChartData(lineChartData);
                    }else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetIcmpBarChartData(param);
                        defaultWidget.setChartData(barChartData);
                    }
                }
                // 장애 현황
                if ("ALERT_SUMMARY".equals(defaultWidget.getWidgetCode())) {
                    DashboardDto.WidgetAlertCntData cntData = dashboardMapper.getWidgetAlertSummary();
                    defaultWidget.setCntData(cntData);
                }else if ("DEVICE_SUMMARY".equals(defaultWidget.getWidgetCode())) {
                    List<DashboardDto.DevCodeData> devCodeList =  dashboardMapper.getDevCode();
                    DashboardDto.WidgetDeviceCntData deviceCnt = new DashboardDto.WidgetDeviceCntData();

                    for (DashboardDto.DevCodeData devCode : devCodeList) {
                        List<Long> devCodeIdList = dashboardMapper.getDevCodeAllId(devCode.getDevCodeId()); // 하위 장비코드조회
                        Integer count = dashboardMapper.getDeviceCountBydevCodeId(devCodeIdList);

                        if ("네트워크".equals(devCode.getCodeNm())) {
                            deviceCnt.setNetworkCnt(count);
                        }else if ("서버".equals(devCode.getCodeNm())) {
                            deviceCnt.setServerCnt(count);
                        }else if ("전송".equals(devCode.getCodeNm())) {
                            deviceCnt.setTranCnt(count);
                        }else if ("FMS".equals(devCode.getCodeNm())) {
                            deviceCnt.setFmsCnt(count);
                        }
                    }
                    defaultWidget.setCntData(deviceCnt);
                }
            }
            return defaultWidgetList;
        } catch (Exception e) {
            log.warn("대시보드 기본 위젯 조회 실패 - {}", e.getMessage());
            return Collections.singletonList(DashboardDto.DefaultWidgetRes.builder().build());
        }
    }

    public List<DashboardDto.UserWidgetRes> getUserWidget(Long userId) {
        try {
            List<DashboardDto.UserWidgetRes> userWidgets = dashboardMapper.getUserWidget(userId);
            ObjectMapper om = new ObjectMapper();
            for (DashboardDto.UserWidgetRes userWidget : userWidgets) {
                DashboardDto.UserWidgetConfig userWidgetConfig = om.readValue(userWidget.getConfig(), DashboardDto.UserWidgetConfig.class);

                // 그룹구분
                if ("CPU_MEM".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toCpuMemMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid CPU_MEM elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetCpuMemPieChartData(param);
                        userWidget.setChartData(pieChartData);
                    } else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetCpuMemLineChartData(param);
                        userWidget.setChartData(lineChartData);
                    } else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetCpuMemBarChartData(param);
                        userWidget.setChartData(barChartData);
                    }
                }else if ("FILE".equals(userWidgetConfig.getGroup())) {

                }else if ("PROCESS".equals(userWidgetConfig.getGroup())) {

                }else if ("TRAFFIC".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toTrafficMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);   // [{key=TRAFFIC_IN_BPS,col=...}, ...]
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetTrafficPieChartData(param);
                        userWidget.setChartData(pieChartData);
                    }else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetTrafficLineChartData(param);
                        userWidget.setChartData(lineChartData);
                    }else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetTrafficBarChartData(param);
                        userWidget.setChartData(barChartData);
                    }
                }else if ("ICMP".equals(userWidgetConfig.getGroup())) {
                    List<Map<String, String>> metrics = toMetricParams(userWidgetConfig.getElements());
                    if (metrics.isEmpty()) throw new IllegalArgumentException("No valid elements");

                    Map<String, Object> param = new HashMap<>();
                    param.put("metrics", metrics);   // [{key=ICMP_MAX,col=...}, ...]
                    param.put("topN", 10);
                    param.put("intervalSec", 300);

                    if ("pie".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetIcmpPieChartData(param);
                        userWidget.setChartData(pieChartData);
                    }else if ("line".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetIcmpLineChartData(param);
                        userWidget.setChartData(lineChartData);
                    }else if ("bar".equals(userWidgetConfig.getChartType())) {
                        List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetIcmpBarChartData(param);
                        userWidget.setChartData(barChartData);
                    }
                }
                // 장애 현황
                if ("ALERT_SUMMARY".equals(userWidget.getWidgetCode())) {
                    DashboardDto.WidgetAlertCntData cntData = dashboardMapper.getWidgetAlertSummary();
                    userWidget.setCntData(cntData);
                }else if ("DEVICE_SUMMARY".equals(userWidget.getWidgetCode())) {
                    List<DashboardDto.DevCodeData> devCodeList =  dashboardMapper.getDevCode();
                    DashboardDto.WidgetDeviceCntData deviceCnt = new DashboardDto.WidgetDeviceCntData();

                    for (DashboardDto.DevCodeData devCode : devCodeList) {
                        List<Long> devCodeIdList = dashboardMapper.getDevCodeAllId(devCode.getDevCodeId()); // 하위 장비코드조회
                        Integer count = dashboardMapper.getDeviceCountBydevCodeId(devCodeIdList);

                        if ("네트워크".equals(devCode.getCodeNm())) {
                            deviceCnt.setNetworkCnt(count);
                        }else if ("서버".equals(devCode.getCodeNm())) {
                            deviceCnt.setServerCnt(count);
                        }else if ("전송".equals(devCode.getCodeNm())) {
                            deviceCnt.setTranCnt(count);
                        }else if ("FMS".equals(devCode.getCodeNm())) {
                            deviceCnt.setFmsCnt(count);
                        }
                    }
                    userWidget.setCntData(deviceCnt);
                }
            }
            
            return userWidgets;
        }catch (Exception e) {
            log.warn("대시보드 사용자 위젯 조회 실패 - {}", e.getMessage());
            return Collections.singletonList(DashboardDto.UserWidgetRes.builder().build());
        }
    }

    @Transactional
    public Boolean putUserWidget(Long userId, DashboardDto.UpdateUserWidgetsReq req) {
        try {
            // 먼저 요청에 포함된 ID 목록 추출 (나중에 DELETE에서 제외할 대상)
            List<Long> keepIds = new ArrayList<>(req.getWidgets().stream()
                    .map(DashboardDto.UserWidgetReq::getUserDashboardWidgetId)
                    .filter(Objects::nonNull)
                    .toList());
            if (keepIds.isEmpty()) keepIds.add(0L);

            // 요청에 포함되지 않은 위젯 먼저 DELETE (keepIds가 비어있으면 모든 위젯 삭제)
            log.info("DELETE 호출 - userId: {}, keepIds: {}", userId, keepIds);
            int deletedRows = dashboardMapper.deleteUserWidget(Map.of("userId", userId, "keepIds", keepIds));
            log.info("DELETE 결과 - 삭제된 행: {}", deletedRows);

            // 그 다음 UPDATE/INSERT 처리
            for (DashboardDto.UserWidgetReq widget : req.getWidgets()) {
                // 기존위젯은 UPDATE 시도
                if (widget.getUserDashboardWidgetId() != null) {
                    log.info("UPDATE 시도 - userDashboardWidgetId: {}, widgetId: {}", widget.getUserDashboardWidgetId(), widget.getWidgetId());
                    int updatedRows = dashboardMapper.updateUserWidget(userId, widget);
                    log.info("UPDATE 결과 - 영향받은 행: {}", updatedRows);
                    // UPDATE 실패 시 (해당 ID가 없는 경우) INSERT
                    if (updatedRows == 0) {
                        log.info("UPDATE 실패, INSERT 시도 - widgetId: {}", widget.getWidgetId());
                        int insertedRows = dashboardMapper.insertUserWidget(userId, widget);
                        log.info("INSERT 결과 - 영향받은 행: {}", insertedRows);
                    }
                } else {
                    // 새로운 위젯은 INSERT
                    log.info("새 위젯 INSERT 시도 - widgetId: {}, title: {}", widget.getWidgetId(), widget.getTitle());
                    int insertedRows = dashboardMapper.insertUserWidget(userId, widget);
                    log.info("INSERT 결과 - 영향받은 행: {}", insertedRows);
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("대시보드 사용자 위젯 저장 실패 - {}", e.getMessage());
            return false;
        }
    }
    @Transactional
    public List<DashboardDto.UserWidgetRes> resetUserWidget(Long userId) {
        try {
            List<DashboardDto.DefaultWidgetRes> defaultWidget = dashboardMapper.getDefaultWidget();

            if (defaultWidget.size() > 0) {
                // 다 지움
                List<Long> keepIds = new ArrayList<>();
                keepIds.add(0L);
                dashboardMapper.deleteUserWidget(Map.of("userId", userId, "keepIds", keepIds));

                // 기본 값으로 업데이트
                for (DashboardDto.DefaultWidgetRes dto : defaultWidget) {
                    int insertedRows = dashboardMapper.insertUserWidgetByDefault(userId, dto);
                    log.info("INSERT 결과 - 영향받은 행: {}", insertedRows);
                }
                return getUserWidget(userId);
            }else {
                log.info("대시보드 기본 값 X 업데이트 불가");
                return null;
            }
        }catch (Exception e) {
            log.warn("대시보드 사용자 위젯 저장 실패 - {}", e.getMessage());
            return null;
        }
    }


    public static List<String> normalizeElements(List<String> elements) {
        if (elements == null) return List.of();

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String e : elements) {
            if (e == null) continue;
            String key = e.trim();
            if (IcmpMetricMap.METRIC_COL.containsKey(key)) {
                set.add(key); // 중복 제거 + 입력순서 유지
            }
        }
        return new ArrayList<>(set);
    }
    public static List<Map<String, String>> toMetricParams(List<String> elements) {
        List<String> keys = normalizeElements(elements);
        List<Map<String, String>> metrics = new ArrayList<>(keys.size());
        for (String k : keys) {
            String col = IcmpMetricMap.METRIC_COL.get(k);
            if (col == null) continue; // 방어 (normalizeElements가 걸러주지만)
            Map<String, String> item = new HashMap<>();
            item.put("key", k);
            item.put("col", col);
            metrics.add(item);
        }
        return metrics;
    }

    public static List<String> normalizeTrafficElements(List<String> elements) {
        if (elements == null) return List.of();

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String e : elements) {
            if (e == null) continue;
            String key = e.trim();
            if (TrafficMetricMap.METRIC_COL.containsKey(key)) {
                set.add(key);
            }
        }
        return new ArrayList<>(set);
    }

    public static List<Map<String, String>> toTrafficMetricParams(List<String> elements) {
        List<String> keys = normalizeTrafficElements(elements);
        List<Map<String, String>> metrics = new ArrayList<>(keys.size());
        for (String k : keys) {
            String col = TrafficMetricMap.METRIC_COL.get(k);
            if (col == null) continue;
            Map<String, String> item = new HashMap<>();
            item.put("key", k);
            item.put("col", col);
            metrics.add(item);
        }
        return metrics;
    }

    public static class IcmpMetricMap {
        // key = elements 값, value = DB 컬럼(또는 식)
        public static final Map<String, String> METRIC_COL;
        static {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("ICMP_MAX",  "RESPONSE_MAX_TIME");
            m.put("ICMP_MIN",  "RESPONSE_MIN_TIME");
            m.put("ICMP_AVG",  "RESPONSE_TIME");
            m.put("ICMP_LOSS", "PACKET_LOSS");
            METRIC_COL = Collections.unmodifiableMap(m);
        }
    }

    public static class TrafficMetricMap {
        // key = elements 값, value = DB 컬럼(또는 식)
        public static final Map<String, String> METRIC_COL;
        static {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("TRAFFIC_IN_BPS",  "COALESCE(IN_HIGH_BPS, IN_BPS, 0)");
            m.put("TRAFFIC_OUT_BPS", "COALESCE(OUT_HIGH_BPS, OUT_BPS, 0)");
            m.put("TRAFFIC_IN_ERR",  "IN_ERROR");
            m.put("TRAFFIC_OUT_ERR", "OUT_ERROR");
            m.put("TRAFFIC_IN_BYTE", "COALESCE(IN_HIGH_USED, IN_USED, 0)");
            m.put("TRAFFIC_OUT_BYTE", "COALESCE(OUT_HIGH_USED, OUT_USED, 0)");
            m.put("TRAFFIC_IN_DISCARD", "IN_DISCARD");
            m.put("TRAFFIC_OUT_DISCARD", "OUT_DISCARD");
            METRIC_COL = Collections.unmodifiableMap(m);
        }
    }

    public static class CpuMemMetricMap {
        // key = elements 값, value = DB 컬럼
        public static final Map<String, String> METRIC_COL;
        static {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("CPU", "CPU_USAGE");
            m.put("MEMORY", "MEM_USAGE");
            METRIC_COL = Collections.unmodifiableMap(m);
        }
    }

    public static List<String> normalizeCpuMemElements(List<String> elements) {
        if (elements == null) return List.of();

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String e : elements) {
            if (e == null) continue;
            String key = e.trim();
            if (CpuMemMetricMap.METRIC_COL.containsKey(key)) {
                set.add(key);
            }
        }
        return new ArrayList<>(set);
    }

    public static List<Map<String, String>> toCpuMemMetricParams(List<String> elements) {
        List<String> keys = normalizeCpuMemElements(elements);
        List<Map<String, String>> metrics = new ArrayList<>(keys.size());
        for (String k : keys) {
            String col = CpuMemMetricMap.METRIC_COL.get(k);
            if (col == null) continue;
            Map<String, String> item = new HashMap<>();
            item.put("key", k);
            item.put("col", col);
            metrics.add(item);
        }
        return metrics;
    }
}
