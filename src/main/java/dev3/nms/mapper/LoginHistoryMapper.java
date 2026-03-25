package dev3.nms.mapper;

import dev3.nms.vo.auth.LoginHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginHistoryMapper {

    /**
     * 로그인 히스토리 저장
     */
    void insert(LoginHistoryVO history);

    /**
     * 페이지네이션 조회 (JOIN R_USER_T)
     */
    List<LoginHistoryVO> findPaged(Map<String, Object> params);

    /**
     * 총 건수
     */
    long count(Map<String, Object> params);

    /**
     * 로그아웃 시각 기록
     */
    void updateLogoutAt(@Param("historyId") Long historyId);

    /**
     * 사용자의 최근 로그인 이력 조회
     */
    List<LoginHistoryVO> findByUserId(@Param("USER_ID") Long userId, @Param("limit") int limit);

    /**
     * 마지막 활동이 N분 이상 지난 세션의 LOGOUT_AT 설정
     * @return 정리된 건수
     */
    int closeStaleSession(@Param("timeoutMinutes") int timeoutMinutes);
}
