package dev3.nms.service;

import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.PortMapper;
import dev3.nms.vo.mgmt.DeviceVO;
import dev3.nms.vo.mgmt.PortVO;
import dev3.nms.service.ErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortService {

    private final PortMapper portMapper;
    private final DeviceMapper deviceMapper;
    private final ErrorService errorService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 장비의 Ethernet 포트 조회 (물리 포트만)
     */
    public List<PortVO> getPortsByDeviceId(Integer deviceId) {
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
     * IF_OPER_FLAG = false 설정 시 해당 포트의 활성 PORT 장애 자동 해소
     */
    @Transactional
    public void updatePort(PortVO port) {
        portMapper.updatePort(port);
        log.info("포트 수정 완료 - DeviceId: {}, IfIndex: {}", port.getDEVICE_ID(), port.getIF_INDEX());

        if (Boolean.FALSE.equals(port.getIF_OPER_FLAG())) {
            errorService.clearPortError(port.getDEVICE_ID(), port.getIF_INDEX());
            broadcastPortUpAlert(port.getDEVICE_ID(), port.getIF_INDEX());
        }
    }

    /**
     * 포트 감시 설정 수정
     * IF_OPER_FLAG = false 설정 시 해당 포트의 활성 PORT 장애 자동 해소
     */
    @Transactional
    public void updateMonitorSettings(Integer deviceId, Integer ifIndex, Boolean ifOperFlag, Boolean ifPerfFlag) {
        portMapper.updateMonitorSettings(deviceId, ifIndex, ifOperFlag, ifPerfFlag);
        log.info("포트 감시 설정 수정 - DeviceId: {}, IfIndex: {}, OperFlag: {}, PerfFlag: {}",
                deviceId, ifIndex, ifOperFlag, ifPerfFlag);

        if (Boolean.FALSE.equals(ifOperFlag)) {
            errorService.clearPortError(deviceId, ifIndex);
            broadcastPortUpAlert(deviceId, ifIndex);
        }
    }

    /**
     * 포트 삭제 + 해당 포트의 활성 장애 자동 해소
     */
    @Transactional
    public void deletePort(Integer deviceId, Integer ifIndex) {
        errorService.clearPortError(deviceId, ifIndex);
        portMapper.deletePort(deviceId, ifIndex);
        log.info("포트 삭제 완료 (장애 해소 포함) - DeviceId: {}, IfIndex: {}", deviceId, ifIndex);
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
     * INSERT ... ON DUPLICATE KEY UPDATE 사용 (1개 쿼리로 처리)
     * - 신규 포트: 기본 감시 설정(ON)으로 INSERT
     * - 기존 포트: 감시 설정(IF_OPER_FLAG, IF_PERF_FLAG) 유지, 나머지 업데이트
     */
    @Transactional
    public void saveOrUpdatePorts(Integer deviceId, List<PortVO> ports) {
        if (ports == null || ports.isEmpty()) {
            return;
        }

        for (PortVO port : ports) {
            port.setDEVICE_ID(deviceId);
        }

        portMapper.upsertPorts(ports);
        log.info("포트 저장/업데이트 완료 - DeviceId: {}, 포트 수: {}", deviceId, ports.size());

        // 삭제된 포트(DELETE_AT IS NOT NULL)의 잔존 Oper 장애 정리
        // Docker 컨테이너 등 동적 IF가 사라진 후 남은 장애 자동 해소
        errorService.cleanupDeletedPortErrors(deviceId);
    }

    /**
     * 포트 비감시 전환 시 PORT_UP 해소 알림을 WebSocket으로 브로드캐스트
     * Kafka Consumer의 AlertDTO.createPortUpAlert()와 동일한 포맷
     */
    private void broadcastPortUpAlert(Integer deviceId, Integer ifIndex) {
        try {
            DeviceVO device = deviceMapper.findDeviceById(deviceId);
            PortVO port = portMapper.findByDeviceIdAndIfIndex(deviceId, ifIndex);

            String deviceName = device != null ? device.getDEVICE_NAME() : "Unknown";
            String deviceIp = device != null ? device.getDEVICE_IP() : "";
            String ifName = port != null ? port.getIF_NAME() : "Unknown";

            Map<String, Object> alert = new HashMap<>();
            alert.put("category", "PORT");
            alert.put("alertType", "PORT_UP");
            alert.put("severity", "INFO");
            alert.put("deviceId", deviceId);
            alert.put("deviceName", deviceName);
            alert.put("deviceIp", deviceIp);
            alert.put("ifIndex", ifIndex);
            alert.put("ifName", ifName);
            alert.put("message", String.format("포트 비관리 설정으로 장애 해소 - %s (ifIndex: %d)", ifName, ifIndex));
            alert.put("isCleared", true);
            alert.put("occurredAt", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/alerts", (Object) alert);
            log.info("PORT_UP 해소 알림 브로드캐스트 - DeviceId: {}, IfIndex: {}", deviceId, ifIndex);
        } catch (Exception e) {
            log.warn("PORT_UP 알림 브로드캐스트 실패 - DeviceId: {}, IfIndex: {}: {}", deviceId, ifIndex, e.getMessage());
        }
    }

}
