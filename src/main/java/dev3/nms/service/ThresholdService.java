package dev3.nms.service;

import dev3.nms.mapper.ThresholdMapper;
import dev3.nms.vo.mgmt.ThresholdVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThresholdService {

    private final ThresholdMapper thresholdMapper;

    public List<ThresholdVO> getBaseThresholds() {
        return thresholdMapper.findAllBase();
    }

    @Transactional
    public void updateBaseThresholds(List<ThresholdVO> thresholds) {
        for (ThresholdVO t : thresholds) {
            validateOrder(t);
            thresholdMapper.updateBase(t);
        }
        log.info("[Threshold] 시스템 임계치 수정 - {}건", thresholds.size());
    }

    // ==================== 장비별 오버라이드 ====================

    public List<ThresholdVO> getDeviceThresholds() {
        return thresholdMapper.findAllDevice();
    }

    public List<ThresholdVO> getDeviceThreshold(String deviceId) {
        return thresholdMapper.findByDeviceId(deviceId);
    }

    @Transactional
    public void upsertDeviceThresholds(String deviceId, List<ThresholdVO> thresholds) {
        for (ThresholdVO t : thresholds) {
            validateOrder(t);
            t.setDEVICE_ID(deviceId);
            thresholdMapper.upsertDevice(t);
        }
        log.info("[Threshold] 장비별 임계치 저장 - deviceId: {}, {}건", deviceId, thresholds.size());
    }

    @Transactional
    public void deleteDeviceThresholds(String deviceId) {
        thresholdMapper.deleteByDeviceId(deviceId);
        log.info("[Threshold] 장비별 임계치 삭제 - deviceId: {}", deviceId);
    }

    private void validateOrder(ThresholdVO t) {
        if (t.getCRITICAL() < t.getMAJOR() || t.getMAJOR() < t.getMINOR() || t.getMINOR() < t.getWARNING()) {
            throw new IllegalArgumentException(
                t.getTYPE() + ": Critical > Major > Minor > Warning 순서여야 합니다.");
        }
    }
}
