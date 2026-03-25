package dev3.nms.controller;

import dev3.nms.service.NotificationPrefService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.auth.NotificationPrefVO;
import dev3.nms.vo.common.ResVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationPrefController {

    private final NotificationPrefService notificationPrefService;

    /**
     * 알림 환경설정 조회
     */
    @GetMapping("/preferences")
    public ResponseEntity<ResVO<NotificationPrefVO>> getPreferences(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        NotificationPrefVO pref = notificationPrefService.getPreferences(userId);
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", pref));
    }

    /**
     * 알림 환경설정 저장
     */
    @PutMapping("/preferences")
    public ResponseEntity<ResVO<Void>> updatePreferences(
            @RequestBody NotificationPrefVO pref,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResVO<>(401, "로그인이 필요합니다.", null));
        }

        notificationPrefService.updatePreferences(userId, pref);
        return ResponseEntity.ok(new ResVO<>(200, "설정이 저장되었습니다.", null));
    }
}
