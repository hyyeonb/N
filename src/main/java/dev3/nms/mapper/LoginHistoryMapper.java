package dev3.nms.mapper;

import dev3.nms.vo.auth.LoginHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoginHistoryMapper {

    /**
     * 로그인 히스토리 저장
     */
    void insert(LoginHistoryVO history);


}
