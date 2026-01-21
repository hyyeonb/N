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
     * 특정 장비의 Ethernet 포트 조회 (차트 플래그 자동 초기화 포함, 물리 포트만)
     */
    @Transactional
    public List<PortVO> getPortsByDeviceId(Integer deviceId) {
        // 차트 플래그가 설정된 포트가 없으면 자동으로 TOP 5 설정
        int chartCount = portMapper.countChartEnabledPorts(deviceId);
        if (chartCount == 0) {
            initializeChartPorts(deviceId, 5);
        }
        return portMapper.findByDeviceId(deviceId);
    }

    /**
     * 특정 장비의 모든 포트 조회 (타입 제한 없음)
     */
    public List<PortVO> getAllPortsByDeviceId(Integer deviceId) {
        return portMapper.findAllByDeviceId(deviceId);
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

    /**
     * 포트 정보 저장 또는 업데이트 (SNMP 수집 후)
     * 기존 포트가 있으면 업데이트, 없으면 새로 생성
     */
    @Transactional
    public void saveOrUpdatePorts(Integer deviceId, List<PortVO> ports) {
        if (ports == null || ports.isEmpty()) {
            return;
        }

        for (PortVO port : ports) {
            port.setDEVICE_ID(deviceId);
            PortVO existingPort = portMapper.findByDeviceIdAndIfIndex(deviceId, port.getIF_INDEX());

            if (existingPort != null) {
                // 기존 포트 업데이트 (감시 설정은 유지)
                port.setIF_OPER_FLAG(existingPort.getIF_OPER_FLAG());
                port.setIF_PERF_FLAG(existingPort.getIF_PERF_FLAG());
                portMapper.updatePort(port);
            } else {
                // 새 포트 등록 (기본 감시 설정: ON)
                port.setIF_OPER_FLAG(true);
                port.setIF_PERF_FLAG(true);
                portMapper.insertPort(port);
            }
        }

        log.info("포트 저장/업데이트 완료 - DeviceId: {}, 포트 수: {}", deviceId, ports.size());
    }

    /**
     * 차트 플래그 토글
     */
    @Transactional
    public void toggleChartFlag(Integer deviceId, Integer ifIndex) {
        PortVO port = portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);
        if (port != null) {
            Boolean newFlag = !(port.getIF_CHART_FLAG() != null && port.getIF_CHART_FLAG());
            portMapper.updateChartFlag(deviceId, ifIndex, newFlag);
            log.info("차트 플래그 토글 - DeviceId: {}, IfIndex: {}, NewFlag: {}", deviceId, ifIndex, newFlag);
        }
    }

    /**
     * 차트 플래그 설정
     */
    @Transactional
    public void setChartFlag(Integer deviceId, Integer ifIndex, Boolean chartFlag) {
        portMapper.updateChartFlag(deviceId, ifIndex, chartFlag);
        log.info("차트 플래그 설정 - DeviceId: {}, IfIndex: {}, Flag: {}", deviceId, ifIndex, chartFlag);
    }

    /**
     * 차트 표시 포트 조회 (없으면 TOP 5 자동 설정)
     */
    @Transactional
    public List<PortVO> getChartEnabledPorts(Integer deviceId) {
        int chartCount = portMapper.countChartEnabledPorts(deviceId);

        // 차트 표시 포트가 없으면 TOP 5 자동 설정
        if (chartCount == 0) {
            initializeChartPorts(deviceId, 5);
        }

        return portMapper.findChartEnabledPorts(deviceId);
    }

    /**
     * TOP N 트래픽 포트로 차트 초기화
     */
    @Transactional
    public void initializeChartPorts(Integer deviceId, int limit) {
        // 기존 차트 플래그 초기화
        portMapper.resetChartFlags(deviceId);

        // TOP N 트래픽 포트에 차트 플래그 설정
        int updated = portMapper.setTopTrafficPortsChartFlag(deviceId, limit);

        // 트래픽 데이터가 없으면 첫 N개 포트에 플래그 설정
        if (updated == 0) {
            List<PortVO> ports = portMapper.findByDeviceId(deviceId);
            int count = 0;
            for (PortVO port : ports) {
                if (count >= limit) break;
                portMapper.updateChartFlag(deviceId, port.getIF_INDEX(), true);
                count++;
            }
            log.info("트래픽 데이터 없음 - 첫 {}개 포트에 차트 플래그 설정 (DeviceId: {})", count, deviceId);
        } else {
            log.info("TOP {} 트래픽 포트에 차트 플래그 설정 완료 (DeviceId: {})", updated, deviceId);
        }
    }

    /**
     * 차트 플래그 전체 초기화
     */
    @Transactional
    public void resetChartFlags(Integer deviceId) {
        portMapper.resetChartFlags(deviceId);
        log.info("차트 플래그 초기화 완료 - DeviceId: {}", deviceId);
    }
}
