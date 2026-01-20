package dev3.nms.mapper;

import dev3.nms.vo.alert.AlertVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertMapper {

    // === 현재 장애 테이블 ===

    /**
     * 현재 장애 추가/갱신
     */
    int upsertCurrentAlert(AlertVO alert);

    /**
     * 현재 장애 삭제 (해소 시)
     */
    int deleteCurrentAlert(@Param("deviceId") Integer deviceId,
                           @Param("alertType") String alertType,
                           @Param("ifIndex") Integer ifIndex);

    /**
     * 현재 장애 목록 조회
     */
    List<AlertVO> selectCurrentAlerts(@Param("category") String category,
                                      @Param("severity") String severity);

    /**
     * 특정 장비의 현재 장애 조회
     */
    List<AlertVO> selectCurrentAlertsByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 현재 장애 수 조회
     */
    int countCurrentAlerts(@Param("category") String category,
                           @Param("severity") String severity);

    // === 알림 히스토리 테이블 ===

    /**
     * 알림 히스토리 저장
     */
    int insertAlertHistory(AlertVO alert);

    /**
     * 알림 히스토리 조회
     */
    List<AlertVO> selectAlertHistory(@Param("category") String category,
                                     @Param("deviceId") Integer deviceId,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    /**
     * 알림 히스토리 총 건수
     */
    int countAlertHistory(@Param("category") String category,
                          @Param("deviceId") Integer deviceId,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate);
}
