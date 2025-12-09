package dev3.nms.vo.common;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ResVO<T> {
    private int code;
    private String message;
    private T data;
}
