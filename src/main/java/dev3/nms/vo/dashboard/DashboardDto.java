package dev3.nms.vo.dashboard;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class DashboardDto {

    @Getter
    @Builder
    public static class WidgetsRes {
        private Long widgetId;
        private String widgetCode;
        private String name;
        private String icon;
        private String category;
        private int defaultW;
        private int defaultH;
        private int minW;
        private int minH;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultWidgetRes {
        private Long defaultDashboardWidgetId;        // 기본 대시보드 id
        private Long widgetId;
        private String widgetCode;
        private String name;
        private String icon;
        private String category;
        private String config;
        private String title;
        private int x;
        private int y;
        private int width;
        private int height;
        @Setter private Object chartData;
        @Setter private Object cntData;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWidgetRes{
        private Long userDashboardWidgetId;        // 기본 대시보드 id
        private Long widgetId;
        private String widgetCode;
        private String name;
        private String icon;
        private String category;
        private String config;
        private String title;
        private int x;
        private int y;
        private int width;
        private int height;
        @Setter private Object chartData;
        @Setter private Object cntData;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWidgetConfig {
        private String group;
        private List<String> elements;
        private String chartType;
    }

    // Request DTOs
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserWidgetsReq {
        private List<UserWidgetReq> widgets;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWidgetReq {
        private Long userDashboardWidgetId;  // 기존 위젯인 경우 (update), null이면 새 위젯 (insert)
        private Long widgetId;
        private String title;
        private String config;
        private Integer posX;
        private Integer posY;
        private Integer width;
        private Integer height;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WidgetPieChartData {
        private String metric;
        private Long deviceId;
        private String deviceName;
        private Float piePct;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WidgetLineChartData {
        private String metric;
        private Long deviceId;
        private String deviceName;
        private String timestamp;
        private Float linePct;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WidgetBarChartData {
        private String metric;
        private Long deviceId;
        private String deviceName;
        private Float barPct;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WidgetAlertCntData {
        private Integer criticalCnt = 0;
        private Integer majorCnt = 0;
        private Integer minorCnt = 0;
        private Integer warningCnt = 0;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WidgetDeviceCntData {
        private Integer networkCnt = 0;
        private Integer majorCnt = 0;
        private Integer minorCnt = 0;
        private Integer warningCnt = 0;
    }
}
