package dev3.nms.mapper;

import dev3.nms.vo.mgmt.PortVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PortMapper {

    /**
     * 특정 장비의 모든 포트 조회
     */
    List<PortVO> findByDeviceId(Integer deviceId);

    /**
     * 특정 포트 조회
     */
    PortVO findByDeviceIdAndIfIndex(@Param("deviceId") Integer deviceId, @Param("ifIndex") Integer ifIndex);

    /**
     * 모든 포트 조회 (장비 정보 포함)
     */
    List<PortVO> findAll();

    /**
     * 감시 대상 포트만 조회
     */
    List<PortVO> findMonitoredPorts();

    /**
     * 포트 등록
     */
    int insertPort(PortVO port);

    /**
     * 포트 일괄 등록
     */
    int insertPorts(List<PortVO> ports);

    /**
     * 포트 정보 수정
     */
    int updatePort(PortVO port);

    /**
     * 포트 감시 설정 수정
     */
    int updateMonitorSettings(@Param("deviceId") Integer deviceId,
                              @Param("ifIndex") Integer ifIndex,
                              @Param("ifOperFlag") Boolean ifOperFlag,
                              @Param("ifPerfFlag") Boolean ifPerfFlag);

    /**
     * 포트 삭제 (소프트 삭제)
     */
    int deletePort(@Param("deviceId") Integer deviceId, @Param("ifIndex") Integer ifIndex);

    /**
     * 장비의 모든 포트 삭제 (소프트 삭제)
     */
    int deletePortsByDeviceId(Integer deviceId);

    /**
     * 특정 장비의 포트 개수 조회
     */
    int countByDeviceId(Integer deviceId);

    /**
     * 차트 플래그 업데이트
     */
    int updateChartFlag(@Param("deviceId") Integer deviceId,
                        @Param("ifIndex") Integer ifIndex,
                        @Param("chartFlag") Boolean chartFlag);

    /**
     * 차트 표시 포트 조회
     */
    List<PortVO> findChartEnabledPorts(Integer deviceId);

    /**
     * 차트 표시 포트 수 조회
     */
    int countChartEnabledPorts(Integer deviceId);

    /**
     * 장비의 모든 포트 차트 플래그 초기화
     */
    int resetChartFlags(Integer deviceId);

    /**
     * TOP N 트래픽 포트에 차트 플래그 설정
     */
    int setTopTrafficPortsChartFlag(@Param("deviceId") Integer deviceId, @Param("limit") Integer limit);
}
