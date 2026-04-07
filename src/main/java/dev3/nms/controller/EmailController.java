package dev3.nms.controller;

import dev3.nms.mapper.EmailPrefMapper;
import dev3.nms.mapper.ErrorMapper;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.notification.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailPrefMapper emailPrefMapper;
    private final ErrorMapper errorMapper;

    // === User Email Preferences ===

    @GetMapping("/preferences")
    public ResponseEntity<ResVO<Map<String, Object>>> getPreferences(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return unauthorized();

        EmailPrefVO pref = emailPrefMapper.findByUserId(userId);
        List<EmailTypePrefVO> typePrefs = emailPrefMapper.findTypePrefs(userId);
        List<EmailDevicePrefVO> devicePrefs = emailPrefMapper.findDevicePrefs(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("pref", pref);
        result.put("typePrefs", typePrefs);
        result.put("devicePrefs", devicePrefs);

        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", result));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ResVO<Void>> savePreferences(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return unauthorized();

        // Master pref
        EmailPrefVO pref = new EmailPrefVO();
        pref.setUSER_ID(userId);
        pref.setEMAIL_ENABLED((Boolean) body.getOrDefault("emailEnabled", false));
        pref.setDIGEST_MINUTES((Integer) body.getOrDefault("digestMinutes", 5));
        emailPrefMapper.upsertPref(pref);

        // Type prefs (replace all)
        @SuppressWarnings("unchecked")
        List<EmailTypePrefVO> typePrefs = ((List<Map<String, String>>) body.getOrDefault("typePrefs", List.of()))
            .stream()
            .map(m -> EmailTypePrefVO.builder()
                .USER_ID(userId)
                .ALERT_TYPE(m.get("ALERT_TYPE"))
                .SEVERITY(m.get("SEVERITY"))
                .DELIVERY_MODE(m.get("DELIVERY_MODE"))
                .build())
            .toList();

        emailPrefMapper.deleteTypePrefs(userId);
        if (!typePrefs.isEmpty()) {
            emailPrefMapper.insertTypePrefs(userId, typePrefs);
        }

        return ResponseEntity.ok(new ResVO<>(200, "저장 성공", null));
    }

    // === Device Override ===

    @PutMapping("/preferences/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> saveDevicePrefs(@PathVariable Integer deviceId,
                                                        @RequestBody List<EmailDevicePrefVO> prefs,
                                                        HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return unauthorized();

        emailPrefMapper.deleteDevicePrefs(userId, deviceId);
        if (prefs != null && !prefs.isEmpty()) {
            prefs.forEach(p -> {
                p.setUSER_ID(userId);
                p.setDEVICE_ID(deviceId);
            });
            emailPrefMapper.insertDevicePrefs(prefs);
        }

        return ResponseEntity.ok(new ResVO<>(200, "저장 성공", null));
    }

    @DeleteMapping("/preferences/devices/{deviceId}")
    public ResponseEntity<ResVO<Void>> deleteDevicePrefs(@PathVariable Integer deviceId, HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return unauthorized();

        emailPrefMapper.deleteDevicePrefs(userId, deviceId);
        return ResponseEntity.ok(new ResVO<>(200, "삭제 성공", null));
    }

    // === System Email (Admin only) ===

    @GetMapping("/system")
    public ResponseEntity<ResVO<List<SystemEmailVO>>> getSystemEmails() {
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", emailPrefMapper.findAllSystemEmails()));
    }

    @PostMapping("/system")
    public ResponseEntity<ResVO<Void>> addSystemEmail(@RequestBody SystemEmailVO vo) {
        emailPrefMapper.insertSystemEmail(vo);
        return ResponseEntity.ok(new ResVO<>(200, "등록 성공", null));
    }

    @PutMapping("/system/{emailId}")
    public ResponseEntity<ResVO<Void>> updateSystemEmail(@PathVariable Integer emailId, @RequestBody SystemEmailVO vo) {
        vo.setEMAIL_ID(emailId);
        emailPrefMapper.updateSystemEmail(vo);
        return ResponseEntity.ok(new ResVO<>(200, "수정 성공", null));
    }

    @DeleteMapping("/system/{emailId}")
    public ResponseEntity<ResVO<Void>> deleteSystemEmail(@PathVariable Integer emailId) {
        emailPrefMapper.deleteSystemEmail(emailId);
        return ResponseEntity.ok(new ResVO<>(200, "삭제 성공", null));
    }

    // === 장애 유형별 등급 정보 (고정/임계치 구분) ===

    @GetMapping("/alert-types")
    public ResponseEntity<ResVO<List<Map<String, Object>>>> getAlertTypes() {
        return ResponseEntity.ok(new ResVO<>(200, "조회 성공", errorMapper.findErrorCodeTypes()));
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ResVO<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ResVO<>(401, "로그인이 필요합니다.", null));
    }
}
