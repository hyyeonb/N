package dev3.nms.controller;

import dev3.nms.service.PermissionService;
import dev3.nms.service.SshSessionService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.ssh.SshSessionDto;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh")
public class SshSessionController {

    private final SshSessionService sshSessionService;
    private final PermissionService permissionService;

    /**
     * SSH 접속이력 목록 조회 (페이지네이션 + 검색)
     * 비관리자는 자신의 SSH 세션만 조회 가능
     */
    @GetMapping("/sessions")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.SessionRes>>> getSessionList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "SESSION_ID") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String search,
            HttpSession session
    ) {
        Long filterUserId = getFilterUserId(session);
        PageVO<SshSessionDto.SessionRes> result = sshSessionService.getSessionList(page, size, sort, order, search, filterUserId);
        ResVO<PageVO<SshSessionDto.SessionRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 세션별 명령어 목록 조회
     * 비관리자는 자신의 SSH 세션만 조회 가능
     */
    @GetMapping("/sessions/{sessionId}/commands")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.CommandRes>>> getCommands(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        Long filterUserId = getFilterUserId(session);
        PageVO<SshSessionDto.CommandRes> result = sshSessionService.getCommandsBySessionId(sessionId, page, size, filterUserId);
        ResVO<PageVO<SshSessionDto.CommandRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 세션별 SFTP 로그 목록 조회 (페이지네이션)
     * 비관리자는 자신의 SSH 세션만 조회 가능
     */
    @GetMapping("/sessions/{sessionId}/sftp-logs")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.SftpLogRes>>> getSftpLogs(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        Long filterUserId = getFilterUserId(session);
        PageVO<SshSessionDto.SftpLogRes> result = sshSessionService.getSftpLogsBySessionId(sessionId, page, size, filterUserId);
        ResVO<PageVO<SshSessionDto.SftpLogRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ========== 헬퍼 ==========

    /**
     * 비관리자인 경우 현재 사용자 ID 반환, 관리자는 null (전체 조회)
     */
    private Long getFilterUserId(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId != null && !permissionService.isAdmin(userId)) {
            return userId;
        }
        return null;
    }
}
