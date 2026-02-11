package dev3.nms.vo.board;

import lombok.*;

import java.util.List;

public class BoardFileDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostRes {
        private Long postId;
        private String category;
        private String title;
        private String content;
        private int viewCnt;
        private String isPublic;
        private Long userId;
        private String createdAt;
        private String updatedAt;
        private int attachCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostDetailRes {
        private Long postId;
        private String category;
        private String title;
        private String content;
        private int viewCnt;
        private String isPublic;
        private Long userId;
        private String createdAt;
        private String updatedAt;
        @Setter private List<AttachRes> attachments;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachRes {
        private Long attachId;
        private Long postId;
        private String orgFileName;
        private Long fileSize;
        private String mimeType;
        private int downCnt;
        private String createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreateReq {
        private String category;
        private String title;
        private String content;
        private String isPublic;
        private Long userId;
        private Long postId; // insertPost 후 generated key 세팅용
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostUpdateReq {
        private String category;
        private String title;
        private String content;
        private String isPublic;
        private List<Long> deleteAttachIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachInsertReq {
        private Long postId;
        private String orgFileName;
        private String storedName;
        private String filePath;
        private Long fileSize;
        private String mimeType;
        private Long userId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachFileInfo {
        private Long attachId;
        private String orgFileName;
        private String storedName;
        private String filePath;
        private String mimeType;
    }
}
