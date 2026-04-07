package dev3.nms.controller;

import dev3.nms.service.EmailAlertService;
import dev3.nms.vo.common.ResVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Alert API - Kafka Consumer → WebSocket 브로드캐스트
 * 인증 제외 경로 (/api/alerts/**)
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmailAlertService emailAlertService;

    /**
     * 단건 알림 수신 및 WebSocket 브로드캐스트
     */
    @PostMapping
    public ResVO<Void> receiveAlert(@RequestBody Map<String, Object> alert) {
        messagingTemplate.convertAndSend("/topic/alerts", (Object) alert);
        emailAlertService.processAlert(alert);

        String category = (String) alert.getOrDefault("category", "");
        String alertType = (String) alert.getOrDefault("alertType", "");
        log.debug("Alert broadcast: {} / {}", category, alertType);

        return ResVO.<Void>builder().code(200).message("Alert broadcast 완료").build();
    }

    /**
     * 배치 알림 수신 및 WebSocket 브로드캐스트
     */
    @PostMapping("/batch")
    public ResVO<Void> receiveAlerts(@RequestBody List<Map<String, Object>> alerts) {
        for (Map<String, Object> alert : alerts) {
            messagingTemplate.convertAndSend("/topic/alerts", (Object) alert);
            emailAlertService.processAlert(alert);
        }
        log.debug("Alert batch broadcast: {}건", alerts.size());
        return ResVO.<Void>builder().code(200).message("Alert batch broadcast 완료").build();
    }
}
