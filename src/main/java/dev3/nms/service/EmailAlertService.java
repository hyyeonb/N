package dev3.nms.service;

import dev3.nms.mapper.EmailPrefMapper;
import dev3.nms.mapper.ErrorMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.vo.auth.UserVO;
import dev3.nms.vo.notification.EmailDevicePrefVO;
import dev3.nms.vo.notification.EmailPrefVO;
import dev3.nms.vo.notification.EmailTypePrefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAlertService {

    private final EmailPrefMapper emailPrefMapper;
    private final EmailService emailService;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final ErrorMapper errorMapper;

    // DB에서 로드한 고정 등급 매핑 (유형당 등급 1개인 경우)
    private volatile Map<String, String> fixedSeverityCache = null;

    private Map<String, String> getFixedSeverity() {
        if (fixedSeverityCache != null) return fixedSeverityCache;
        synchronized (this) {
            if (fixedSeverityCache != null) return fixedSeverityCache;
            Map<String, String> map = new HashMap<>();
            try {
                List<Map<String, Object>> types = errorMapper.findErrorCodeTypes();
                for (Map<String, Object> row : types) {
                    String code = String.valueOf(row.get("errorCodeNm"));
                    int count = ((Number) row.get("levelCount")).intValue();
                    String levels = String.valueOf(row.get("levels"));
                    if (count == 1) {
                        String severity = mapSeverity(levels.trim());
                        map.put(code, severity);
                        // alertType 변환명도 추가
                        if ("PING".equals(code)) {
                            map.put("PING_FAIL", severity);
                            map.put("ICMP", severity);
                        } else if ("SNMP".equals(code)) {
                            map.put("SNMP_FAIL", severity);
                        } else if ("PORT".equals(code)) {
                            map.put("PORT_DOWN", severity);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("고정 등급 로드 실패, 기본값 사용: {}", e.getMessage());
                map.put("PING", "CRITICAL"); map.put("PING_FAIL", "CRITICAL"); map.put("ICMP", "CRITICAL");
                map.put("SNMP", "MAJOR"); map.put("SNMP_FAIL", "MAJOR");
                map.put("PORT", "MAJOR"); map.put("PORT_DOWN", "MAJOR");
            }
            fixedSeverityCache = map;
            return map;
        }
    }

    // Alert type normalization
    private static final Map<String, String> TYPE_NORMALIZE = Map.of(
        "PING_FAIL", "ICMP",
        "PING_CLEAR", "ICMP",
        "SNMP_FAIL", "SNMP",
        "SNMP_CLEAR", "SNMP",
        "PORT_DOWN", "PORT",
        "PORT_UP", "PORT",
        "CPU_THRESHOLD", "CPU_MEM",
        "MEM_THRESHOLD", "CPU_MEM",
        "TRAFFIC_THRESHOLD", "TRAFFIC",
        "THRESHOLD_CLEAR", "CLEAR"
    );

    // Digest queue: userId -> list of pending alerts
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Map<String, Object>>> digestQueue = new ConcurrentHashMap<>();
    // 유저별 마지막 다이제스트 발송 시각 (ms)
    private final ConcurrentHashMap<Long, Long> lastDigestFlush = new ConcurrentHashMap<>();

    /**
     * Process incoming alert and decide email delivery per user
     */
    public void processAlert(Map<String, Object> alert) {
        if (!emailService.isEnabled()) return;
        if (Boolean.TRUE.equals(alert.get("isCleared"))) return;

        String rawAlertType = String.valueOf(alert.getOrDefault("alertType", ""));
        String normalizedType = TYPE_NORMALIZE.getOrDefault(rawAlertType, rawAlertType);
        if ("CLEAR".equals(normalizedType)) return;

        String severity = getFixedSeverity().containsKey(rawAlertType)
            ? getFixedSeverity().get(rawAlertType)
            : String.valueOf(alert.getOrDefault("severity", alert.getOrDefault("errorLevel", "")));

        severity = mapSeverity(severity);

        Integer deviceId = alert.get("deviceId") != null ? ((Number) alert.get("deviceId")).intValue() : null;
        if (deviceId == null) return;

        List<EmailPrefVO> enabledUsers = emailPrefMapper.findAllEnabled();
        if (enabledUsers == null || enabledUsers.isEmpty()) return;

        for (EmailPrefVO user : enabledUsers) {
            try {
                processAlertForUser(user, alert, deviceId, normalizedType, severity);
            } catch (Exception e) {
                log.warn("Email alert processing failed for userId={}: {}", user.getUSER_ID(), e.getMessage());
            }
        }
    }

    private void processAlertForUser(EmailPrefVO user, Map<String, Object> alert,
                                      int deviceId, String alertType, String severity) {
        Long userId = user.getUSER_ID();

        // Check device access permission
        List<Long> accessibleDevices = permissionService.getAccessibleDeviceIds(userId);
        if (accessibleDevices != null && !accessibleDevices.contains((long) deviceId)) {
            return;
        }

        // 1st priority: Device override
        EmailDevicePrefVO devicePref = emailPrefMapper.findDeviceMode(userId, deviceId, alertType, severity);
        String mode;
        if (devicePref != null) {
            mode = devicePref.getDELIVERY_MODE();
        } else {
            // 2nd priority: Global type x severity
            EmailTypePrefVO typePref = emailPrefMapper.findTypeMode(userId, alertType, severity);
            mode = typePref != null ? typePref.getDELIVERY_MODE() : "OFF";
        }

        if ("OFF".equals(mode) || mode == null) return;

        if ("IMMEDIATE".equals(mode)) {
            sendImmediateEmail(userId, alert, alertType, severity);
        } else if ("DIGEST".equals(mode)) {
            addToDigestQueue(userId, alert);
        }
    }

    private void sendImmediateEmail(Long userId, Map<String, Object> alert, String alertType, String severity) {
        String email = getUserEmail(userId);
        if (email == null || email.isBlank()) return;

        String deviceName = String.valueOf(alert.getOrDefault("deviceName", "-"));
        String deviceIp = String.valueOf(alert.getOrDefault("deviceIp", "-"));
        String message = String.valueOf(alert.getOrDefault("message", "-"));

        String subject = String.format("[NMS] %s - %s (%s)", severity, deviceName, alertType);
        String body = buildAlertHtml(deviceName, deviceIp, alertType, severity, message, LocalDateTime.now());

        emailService.sendHtml(email, subject, body);
    }

    public void addToDigestQueue(Long userId, Map<String, Object> alert) {
        digestQueue.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>()).add(alert);
    }

    /**
     * Called by scheduler (1분 주기) - 유저별 DIGEST_MINUTES에 따라 발송
     */
    public void flushDigest() {
        if (digestQueue.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (Map.Entry<Long, ConcurrentLinkedQueue<Map<String, Object>>> entry : digestQueue.entrySet()) {
            Long userId = entry.getKey();
            ConcurrentLinkedQueue<Map<String, Object>> queue = entry.getValue();

            if (queue.isEmpty()) continue;

            // 유저별 DIGEST_MINUTES 확인
            EmailPrefVO pref = emailPrefMapper.findByUserId(userId);
            int digestMinutes = (pref != null && pref.getDIGEST_MINUTES() != null && pref.getDIGEST_MINUTES() > 0)
                    ? pref.getDIGEST_MINUTES() : 5;

            // 정시 기준 정렬: 현재 시각이 주기 경계에 해당하는지 체크
            // 60분 → 매시 :00, 5분 → :00/:05/:10..., 1분 → 매분
            java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();
            int totalMinutes = nowTime.getHour() * 60 + nowTime.getMinute();
            if (totalMinutes % digestMinutes != 0) {
                continue; // 주기 경계 아님
            }

            // 같은 경계에서 중복 발송 방지
            long boundary = totalMinutes / digestMinutes;
            Long lastBoundary = lastDigestFlush.get(userId);
            if (lastBoundary != null && lastBoundary == boundary) {
                continue; // 이미 이 경계에서 발송함
            }

            List<Map<String, Object>> alerts = new ArrayList<>();
            Map<String, Object> alert;
            while ((alert = queue.poll()) != null) {
                alerts.add(alert);
            }

            if (alerts.isEmpty()) continue;

            String email = getUserEmail(userId);
            if (email == null || email.isBlank()) continue;

            String subject = String.format("[NMS] 장애 알림 요약 - %d건", alerts.size());
            String body = buildDigestHtml(alerts);
            emailService.sendHtml(email, subject, body);
            lastDigestFlush.put(userId, boundary);
        }
    }

    private String getUserEmail(Long userId) {
        return userMapper.findById(userId)
                .map(UserVO::getEMAIL)
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
    }

    private String mapSeverity(String severity) {
        if (severity == null) return "WARNING";
        return switch (severity.toUpperCase()) {
            case "C", "CRITICAL" -> "CRITICAL";
            case "M", "MAJOR" -> "MAJOR";
            case "N", "MINOR" -> "MINOR";
            case "W", "WARNING" -> "WARNING";
            default -> severity.toUpperCase();
        };
    }

    private String buildAlertHtml(String deviceName, String deviceIp, String alertType,
                                   String severity, String message, LocalDateTime time) {
        String severityColor = switch (severity) {
            case "CRITICAL" -> "#ef4444";
            case "MAJOR" -> "#f59e0b";
            case "MINOR" -> "#3b82f6";
            default -> "#64748b";
        };

        return """
            <div style="font-family: 'Pretendard', 'Inter', -apple-system, sans-serif; max-width: 500px; margin: 0 auto; background: #1a1a2e; border-radius: 12px; overflow: hidden; border: 1px solid #2d2d44;">
                <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 16px 24px;">
                    <h2 style="color: white; margin: 0; font-size: 16px;">NMS Alert</h2>
                </div>
                <div style="padding: 24px;">
                    <div style="display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; color: white; background: %s; margin-bottom: 16px;">%s</div>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px 0; color: #94a3b8; font-size: 13px; width: 80px;">Device</td><td style="padding: 8px 0; color: #f8fafc; font-size: 13px;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #94a3b8; font-size: 13px;">IP</td><td style="padding: 8px 0; color: #818cf8; font-family: monospace; font-size: 13px;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #94a3b8; font-size: 13px;">Type</td><td style="padding: 8px 0; color: #f8fafc; font-size: 13px;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #94a3b8; font-size: 13px;">Message</td><td style="padding: 8px 0; color: #f8fafc; font-size: 13px;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #94a3b8; font-size: 13px;">Time</td><td style="padding: 8px 0; color: #f8fafc; font-size: 13px;">%s</td></tr>
                    </table>
                </div>
                <div style="padding: 16px 24px; border-top: 1px solid #2d2d44; text-align: center;">
                    <span style="color: #64748b; font-size: 11px;">This email was sent automatically by NMS.</span>
                </div>
            </div>
            """.formatted(severityColor, severity, deviceName, deviceIp, alertType, message,
                time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private String buildDigestHtml(List<Map<String, Object>> alerts) {
        StringBuilder rows = new StringBuilder();
        int critical = 0, major = 0, minor = 0, warning = 0;

        for (Map<String, Object> a : alerts) {
            String deviceName = String.valueOf(a.getOrDefault("deviceName", "-"));
            String deviceIp = String.valueOf(a.getOrDefault("deviceIp", "-"));
            String alertType = String.valueOf(a.getOrDefault("alertType", "-"));
            String severity = mapSeverity(String.valueOf(a.getOrDefault("severity", a.getOrDefault("errorLevel", ""))));
            String message = String.valueOf(a.getOrDefault("message", "-"));

            String severityColor = switch (severity) {
                case "CRITICAL" -> { critical++; yield "#ef4444"; }
                case "MAJOR" -> { major++; yield "#f59e0b"; }
                case "MINOR" -> { minor++; yield "#3b82f6"; }
                default -> { warning++; yield "#64748b"; }
            };

            rows.append(String.format("""
                <tr>
                    <td style="padding:8px 6px; border-bottom:1px solid #2d2d44; color:#f8fafc; font-size:12px;">%s</td>
                    <td style="padding:8px 6px; border-bottom:1px solid #2d2d44; color:#818cf8; font-family:monospace; font-size:12px;">%s</td>
                    <td style="padding:8px 6px; border-bottom:1px solid #2d2d44; font-size:12px;"><span style="color:%s; font-weight:600;">%s</span></td>
                    <td style="padding:8px 6px; border-bottom:1px solid #2d2d44; color:#e2e8f0; font-size:12px;">%s</td>
                    <td style="padding:8px 6px; border-bottom:1px solid #2d2d44; color:#94a3b8; font-size:12px;">%s</td>
                </tr>
                """, deviceName, deviceIp, severityColor, severity, alertType, message));
        }

        String summary = String.format("Total %d (Critical %d / Major %d / Minor %d / Warning %d)",
            alerts.size(), critical, major, minor, warning);

        return """
            <div style="font-family: 'Pretendard', 'Inter', -apple-system, sans-serif; max-width: 700px; margin: 0 auto; background: #1a1a2e; border-radius: 12px; overflow: hidden; border: 1px solid #2d2d44;">
                <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 16px 24px;">
                    <h2 style="color: white; margin: 0; font-size: 16px;">NMS Alert Digest</h2>
                </div>
                <div style="padding: 24px;">
                    <table style="width: 100%%; border-collapse: collapse;">
                        <thead>
                            <tr style="border-bottom: 2px solid #2d2d44;">
                                <th style="padding:10px 6px; text-align:left; color:#94a3b8; font-size:11px; font-weight:500;">Device</th>
                                <th style="padding:10px 6px; text-align:left; color:#94a3b8; font-size:11px; font-weight:500;">IP</th>
                                <th style="padding:10px 6px; text-align:left; color:#94a3b8; font-size:11px; font-weight:500;">Severity</th>
                                <th style="padding:10px 6px; text-align:left; color:#94a3b8; font-size:11px; font-weight:500;">Type</th>
                                <th style="padding:10px 6px; text-align:left; color:#94a3b8; font-size:11px; font-weight:500;">Message</th>
                            </tr>
                        </thead>
                        <tbody>%s</tbody>
                    </table>
                    <p style="margin-top:16px; color:#94a3b8; font-size:12px;">%s</p>
                </div>
                <div style="padding: 16px 24px; border-top: 1px solid #2d2d44; text-align: center;">
                    <span style="color: #64748b; font-size: 11px;">This email was sent automatically by NMS.</span>
                </div>
            </div>
            """.formatted(rows.toString(), summary);
    }
}
