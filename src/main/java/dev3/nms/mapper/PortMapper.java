package dev3.nms.mapper;

import dev3.nms.vo.mgmt.PortVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PortMapper {

    /**
     * 특정 장비의 Ethernet 포트 조회 (IF_TYPE = 6, 물리 포트만)
     */
    List<PortVO> findByDeviceId(Integer deviceId);

    /**
     * 특정 장비의 모든 포트 조회 (타입 제한 없음)
     */
    List<PortVO> findAllByDeviceId(Integer deviceId);

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
     * 포트 일괄 UPSERT (INSERT ... ON DUPLICATE KEY UPDATE)
     * 신규: 기본 감시 설정(ON)으로 INSERT
     * 기존: 감시 설정(IF_OPER_FLAG, IF_PERF_FLAG) 유지하고 나머지 업데이트
     */
    int upsertPorts(List<PortVO> ports);
}
