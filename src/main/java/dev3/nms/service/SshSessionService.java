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

    public PageVO<SshSessionDto.SessionRes> getSessionList(int page, int size, String sort, String order, String search) {
        int offset = (page - 1) * size;
        List<SshSessionDto.SessionRes> sessions = sshSessionMapper.findSessionsPaged(offset, size, sort, order, search);
        int totalCount = sshSessionMapper.countSessions(search);
        return PageVO.of(sessions, page, size, totalCount);
    }

    public PageVO<SshSessionDto.CommandRes> getCommandsBySessionId(Long sessionId, int page, int size) {
        int offset = (page - 1) * size;
        List<SshSessionDto.CommandRes> commands = sshSessionMapper.findCommandsBySessionId(sessionId, offset, size);
        int totalCount = sshSessionMapper.countCommandsBySessionId(sessionId);
        return PageVO.of(commands, page, size, totalCount);
    }

    public PageVO<SshSessionDto.SftpLogRes> getSftpLogsBySessionId(Long sessionId, int page, int size) {
        int offset = (page - 1) * size;
        List<SshSessionDto.SftpLogRes> logs = sshSessionMapper.findSftpLogsBySessionId(sessionId, offset, size);
        int totalCount = sshSessionMapper.countSftpLogsBySessionId(sessionId);
        return PageVO.of(logs, page, size, totalCount);
    }
}
