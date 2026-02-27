package dev3.nms.vo.notice;

import lombok.*;

public class NoticeDto {

    // ===== 목록 응답 =====
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostRes {
        private Long noticeId;
        private String title;
        private String isUrgent;
        private String isPinned;
        private int viewCnt;
        private Long userId;
        private String userName;
        private String createdAt;
    }

    // ===== 상세 응답 =====
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostDetailRes {
        private Long noticeId;
        private String title;
        private String content;
        private String isUrgent;
        private String isPinned;
        private int viewCnt;
        private Long userId;
        private String userName;
        private String createdAt;
        private String updatedAt;
    }

    // ===== 등록 요청 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreateReq {
        private String title;
        private String content;
        private String isUrgent;
        private String isPinned;
        private Long userId;
        private Long noticeId; // insert 후 generated key
    }

    // ===== 수정 요청 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostUpdateReq {
        private String title;
        private String content;
        private String isUrgent;
        private String isPinned;
    }

    // ===== 긴급공지 WebSocket 메시지 =====
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UrgentNoticeMsg {
        private Long noticeId;
        private String title;
        private String content;
        private String userName;
        private String createdAt;
    }
}
