package dev3.nms.service;

import dev3.nms.mapper.ErrorMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.fault.ErrorHistoryVO;
import dev3.nms.vo.fault.ErrorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorService {

    private final ErrorMapper errorMapper;

    /**
     * 실시간 장애 목록 조회
     */
    public List<ErrorVO> getErrors(String errorLevel, Long deviceId, Long devCodeId,
                                    String deviceName, String deviceIp,
                                    String errorMessage, String groupName) {
        return errorMapper.selectErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
    }

    /**
     * 실시간 장애 수 조회
     */
    public int countErrors(String errorLevel, Long deviceId, Long devCodeId,
                           String deviceName, String deviceIp,
                           String errorMessage, String groupName) {
        return errorMapper.countErrors(errorLevel, deviceId, devCodeId, deviceName, deviceIp, errorMessage, groupName);
    }

    /**
     * 장애 상세 조회
     */
    public ErrorVO getErrorById(Long errorId) {
        return errorMapper.selectErrorById(errorId);
    }

    /**
     * 장애 인지 처리
     */
    @Transactional
    public boolean acknowledgeError(Long errorId, String userMessage) {
        int result = errorMapper.updateErrorFlag(errorId, 1, userMessage);
        return result > 0;
    }

    /**
     * 장애 이력 목록 조회 (페이징)
     */
    public PageVO<ErrorHistoryVO> getErrorHistory(int page, int size,
                                                   String errorLevel, Long deviceId, Long devCodeId,
                                                   String startDate, String endDate,
                                                   String deviceName, String deviceIp,
                                                   String errorMessage, String groupName,
                                                   String sortKey, String sortDirection) {
        int offset = (page - 1) * size;
        List<ErrorHistoryVO> list = errorMapper.selectErrorHistory(
                errorLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName,
                sortKey, sortDirection, offset, size);
        int total = errorMapper.countErrorHistory(
                errorLevel, deviceId, devCodeId, startDate, endDate,
                deviceName, deviceIp, errorMessage, groupName);

        return PageVO.of(list, page, size, total);
    }

    /**
     * 장애 이력 상세 조회
     */
    public ErrorHistoryVO getErrorHistoryById(Long errorHistoryId) {
        return errorMapper.selectErrorHistoryById(errorHistoryId);
    }
}
