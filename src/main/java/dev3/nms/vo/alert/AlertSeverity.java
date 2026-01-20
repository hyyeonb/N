package dev3.nms.vo.alert;

import lombok.Getter;

@Getter
public enum AlertSeverity {
    CRITICAL(1, "심각"),
    MAJOR(2, "주요"),
    MINOR(3, "경미"),
    WARNING(4, "주의"),
    INFO(5, "정보");

    private final int level;
    private final String label;

    AlertSeverity(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public static AlertSeverity fromValue(double value) {
        if (value >= 95) return CRITICAL;
        if (value >= 90) return MAJOR;
        if (value >= 80) return MINOR;
        if (value >= 70) return WARNING;
        return INFO;
    }
}
