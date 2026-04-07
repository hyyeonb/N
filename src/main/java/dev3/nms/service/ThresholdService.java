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
        // MAX_VALUE 검증 — 베이스 테이블에서 MAX_VALUE 조회
        Integer maxValue = t.getMAX_VALUE();
        if (maxValue == null) {
            // 장비별 임계치인 경우 베이스에서 MAX_VALUE 조회
            List<ThresholdVO> baseList = thresholdMapper.findAllBase();
            for (ThresholdVO base : baseList) {
                if (base.getTYPE().equals(t.getTYPE())) {
                    maxValue = base.getMAX_VALUE();
                    break;
                }
            }
        }
        if (maxValue != null && maxValue > 0) {
            if (t.getCRITICAL() > maxValue || t.getMAJOR() > maxValue
                || t.getMINOR() > maxValue || t.getWARNING() > maxValue) {
                throw new IllegalArgumentException(
                    t.getTYPE() + ": 임계치 값은 최대 " + maxValue + "을(를) 초과할 수 없습니다.");
            }
            if (t.getWARNING() < 0) {
                throw new IllegalArgumentException(
                    t.getTYPE() + ": 임계치 값은 0 미만일 수 없습니다.");
            }
        }
    }
}
