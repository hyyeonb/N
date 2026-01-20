package dev3.nms.mapper;

import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErrorMapper {

    /**
     * 실시간 장애 목록 조회
     */
    List<ErrorVO> selectErrors(@Param("errorLevel") String errorLevel,
                                @Param("deviceId") Long deviceId,
                                @Param("deviceName") String deviceName,
                                @Param("deviceIp") String deviceIp,
                                @Param("errorMessage") String errorMessage,
                                @Param("groupName") String groupName);

    /**
     * 실시간 장애 수 조회
     */
    int countErrors(@Param("errorLevel") String errorLevel,
                    @Param("deviceId") Long deviceId,
                    @Param("deviceName") String deviceName,
                    @Param("deviceIp") String deviceIp,
                    @Param("errorMessage") String errorMessage,
                    @Param("groupName") String groupName);

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
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("deviceName") String deviceName,
                                             @Param("deviceIp") String deviceIp,
                                             @Param("errorMessage") String errorMessage,
                                             @Param("groupName") String groupName,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    /**
     * 장애 이력 수 조회
     */
    int countErrorHistory(@Param("errorLevel") String errorLevel,
                          @Param("deviceId") Long deviceId,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate,
                          @Param("deviceName") String deviceName,
                          @Param("deviceIp") String deviceIp,
                          @Param("errorMessage") String errorMessage,
                          @Param("groupName") String groupName);

    /**
     * 장애 이력 상세 조회
     */
    ErrorHistoryVO selectErrorHistoryById(@Param("errorHistoryId") Long errorHistoryId);
}
