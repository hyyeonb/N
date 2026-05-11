package dev3.nms.mapper;

import dev3.nms.vo.mgmt.IcmpVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IcmpMapper {

    /**
     * 시간 범위 내 ICMP raw 데이터 조회
     */
    List<IcmpVO> findHistoryRaw(@Param("deviceId") Integer deviceId,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate);

    /**
     * 시간 범위 내 ICMP 집계 데이터 조회 (intervalSec 단위로 그룹화)
     */
    List<IcmpVO> findHistoryAggregated(@Param("deviceId") Integer deviceId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("intervalSec") Integer intervalSec);

    /**
     * 다중 장비의 ICMP raw 데이터 조회 (batch)
     */
    List<IcmpVO> findHistoryRawBatch(@Param("deviceIds") List<Integer> deviceIds,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate);

    /**
     * 다중 장비의 ICMP 집계 데이터 조회 (batch, intervalSec 단위로 그룹화)
     */
    List<IcmpVO> findHistoryAggregatedBatch(@Param("deviceIds") List<Integer> deviceIds,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("intervalSec") Integer intervalSec);
}
