package dev3.nms.controller;

import dev3.nms.service.SnmpMetricService;
import dev3.nms.vo.mgmt.SnmpMetricVO;
import dev3.nms.vo.mgmt.SnmpModelOidVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/snmp")
@RequiredArgsConstructor
public class SnmpMetricController {

    private final SnmpMetricService snmpMetricService;

    /**
     * 모든 메트릭 조회
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SnmpMetricVO> metrics = snmpMetricService.getAllMetrics();
            response.put("success", true);
            response.put("data", metrics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("메트릭 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 모델의 OID 설정 조회
     */
    @GetMapping("/model/{modelId}/oids")
    public ResponseEntity<Map<String, Object>> getModelOids(@PathVariable Integer modelId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SnmpModelOidVO> oids = snmpMetricService.getModelOids(modelId);
            response.put("success", true);
            response.put("data", oids);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("모델 OID 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 모델 OID 저장 (단일)
     */
    @PostMapping("/model/{modelId}/oid")
    public ResponseEntity<Map<String, Object>> saveModelOid(
            @PathVariable Integer modelId,
            @RequestBody SnmpModelOidVO oid) {
        Map<String, Object> response = new HashMap<>();
        try {
            oid.setMODEL_ID(modelId);
            snmpMetricService.saveModelOid(oid);
            response.put("success", true);
            response.put("message", "OID가 저장되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OID 저장 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 모델 OID 일괄 저장
     */
    @PostMapping("/model/{modelId}/oids")
    public ResponseEntity<Map<String, Object>> saveModelOids(
            @PathVariable Integer modelId,
            @RequestBody List<SnmpModelOidVO> oids) {
        Map<String, Object> response = new HashMap<>();
        try {
            snmpMetricService.saveModelOids(modelId, oids);
            response.put("success", true);
            response.put("message", "OID 설정이 저장되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OID 일괄 저장 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 모델 OID 삭제
     */
    @DeleteMapping("/oid/{oidId}")
    public ResponseEntity<Map<String, Object>> deleteModelOid(@PathVariable Integer oidId) {
        Map<String, Object> response = new HashMap<>();
        try {
            snmpMetricService.deleteModelOid(oidId);
            response.put("success", true);
            response.put("message", "OID가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OID 삭제 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
