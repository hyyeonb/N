package dev3.nms.vo.topo;

import lombok.*;

import java.util.List;

public class UserTopoDto {

    // ===== 조회 응답 =====
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewRes {
        private Long viewId;
        private Long userId;
        private Long groupId;
        private Double zoom;
        private Double centerX;
        private Double centerY;
        @Setter private String backIconData;
        @Setter private List<NodeRes> nodes;
        @Setter private List<LinkRes> links;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeRes {
        private String id;
        private String nodeType;
        private Long groupId;
        private Long deviceId;
        private String iconData;
        private String name;
        private String type;
        private String ip;
        private String location;
        private String note;
        private Double x;
        private Double y;
        private Double fx;
        private Double fy;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkRes {
        private String source;
        private String target;
        private String srcIfIndex;
        private String dstIfIndex;
        private String srcType;
        private String dstType;
        private String srcIfName;
        private String dstIfName;
        private String status;
    }

    // ===== 저장 요청 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveReq {
        private Double zoom;
        private Double centerX;
        private Double centerY;
        private List<SaveNode> nodes;
        private List<SaveLink> links;
        private Long viewId; // upsert 후 세팅용
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveNode {
        private String id;
        private String nodeType;
        private Long deviceId;
        private Long groupId;
        private String iconData;
        private String name;
        private String type;
        private String ip;
        private String location;
        private String note;
        private Double x;
        private Double y;
        private Double fx;
        private Double fy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveLink {
        private String source;
        private String target;
        private String srcType;
        private String dstType;
        private String srcIfIndex;
        private String dstIfIndex;
        private String status;

        public String getSrcIfIndex() {
            return srcIfIndex == null ? "0" : srcIfIndex;
        }
        public String getDstIfIndex() {
            return dstIfIndex == null ? "0" : dstIfIndex;
        }
    }

    // ===== 배경 이미지 저장 =====
    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveBackImgReq {
        private Long userId;
        private Long groupId;           // 0 = 메인, 실제값 = 그룹 하위
        private String imgSrc;          // base64 형식 이미지
    }

    // ===== 노드 아이콘 저장 =====
    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveNodeImgReq {
        private Long userId;
        private Long groupId;           // 0 = 메인, 실제값 = 그룹 하위
        private String nodeKey;
        private String imgSrc;          // base64 형식 이미지
    }
}
