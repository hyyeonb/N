package dev3.nms.vo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class PageVO<T> {
    private List<T> content;       // 데이터 목록
    private int page;              // 현재 페이지 (1부터 시작)
    private int size;              // 페이지 크기
    private long totalElements;    // 전체 데이터 개수
    private int totalPages;        // 전체 페이지 수

    public static <T> PageVO<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageVO.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
