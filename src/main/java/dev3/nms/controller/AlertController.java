package dev3.nms.controller;

import dev3.nms.service.AlertService;
import dev3.nms.vo.alert.AlertVO;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 알림 수신 (Kafka Consumer -> Backend)
     */
    @PostMapping
    public ResponseEntity<ResVO<Void>> receiveAlert(@RequestBody AlertVO alert) {
        alertService.processAlert(alert);
        return ResponseEntity.ok(new ResVO<>(200, "알림 처리 완료", null));
    }

    /**
     * 배치 알림 수신
     */
    @PostMapping("/batch")
    public ResponseEntity<ResVO<Void>> receiveAlerts(@RequestBody List<AlertVO> alerts) {
        alertService.processAlerts(alerts);
        return ResponseEntity.ok(new ResVO<>(200, alerts.size() + "건 처리 완료", null));
    }

    /**
     * 현재 장애 목록 조회
     */
    @GetMapping("/current")
    public ResponseEntity<ResVO<List<AlertVO>>> getCurrentAlerts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity) {
        List<AlertVO> alerts = alertService.getCurrentAlerts(category, severity);
        return ResponseEntity.ok(new ResVO<>(200, "현재 장애 목록 조회 성공", alerts));
    }

    /**
     * 특정 장비의 현재 장애 조회
     */
    @GetMapping("/current/device/{deviceId}")
    public ResponseEntity<ResVO<List<AlertVO>>> getCurrentAlertsByDevice(
            @PathVariable Integer deviceId) {
        List<AlertVO> alerts = alertService.getCurrentAlertsByDeviceId(deviceId);
        return ResponseEntity.ok(new ResVO<>(200, "장비 장애 조회 성공", alerts));
    }

    /**
     * 현재 장애 요약 (카운트)
     */
    @GetMapping("/current/summary")
    public ResponseEntity<ResVO<Map<String, Object>>> getCurrentAlertsSummary() {
        int critical = alertService.countCurrentAlerts(null, "CRITICAL");
        int major = alertService.countCurrentAlerts(null, "MAJOR");
        int minor = alertService.countCurrentAlerts(null, "MINOR");
        int warning = alertService.countCurrentAlerts(null, "WARNING");
        int total = critical + major + minor + warning;

        Map<String, Object> summary = Map.of(
                "total", total,
                "critical", critical,
                "major", major,
                "minor", minor,
                "warning", warning
        );

        return ResponseEntity.ok(new ResVO<>(200, "장애 요약 조회 성공", summary));
    }

    /**
     * 알림 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<ResVO<PageVO<AlertVO>>> getAlertHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageVO<AlertVO> history = alertService.getAlertHistory(
                page, size, category, deviceId, startDate, endDate);
        return ResponseEntity.ok(new ResVO<>(200, "알림 히스토리 조회 성공", history));
    }
}
