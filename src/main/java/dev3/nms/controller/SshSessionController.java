package dev3.nms.controller;

import dev3.nms.service.SshSessionService;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.ssh.SshSessionDto;

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

    /**
     * SSH 접속이력 목록 조회 (페이지네이션 + 검색)

     */
    @GetMapping("/sessions")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.SessionRes>>> getSessionList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "SESSION_ID") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String search
    ) {
        PageVO<SshSessionDto.SessionRes> result = sshSessionService.getSessionList(page, size, sort, order, search);
        ResVO<PageVO<SshSessionDto.SessionRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 세션별 명령어 목록 조회
     */
    @GetMapping("/sessions/{sessionId}/commands")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.CommandRes>>> getCommands(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageVO<SshSessionDto.CommandRes> result = sshSessionService.getCommandsBySessionId(sessionId, page, size);
        ResVO<PageVO<SshSessionDto.CommandRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 세션별 SFTP 로그 목록 조회 (페이지네이션)
     */
    @GetMapping("/sessions/{sessionId}/sftp-logs")
    public ResponseEntity<ResVO<PageVO<SshSessionDto.SftpLogRes>>> getSftpLogs(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageVO<SshSessionDto.SftpLogRes> result = sshSessionService.getSftpLogsBySessionId(sessionId, page, size);
        ResVO<PageVO<SshSessionDto.SftpLogRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
