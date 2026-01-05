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
            return dashboardMapper.getDefaultWidget();
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
                    if ("CPU".equals(userWidgetConfig.getElements().get(0))) {
                        if ("pie".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetCpuPieChartData();
                            userWidget.setChartData(pieChartData);
                        }else if ("line".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetCpuLineChartData();
                            userWidget.setChartData(lineChartData);
                        }else if ("bar".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetBarChartData>  barChartData = dashboardMapper.getWidgetCpuBarChartData();
                            userWidget.setChartData(barChartData);
                        }
                    }else {
                        if ("pie".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetPieChartData> pieChartData = dashboardMapper.getWidgetMemPieChartData();
                            userWidget.setChartData(pieChartData);
                        }else if ("line".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetLineChartData> lineChartData = dashboardMapper.getWidgetMemLineChartData();
                            userWidget.setChartData(lineChartData);
                        }else if ("bar".equals(userWidgetConfig.getChartType())) {
                            List<DashboardDto.WidgetBarChartData> barChartData = dashboardMapper.getWidgetMemBarChartData();
                            userWidget.setChartData(barChartData);
                        }
                    }
                }else if ("FILE".equals(userWidgetConfig.getGroup())) {

                }else if ("PROCESS".equals(userWidgetConfig.getGroup())) {

                }else if ("TRAFFIC".equals(userWidgetConfig.getGroup())) {

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


            }
            
            return userWidgets;
        }catch (Exception e) {
            log.warn("대시보드 사용자 위젯 조회 실패 - {}", e.getMessage());
            return Collections.singletonList(DashboardDto.UserWidgetRes.builder().build());
        }
    }

    private void getWidgetCpuPieChartData() {
    }

    @Transactional
    public Boolean putUserWidget(Long userId, DashboardDto.UpdateUserWidgetsReq req) {
        try {
            // 먼저 요청에 포함된 ID 목록 추출 (나중에 DELETE에서 제외할 대상)
            List<Long> keepIds = req.getWidgets().stream()
                    .map(DashboardDto.UserWidgetReq::getUserDashboardWidgetId)
                    .filter(Objects::nonNull)
                    .toList();

            // 요청에 포함되지 않은 위젯 먼저 DELETE
            if (keepIds.size() != 0) {
                dashboardMapper.deleteUserWidget(Map.of("userId", userId, "keepIds", keepIds));
            }

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
}
