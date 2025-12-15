package dev3.nms.service;

import dev3.nms.mapper.SnmpMetricMapper;
import dev3.nms.mapper.SnmpModelOidMapper;
import dev3.nms.vo.mgmt.SnmpMetricVO;
import dev3.nms.vo.mgmt.SnmpModelOidVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnmpMetricService {

    private final SnmpMetricMapper snmpMetricMapper;
    private final SnmpModelOidMapper snmpModelOidMapper;

    /**
     * 모든 메트릭 조회
     */
    public List<SnmpMetricVO> getAllMetrics() {
        return snmpMetricMapper.findAll();
    }

    /**
     * 타입별 메트릭 조회
     */
    public List<SnmpMetricVO> getMetricsByType(String type) {
        return snmpMetricMapper.findByType(type);
    }

    /**
     * 모델의 OID 설정 조회
     */
    public List<SnmpModelOidVO> getModelOids(Integer modelId) {
        return snmpModelOidMapper.findByModelId(modelId);
    }

    /**
     * 모델 OID 저장 (단일)
     */
    @Transactional
    public void saveModelOid(SnmpModelOidVO oid) {
        if (oid.getOID() == null || oid.getOID().trim().isEmpty()) {
            // OID가 비어있으면 삭제
            snmpModelOidMapper.deleteByModelAndMetric(oid.getMODEL_ID(), oid.getMETRIC_ID());
        } else {
            snmpModelOidMapper.upsert(oid);
        }
    }

    /**
     * 모델 OID 일괄 저장
     */
    @Transactional
    public void saveModelOids(Integer modelId, List<SnmpModelOidVO> oids) {
        for (SnmpModelOidVO oid : oids) {
            oid.setMODEL_ID(modelId);
            saveModelOid(oid);
        }
    }

    /**
     * 모델 OID 삭제
     */
    @Transactional
    public void deleteModelOid(Integer oidId) {
        snmpModelOidMapper.delete(oidId);
    }

    /**
     * 모델의 모든 OID 삭제
     */
    @Transactional
    public void deleteAllModelOids(Integer modelId) {
        snmpModelOidMapper.deleteByModelId(modelId);
    }
}
