package dev3.nms.service;

import dev3.nms.mapper.PortMapper;
import dev3.nms.vo.mgmt.PortVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortService {

    private final PortMapper portMapper;

    /**
     * 특정 장비의 모든 포트 조회
     */
    public List<PortVO> getPortsByDeviceId(Integer deviceId) {
        return portMapper.findByDeviceId(deviceId);
    }

    /**
     * 특정 포트 조회
     */
    public PortVO getPort(Integer deviceId, Integer ifIndex) {
        return portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);
    }

    /**
     * 모든 포트 조회
     */
    public List<PortVO> getAllPorts() {
        return portMapper.findAll();
    }

    /**
     * 감시 대상 포트만 조회
     */
    public List<PortVO> getMonitoredPorts() {
        return portMapper.findMonitoredPorts();
    }

    /**
     * 포트 등록
     */
    @Transactional
    public void createPort(PortVO port) {
        portMapper.insertPort(port);
        log.info("포트 등록 완료 - DeviceId: {}, IfIndex: {}", port.getDEVICE_ID(), port.getIF_INDEX());
    }

    /**
     * 포트 일괄 등록
     */
    @Transactional
    public void createPorts(List<PortVO> ports) {
        if (ports == null || ports.isEmpty()) {
            return;
        }
        portMapper.insertPorts(ports);
        log.info("포트 일괄 등록 완료 - {} 개", ports.size());
    }

    /**
     * 포트 정보 수정
     */
    @Transactional
    public void updatePort(PortVO port) {
        portMapper.updatePort(port);
        log.info("포트 수정 완료 - DeviceId: {}, IfIndex: {}", port.getDEVICE_ID(), port.getIF_INDEX());
    }

    /**
     * 포트 감시 설정 수정
     */
    @Transactional
    public void updateMonitorSettings(Integer deviceId, Integer ifIndex, Boolean ifOperFlag, Boolean ifPerfFlag) {
        portMapper.updateMonitorSettings(deviceId, ifIndex, ifOperFlag, ifPerfFlag);
        log.info("포트 감시 설정 수정 - DeviceId: {}, IfIndex: {}, OperFlag: {}, PerfFlag: {}",
                deviceId, ifIndex, ifOperFlag, ifPerfFlag);
    }

    /**
     * 포트 삭제
     */
    @Transactional
    public void deletePort(Integer deviceId, Integer ifIndex) {
        portMapper.deletePort(deviceId, ifIndex);
        log.info("포트 삭제 완료 - DeviceId: {}, IfIndex: {}", deviceId, ifIndex);
    }

    /**
     * 장비의 모든 포트 삭제
     */
    @Transactional
    public void deletePortsByDeviceId(Integer deviceId) {
        portMapper.deletePortsByDeviceId(deviceId);
        log.info("장비의 모든 포트 삭제 완료 - DeviceId: {}", deviceId);
    }

    /**
     * 특정 장비의 포트 개수 조회
     */
    public int countPortsByDeviceId(Integer deviceId) {
        return portMapper.countByDeviceId(deviceId);
    }
}
