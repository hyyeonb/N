package dev3.nms.mapper;

import dev3.nms.vo.mgmt.TrafficVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrafficMapper {

    /**
     * 특정 장비의 최근 트래픽 데이터 조회 (모든 포트)
     * @param deviceId 장비 ID
     * @param minutes 최근 N분
     * @return 트래픽 데이터 목록
     */
    List<TrafficVO> findRecentByDeviceId(@Param("deviceId") Integer deviceId,
                                          @Param("minutes") Integer minutes,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    /**
     * 특정 장비/포트의 최근 트래픽 데이터 조회
     * @param deviceId 장비 ID
     * @param ifIndex 포트 인덱스
     * @param minutes 최근 N분
     * @return 트래픽 데이터 목록
     */
    List<TrafficVO> findRecentByDeviceIdAndIfIndex(@Param("deviceId") Integer deviceId,
                                                    @Param("ifIndex") Integer ifIndex,
                                                    @Param("minutes") Integer minutes);

    /**
     * 특정 장비의 트래픽 데이터 조회 (시간 범위)
     * @param deviceId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 트래픽 데이터 목록
     */
    List<TrafficVO> findByDeviceIdAndTimeRange(@Param("deviceId") Integer deviceId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 특정 장비의 최신 트래픽 데이터 1건 조회 (포트별)
     * @param deviceId 장비 ID
     * @return 포트별 최신 트래픽 데이터
     */
    List<TrafficVO> findLatestByDeviceId(@Param("deviceId") Integer deviceId);

    int deleteByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 다중 장비의 트래픽 raw 데이터 조회 (batch)
     */
    List<TrafficVO> findRecentBatchRaw(@Param("deviceIds") List<Integer> deviceIds,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    /**
     * 다중 장비의 트래픽 집계 데이터 조회 (batch, intervalSec 단위로 그룹화)
     * 포트별로 GROUP BY (DEVICE_ID, IF_INDEX, time-bucket)
     */
    List<TrafficVO> findRecentBatchAggregated(@Param("deviceIds") List<Integer> deviceIds,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate,
                                               @Param("intervalSec") Integer intervalSec);

    /**
     * 다중 (장비, 포트) 쌍의 트래픽 raw 데이터 조회 (batch)
     * @param ports List of {deviceId, ifIndex}
     * @param minutes 최근 N분
     */
    List<TrafficVO> findRecentByPortsBatch(@Param("ports") List<Map<String, Integer>> ports,
                                            @Param("minutes") Integer minutes);
}
