package dev3.nms.vo.mgmt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * P_CPU_MEM_T 테이블 VO
 * CPU/메모리 사용률 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CpuMemVO {
    private Long ID;
    private Integer DEVICE_ID;
    private LocalDateTime COLLECTED_AT;
    private Integer CORE_INDEX;      // 코어 인덱스 (NULL=집계값)

    // CPU
    private BigDecimal CPU_USAGE;    // CPU 사용률 %
    private Integer CPU_IDLE;        // CPU 유휴율
    private Integer CPU_USER;        // CPU 사용자
    private Integer CPU_SYSTEM;      // CPU 시스템

    // Memory
    private Long MEM_TOTAL;          // 전체 메모리 (bytes)
    private Long MEM_FREE;           // 여유 메모리
    private Long MEM_AVAIL;          // 가용 메모리
    private Long MEM_BUFFER;         // 버퍼 메모리
    private Long MEM_CACHE;          // 캐시 메모리
    private Long MEM_USED;           // 사용 메모리
    private BigDecimal MEM_USAGE;    // 메모리 사용률 %

    /**
     * CPU 사용률 반환 (Double 타입)
     */
    public Double getCpuUsageValue() {
        return CPU_USAGE != null ? CPU_USAGE.doubleValue() : null;
    }

    /**
     * 메모리 사용률 반환 (Double 타입)
     */
    public Double getMemUsageValue() {
        return MEM_USAGE != null ? MEM_USAGE.doubleValue() : null;
    }

    /**
     * 메모리 총량 (GB 단위)
     */
    public Double getMemTotalGB() {
        if (MEM_TOTAL == null) return null;
        return MEM_TOTAL / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * 메모리 사용량 (GB 단위)
     */
    public Double getMemUsedGB() {
        if (MEM_USED == null) return null;
        return MEM_USED / (1024.0 * 1024.0 * 1024.0);
    }
}
