package dev3.nms.vo.alert;

public enum AlertCategory {
    CONNECTION,   // PING, SNMP 연결 장애
    PERFORMANCE,  // CPU, MEM, Traffic 임계치
    PORT,         // 포트 상태 변경
    SYSTEM        // 장비 재부팅, 설정 변경 등
}
