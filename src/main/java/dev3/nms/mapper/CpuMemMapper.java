package dev3.nms.mapper;

import dev3.nms.vo.mgmt.CpuMemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CpuMemMapper {

    /**
     * 특정 장비의 최신 CPU/MEM 데이터 조회 (집계값만, CORE_INDEX IS NULL)
     * @param deviceId 장비 ID
     * @return 최신 CPU/MEM 데이터
     */
    CpuMemVO findLatestByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 특정 장비의 최근 CPU/MEM 데이터 조회 (집계값만)
     * @param deviceId 장비 ID
     * @param minutes 최근 N분
     * @return CPU/MEM 데이터 목록
     */
    List<CpuMemVO> findRecentByDeviceId(@Param("deviceId") Integer deviceId,
                                         @Param("minutes") Integer minutes,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    /**
     * 특정 장비의 코어별 CPU 데이터 조회 (가장 최신)
     * @param deviceId 장비 ID
     * @return 코어별 CPU 데이터 목록
     */
    List<CpuMemVO> findLatestCoresByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 특정 장비의 CPU/MEM 데이터 조회 (시간 범위)
     * @param deviceId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return CPU/MEM 데이터 목록
     */
    List<CpuMemVO> findByDeviceIdAndTimeRange(@Param("deviceId") Integer deviceId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    int deleteByDeviceId(@Param("deviceId") Integer deviceId);

    List<Map<String, Object>> getDailyCpuMemTop(@Param("date") String date, @Param("limit") int limit);

    List<Map<String, Object>> getDailyPerfHourly(@Param("date") String date);

    /**
     * 다중 장비의 CPU/MEM raw 데이터 조회 (batch)
     */
    List<CpuMemVO> findRecentBatchRaw(@Param("deviceIds") List<Integer> deviceIds,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate);

    /**
     * 다중 장비의 CPU/MEM 집계 데이터 조회 (batch, intervalSec 단위로 그룹화)
     */
    List<CpuMemVO> findRecentBatchAggregated(@Param("deviceIds") List<Integer> deviceIds,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate,
                                              @Param("intervalSec") Integer intervalSec);
}
