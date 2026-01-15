package dev3.nms.service;

import dev3.nms.mapper.TrafficMapper;
import dev3.nms.vo.mgmt.TrafficVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficService {

    private final TrafficMapper trafficMapper;

    // 가상 인터페이스 패턴 (프론트엔드와 동일)
    private static final java.util.regex.Pattern VIRTUAL_INTERFACE_PATTERN =
        java.util.regex.Pattern.compile("^(veth|docker|br-|virbr|vnet|tap|tun|dummy|lo$)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * 가상 인터페이스 여부 확인
     */
    private boolean isVirtualInterface(TrafficVO vo) {
        String ifName = vo.getIF_NAME();
        String ifDescr = vo.getIF_DESCR();
        String name = (ifName != null && !ifName.isEmpty()) ? ifName : ifDescr;
        if (name == null || name.isEmpty()) return false;
        return VIRTUAL_INTERFACE_PATTERN.matcher(name).find();
    }

    /**
     * 특정 장비의 최근 트래픽 데이터 조회 (가상 인터페이스 제외)
     * @param deviceId 장비 ID
     * @param minutes 최근 N분 (기본 60분)
     * @return 트래픽 데이터 목록
     */
    public List<TrafficVO> getRecentTraffic(Integer deviceId, Integer minutes) {
        if (minutes == null || minutes <= 0) {
            minutes = 60;
        }
        List<TrafficVO> rawData = trafficMapper.findRecentByDeviceId(deviceId, minutes);
        return rawData.stream()
            .filter(vo -> !isVirtualInterface(vo))
            .collect(Collectors.toList());
    }

    /**
     * 특정 장비/포트의 최근 트래픽 데이터 조회
     * @param deviceId 장비 ID
     * @param ifIndex 포트 인덱스
     * @param minutes 최근 N분 (기본 60분)
     * @return 트래픽 데이터 목록
     */
    public List<TrafficVO> getPortTraffic(Integer deviceId, Integer ifIndex, Integer minutes) {
        if (minutes == null || minutes <= 0) {
            minutes = 60;
        }
        return trafficMapper.findRecentByDeviceIdAndIfIndex(deviceId, ifIndex, minutes);
    }

    /**
     * 특정 장비의 트래픽 데이터 조회 (시간 범위)
     * @param deviceId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 트래픽 데이터 목록
     */
    public List<TrafficVO> getTrafficByTimeRange(Integer deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return trafficMapper.findByDeviceIdAndTimeRange(deviceId, startTime, endTime);
    }

    /**
     * 특정 장비의 최신 트래픽 데이터 조회 (포트별 1건)
     * @param deviceId 장비 ID
     * @return 포트별 최신 트래픽 데이터
     */
    public List<TrafficVO> getLatestTraffic(Integer deviceId) {
        return trafficMapper.findLatestByDeviceId(deviceId);
    }

    /**
     * 차트용 트래픽 데이터 조회
     * 포트별로 그룹화하여 시계열 데이터로 변환 (가상 인터페이스 제외)
     * @param deviceId 장비 ID
     * @param minutes 최근 N분
     * @return 차트용 데이터 (포트별 시계열)
     */
    public Map<String, Object> getTrafficChartData(Integer deviceId, Integer minutes) {
        if (minutes == null || minutes <= 0) {
            minutes = 60;
        }

        List<TrafficVO> rawData = trafficMapper.findRecentByDeviceId(deviceId, minutes);

        // 가상 인터페이스 필터링
        rawData = rawData.stream()
            .filter(vo -> !isVirtualInterface(vo))
            .collect(Collectors.toList());

        if (rawData == null || rawData.isEmpty()) {
            return Map.of(
                "timeLabels", Collections.emptyList(),
                "series", Collections.emptyList()
            );
        }

        // 시간 라벨 수집 (중복 제거, 정렬)
        Set<LocalDateTime> timeSet = new TreeSet<>();
        for (TrafficVO vo : rawData) {
            if (vo.getCOLLECTED_AT() != null) {
                timeSet.add(vo.getCOLLECTED_AT());
            }
        }
        List<String> timeLabels = timeSet.stream()
            .map(t -> String.format("%02d:%02d", t.getHour(), t.getMinute()))
            .collect(Collectors.toList());

        // 포트별로 그룹화
        Map<Integer, List<TrafficVO>> byPort = rawData.stream()
            .collect(Collectors.groupingBy(TrafficVO::getIF_INDEX));

        // 시리즈 데이터 생성
        List<Map<String, Object>> series = new ArrayList<>();
        for (Map.Entry<Integer, List<TrafficVO>> entry : byPort.entrySet()) {
            Integer ifIndex = entry.getKey();
            List<TrafficVO> portData = entry.getValue();

            // 포트 이름 가져오기
            String portName = portData.isEmpty() ? "Port " + ifIndex : portData.get(0).getPortName();

            // 시간별 데이터 매핑
            Map<LocalDateTime, TrafficVO> timeMap = portData.stream()
                .collect(Collectors.toMap(
                    TrafficVO::getCOLLECTED_AT,
                    v -> v,
                    (v1, v2) -> v2
                ));

            // BPS 데이터 추출 (IN + OUT 합산)
            List<Double> bpsData = new ArrayList<>();
            for (LocalDateTime time : timeSet) {
                TrafficVO vo = timeMap.get(time);
                if (vo != null) {
                    Double inBps = vo.getInBpsValue();
                    Double outBps = vo.getOutBpsValue();
                    double total = 0;
                    if (inBps != null) total += inBps;
                    if (outBps != null) total += outBps;
                    bpsData.add(total);
                } else {
                    bpsData.add(0.0);
                }
            }

            Map<String, Object> portSeries = new LinkedHashMap<>();
            portSeries.put("ifIndex", ifIndex);
            portSeries.put("name", portName);
            portSeries.put("data", bpsData);
            series.add(portSeries);
        }

        // 트래픽이 많은 순으로 정렬 (제한 없이 모든 포트 반환 - 프론트엔드에서 IF_CHART_FLAG 기반 필터링)
        series.sort((a, b) -> {
            @SuppressWarnings("unchecked")
            List<Double> dataA = (List<Double>) a.get("data");
            @SuppressWarnings("unchecked")
            List<Double> dataB = (List<Double>) b.get("data");
            double sumA = dataA.stream().mapToDouble(Double::doubleValue).sum();
            double sumB = dataB.stream().mapToDouble(Double::doubleValue).sum();
            return Double.compare(sumB, sumA);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timeLabels", timeLabels);
        result.put("series", series);
        return result;
    }
}
