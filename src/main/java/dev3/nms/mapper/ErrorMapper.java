package dev3.nms.mapper;

import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ErrorMapper {

    /**
     * 실시간 장애 목록 조회
     */
    List<ErrorVO> selectErrors(@Param("errorLevel") String errorLevel,
                                @Param("deviceId") Long deviceId,
                                @Param("devCodeId") Long devCodeId,
                                @Param("deviceName") String deviceName,
                                @Param("deviceIp") String deviceIp,
                                @Param("errorMessage") String errorMessage,
                                @Param("groupName") String groupName,
                                @Param("accessibleDeviceIds") List<Long> accessibleDeviceIds);

    /**
     * 실시간 장애 수 조회
     */
    int countErrors(@Param("errorLevel") String errorLevel,
                    @Param("deviceId") Long deviceId,
                    @Param("devCodeId") Long devCodeId,
                    @Param("deviceName") String deviceName,
                    @Param("deviceIp") String deviceIp,
                    @Param("errorMessage") String errorMessage,
                    @Param("groupName") String groupName,
                    @Param("accessibleDeviceIds") List<Long> accessibleDeviceIds);

    /**
     * 장애 상세 조회
     */
    ErrorVO selectErrorById(@Param("errorId") Long errorId);

    /**
     * 장애 인지 처리
     */
    int updateErrorFlag(@Param("errorId") Long errorId,
                        @Param("errorFlag") Integer errorFlag,
                        @Param("userMessage") String userMessage);

    /**
     * 장애 이력 목록 조회 (페이징)
     */
    List<ErrorHistoryVO> selectErrorHistory(@Param("errorLevel") String errorLevel,
                                             @Param("deviceId") Long deviceId,
                                             @Param("devCodeId") Long devCodeId,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("deviceName") String deviceName,
                                             @Param("deviceIp") String deviceIp,
                                             @Param("errorMessage") String errorMessage,
                                             @Param("groupName") String groupName,
                                             @Param("sortKey") String sortKey,
                                             @Param("sortDirection") String sortDirection,
                                             @Param("offset") int offset,
                                             @Param("size") int size,
                                             @Param("accessibleDeviceIds") List<Long> accessibleDeviceIds,
                                             @Param("errorLevels") List<String> errorLevels);

    /**
     * 장애 이력 수 조회
     */
    int countErrorHistory(@Param("errorLevel") String errorLevel,
                          @Param("deviceId") Long deviceId,
                          @Param("devCodeId") Long devCodeId,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate,
                          @Param("deviceName") String deviceName,
                          @Param("deviceIp") String deviceIp,
                          @Param("errorMessage") String errorMessage,
                          @Param("groupName") String groupName,
                          @Param("accessibleDeviceIds") List<Long> accessibleDeviceIds,
                          @Param("errorLevels") List<String> errorLevels);

    /**
     * 장애 이력 상세 조회
     */
    ErrorHistoryVO selectErrorHistoryById(@Param("errorHistoryId") Long errorHistoryId);

    /**
     * 포트 비관리 전환 시 장애 이력 이관 (CLEAR_AT 설정)
     */
    int insertPortErrorHistory(@Param("deviceId") Integer deviceId, @Param("ifIndex") Integer ifIndex);

    /**
     * 포트 비관리 전환 시 장애 삭제
     */
    int deletePortError(@Param("deviceId") Integer deviceId, @Param("ifIndex") Integer ifIndex);

    // ========== 장비 삭제 시 장애 정리 ==========

    /**
     * 장비의 모든 활성 장애 → 이력 이관
     */
    int moveDeviceErrorsToHistory(@Param("deviceId") Integer deviceId);

    /**
     * 장비의 모든 활성 장애 삭제
     */
    int deleteDeviceErrors(@Param("deviceId") Integer deviceId);

    /**
     * 삭제된 포트(DELETE_AT NOT NULL)의 잔존 Oper 장애 → 이력 이관
     */
    int moveDeletedPortErrorsToHistory(@Param("deviceId") Integer deviceId);

    /**
     * 삭제된 포트의 잔존 Oper 장애 삭제
     */
    int deleteDeletedPortErrors(@Param("deviceId") Integer deviceId);

    int deleteIcmpByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 장비의 모든 장애 이력 삭제
     */
    int deleteErrorHistoryByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 장애 유형별 고정 등급 조회 (b_error_code_t)
     * 유형당 1개 등급만 있으면 고정, 여러 등급이면 임계치 기반
     */
    List<Map<String, Object>> findErrorCodeTypes();

    // ========== 장애 통계 ==========

    List<Map<String, Object>> selectErrorCountByLevel(@Param("groupIds") List<Long> groupIds,
                                                       @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorCountByType(@Param("groupIds") List<Long> groupIds,
                                                      @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorTrendDaily(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("groupIds") List<Long> groupIds,
                                                     @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorTrendByType(@Param("startDate") String startDate,
                                                      @Param("endDate") String endDate,
                                                      @Param("groupIds") List<Long> groupIds,
                                                      @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorTrendHourly(@Param("startDate") String startDate,
                                                      @Param("endDate") String endDate,
                                                      @Param("groupIds") List<Long> groupIds,
                                                      @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorTrendByTypeHourly(@Param("startDate") String startDate,
                                                            @Param("endDate") String endDate,
                                                            @Param("groupIds") List<Long> groupIds,
                                                            @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectMttrByType(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("groupIds") List<Long> groupIds,
                                                @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectTopErrorDevices(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("limit") int limit,
                                                     @Param("groupIds") List<Long> groupIds,
                                                     @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorPatternByHour(@Param("startDate") String startDate,
                                                        @Param("endDate") String endDate,
                                                        @Param("groupIds") List<Long> groupIds,
                                                        @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorPatternByDow(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate,
                                                       @Param("groupIds") List<Long> groupIds,
                                                       @Param("deviceIds") List<Long> deviceIds);

    List<Map<String, Object>> selectErrorAging(@Param("groupIds") List<Long> groupIds,
                                                @Param("deviceIds") List<Long> deviceIds);

    /**
     * 장애 이력의 위치 (해당 행 앞에 몇 건이 있는지)
     */
    int countHistoryBefore(@Param("errorHistoryId") Long errorHistoryId,
                           @Param("deviceId") Long deviceId);
}
