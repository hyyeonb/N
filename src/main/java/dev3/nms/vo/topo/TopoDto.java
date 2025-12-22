package dev3.nms.vo.topo;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
public class TopoDto {
    /*--------------------------------------------------------------------------*/
    @Getter
    @AllArgsConstructor
    public static class TopoViewDtoReq {
        private Long id;
        private String type;
    }

    @Getter
    @Builder
    public static class TopoViewDtoRes {
        @Setter
        private String backIconData;
        private List<TopoViewNode> nodes;
        private List<TopoViewLink> links;
    }

    @Getter
    @AllArgsConstructor
    public static class TopoViewNode {
        private String id;           // nodeKey → id
        private String nodeType;     // "group" | "device"
        private Long groupId;
        private Long deviceId;
        private String iconData;
        private String name;         // nodeName → name
        private String type;         // MODEL_NAME (아이콘용) - 추가
        private String ip;           // IP_ADDR - 추가
        private String location;     // LOCATION - 추가 (선택)
        private String note;         // NOTE - 추가 (선택)
        private Double x;            // Integer → Double 권장
        private Double y;
        private Double fx;
        private Double fy;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoViewLink {
        private String source;       // SRC_NODE_KEY
        private String target;       // DST_NODE_KEY
        private String srcIfIndex;
        private String dstIfIndex;
        private String srcType;
        private String dstType;
        private String srcIfName;
        private String dstIfName;
        private String status;
    }
    /*--------------------------------------------------------------------------*/
    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoSaveDtoReq {
        private Long id;           // groupId 또는 deviceId
        private String type;       // "group" | "device"
        private Long viewId;
        private Double zoom;       // 줌 레벨
        private Double centerX;    // 중심 X 좌표
        private Double centerY;    // 중심 Y 좌표
        private List<TopoSaveNode> nodes;
        private List<TopoSaveLink> links;
    }
    @ToString
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoSaveNode {
        private String id;         // NODE_KEY
        private String nodeType;   // "device" | "group"
        private Long deviceId;     // nullable
        private Long groupId;      // nullable
        private String name;
        private String type;       // MODEL_NAME
        private String ip;
        private String location;
        private String note;
        private Double x;
        private Double y;
        private Double fx;
        private Double fy;
    }
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoSaveLink {
        @Getter private String source;     // SRC_NODE_KEY
        @Getter private String target;     // DST_NODE_KEY
        private String srcType;
        private String dstType;
        private String srcIfIndex;
        private String dstIfIndex;
        @Getter private String status;

        public String getSrcIfIndex() {
            return srcIfIndex == null ? "0" : srcIfIndex;
        }
        public String getDstIfIndex() {
            return dstIfIndex == null ? "0" : dstIfIndex;
        }
    }
    /*--------------------------------------------------------------------------*/

    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoSaveBackImgDtoReq {
        private Long groupId;           // groupId 또는 deviceId
        private String imgSrc;          // base64 형식 이미지
    }

    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopoSaveImgDtoReq {
        private Long id;           // groupId 또는 deviceId
        private String type;
        @Setter Long modelId;
        private String imgSrc;          // base64 형식 이미지
    }
}
