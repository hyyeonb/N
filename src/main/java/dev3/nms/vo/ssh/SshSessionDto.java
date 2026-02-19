package dev3.nms.vo.ssh;

import lombok.*;

public class SshSessionDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionRes {
        private Long sessionId;
        private Long userId;
        private String userName;
        private String host;
        private String sshUser;
        private String remoteAddr;
        private String connectedAt;
        private String disconnectedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandRes {
        private Long commandsId;
        private Long sessionId;
        private String command;
        private String executedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SftpLogRes {
        private Long logId;
        private Long sessionId;
        private Long userId;
        private String userName;
        private String operation;
        private String filePath;
        private Long fileSize;
        private String executedAt;
    }
}
