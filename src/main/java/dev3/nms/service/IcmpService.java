package dev3.nms.service;

import dev3.nms.mapper.IcmpMapper;
import dev3.nms.vo.mgmt.IcmpVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IcmpService {

    private final IcmpMapper icmpMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ICMP 시계열 조회. granularity:
     *   "raw" → raw 데이터, 그 외("5min","30min","1hour","auto") → 집계
     *   "auto" 또는 null이면 기간 길이로 자동 결정
     */
    public List<IcmpVO> getHistory(Integer deviceId, Integer minutes, String startDate, String endDate, String granularity) {
        TimeRange range = resolveRange(minutes, startDate, endDate);
        Integer intervalSec = resolveIntervalSec(granularity, range.start, range.end);
        if (intervalSec == null || intervalSec <= 0) {
            return icmpMapper.findHistoryRaw(deviceId, range.startStr, range.endStr);
        }
        return icmpMapper.findHistoryAggregated(deviceId, range.startStr, range.endStr, intervalSec);
    }

    /**
     * 다중 장비 ICMP 시계열 조회 (batch).
     * 응답: {deviceId(String) → List<IcmpVO>}
     */
    public Map<String, List<IcmpVO>> getHistoryBatch(List<Integer> deviceIds, Integer minutes,
                                                      String startDate, String endDate, String granularity) {
        Map<String, List<IcmpVO>> result = new LinkedHashMap<>();
        if (deviceIds == null || deviceIds.isEmpty()) return result;
        // 빈 배열로 초기화 (장애 등으로 데이터 없는 장비도 응답에 포함)
        for (Integer id : deviceIds) {
            result.put(String.valueOf(id), new ArrayList<>());
        }

        TimeRange range = resolveRange(minutes, startDate, endDate);
        Integer intervalSec = resolveIntervalSec(granularity, range.start, range.end);

        List<IcmpVO> rows;
        if (intervalSec == null || intervalSec <= 0) {
            rows = icmpMapper.findHistoryRawBatch(deviceIds, range.startStr, range.endStr);
        } else {
            rows = icmpMapper.findHistoryAggregatedBatch(deviceIds, range.startStr, range.endStr, intervalSec);
        }

        for (IcmpVO vo : rows) {
            String key = String.valueOf(vo.getDEVICE_ID());
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private static TimeRange resolveRange(Integer minutes, String startDate, String endDate) {
        LocalDateTime start, end;
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            start = parseDate(startDate);
            end = parseDate(endDate);
        } else {
            int m = minutes != null ? minutes : 60;
            end = LocalDateTime.now();
            start = end.minusMinutes(m);
        }
        return new TimeRange(start, end, start.format(FMT), end.format(FMT));
    }

    private static LocalDateTime parseDate(String s) {
        // ISO 'yyyy-MM-ddTHH:mm' 또는 'yyyy-MM-dd HH:mm:ss' 모두 허용
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (Exception e) {
            try { return LocalDateTime.parse(s, FMT); } catch (Exception ex) {
                return LocalDateTime.parse(s + "T00:00:00");
            }
        }
    }

    /**
     * granularity → intervalSec 변환
     * raw:0(null로 raw 반환), 5min:300, 30min:1800, 1hour:3600
     * auto: 기간으로 결정
     */
    public static Integer resolveIntervalSec(String granularity, LocalDateTime start, LocalDateTime end) {
        if (granularity == null) granularity = "auto";
        switch (granularity.toLowerCase()) {
            case "raw":   return null;
            case "5min":  return 300;
            case "30min": return 1800;
            case "1hour": return 3600;
            case "auto":
            default:
                long hours = Math.max(1, Duration.between(start, end).toHours());
                if (hours <= 6)   return null;       // raw
                if (hours <= 48)  return 300;        // 5min
                if (hours <= 14*24) return 1800;     // 30min
                return 3600;                         // 1hour
        }
    }

    /**
     * granularity와 minutes/startDate/endDate로부터 intervalSec 직접 계산 (다른 서비스용 헬퍼)
     */
    public static Integer resolveIntervalSec(Integer minutes, String startDate, String endDate, String granularity) {
        TimeRange range = resolveRange(minutes, startDate, endDate);
        return resolveIntervalSec(granularity, range.start, range.end);
    }

    /**
     * 시간 범위 + 포맷된 문자열을 같이 노출하기 위한 헬퍼
     */
    public static class TimeRange {
        public final LocalDateTime start;
        public final LocalDateTime end;
        public final String startStr;
        public final String endStr;
        public TimeRange(LocalDateTime start, LocalDateTime end, String startStr, String endStr) {
            this.start = start; this.end = end; this.startStr = startStr; this.endStr = endStr;
        }
    }

    /**
     * 외부에서 같은 형태로 시간 범위를 얻기 위한 public helper
     */
    public static TimeRange resolveTimeRange(Integer minutes, String startDate, String endDate) {
        return resolveRange(minutes, startDate, endDate);
    }
}
