package dev3.nms.service;

import dev3.nms.mapper.LoginHistoryMapper;
import dev3.nms.vo.auth.LoginHistoryVO;
import dev3.nms.vo.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryMapper loginHistoryMapper;

    /**
     * 로그인 이력 페이지네이션 조회
     */
    public PageVO<LoginHistoryVO> getLoginHistory(int page, int size, String sort, String order,
                                                   String loginType, String startDate, String endDate,
                                                   String search, Long filterUserId) {
        Map<String, Object> params = new HashMap<>();
        params.put("offset", (page - 1) * size);
        params.put("size", size);
        params.put("sort", sort);
        params.put("order", order);
        params.put("loginType", loginType);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("search", search);
        params.put("filterUserId", filterUserId);

        List<LoginHistoryVO> content = loginHistoryMapper.findPaged(params);
        long totalElements = loginHistoryMapper.count(params);

        return PageVO.of(content, page, size, totalElements);
    }
}
