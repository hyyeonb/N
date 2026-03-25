package dev3.nms.service;

import dev3.nms.mapper.SshSessionMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.ssh.SshSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SshSessionService {

    private final SshSessionMapper sshSessionMapper;

    public PageVO<SshSessionDto.SessionRes> getSessionList(int page, int size, String sort, String order, String search, Long filterUserId) {
        int offset = (page - 1) * size;
        List<SshSessionDto.SessionRes> sessions = sshSessionMapper.findSessionsPaged(offset, size, sort, order, search, filterUserId);
        int totalCount = sshSessionMapper.countSessions(search, filterUserId);
        return PageVO.of(sessions, page, size, totalCount);
    }

    public PageVO<SshSessionDto.CommandRes> getCommandsBySessionId(Long sessionId, int page, int size, Long filterUserId) {
        int offset = (page - 1) * size;
        List<SshSessionDto.CommandRes> commands = sshSessionMapper.findCommandsBySessionId(sessionId, offset, size, filterUserId);
        int totalCount = sshSessionMapper.countCommandsBySessionId(sessionId, filterUserId);
        return PageVO.of(commands, page, size, totalCount);
    }

    public PageVO<SshSessionDto.SftpLogRes> getSftpLogsBySessionId(Long sessionId, int page, int size, Long filterUserId) {
        int offset = (page - 1) * size;
        List<SshSessionDto.SftpLogRes> logs = sshSessionMapper.findSftpLogsBySessionId(sessionId, offset, size, filterUserId);
        int totalCount = sshSessionMapper.countSftpLogsBySessionId(sessionId, filterUserId);
        return PageVO.of(logs, page, size, totalCount);
    }

    /**
     * 특정 장비(IP)의 SSH 세션 목록 조회
     */
    public PageVO<SshSessionDto.SessionRes> getSessionsByHost(String host, int page, int size) {
        int offset = (page - 1) * size;
        List<SshSessionDto.SessionRes> sessions = sshSessionMapper.findSessionsByHost(host, offset, size);
        int totalCount = sshSessionMapper.countSessionsByHost(host);
        return PageVO.of(sessions, page, size, totalCount);
    }
}
