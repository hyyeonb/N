package dev3.nms.config;

import dev3.nms.service.EmailAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDigestScheduler {

    private final EmailAlertService emailAlertService;

    // 1분마다 실행, 유저별 DIGEST_MINUTES에 따라 발송 여부 판단
    @Scheduled(fixedRate = 60000)
    public void flushDigest() {
        try {
            emailAlertService.flushDigest();
        } catch (Exception e) {
            log.error("Digest flush failed: {}", e.getMessage());
        }
    }
}
