package dev3.nms.mapper;

import dev3.nms.vo.ssh.SshSessionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SshSessionMapper {

    List<SshSessionDto.SessionRes> findSessionsPaged(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sort") String sort,
            @Param("order") String order,
            @Param("search") String search,
            @Param("filterUserId") Long filterUserId
    );

    int countSessions(@Param("search") String search, @Param("filterUserId") Long filterUserId);

    List<SshSessionDto.CommandRes> findCommandsBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("filterUserId") Long filterUserId
    );

    int countCommandsBySessionId(@Param("sessionId") Long sessionId, @Param("filterUserId") Long filterUserId);

    List<SshSessionDto.SftpLogRes> findSftpLogsBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("filterUserId") Long filterUserId
    );

    int countSftpLogsBySessionId(@Param("sessionId") Long sessionId, @Param("filterUserId") Long filterUserId);

    List<SshSessionDto.SessionRes> findSessionsByHost(
            @Param("host") String host,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countSessionsByHost(@Param("host") String host);
}
