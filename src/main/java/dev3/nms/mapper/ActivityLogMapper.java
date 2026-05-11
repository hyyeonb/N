package dev3.nms.mapper;

import dev3.nms.vo.auth.ActivityLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityLogMapper {

    void insert(ActivityLogVO log);

    List<ActivityLogVO> findPaged(Map<String, Object> params);

    long count(Map<String, Object> params);

    List<ActivityLogVO> findByHistoryId(Map<String, Object> params);

    long countByHistoryId(@Param("historyId") Long historyId);

    List<ActivityLogVO> findByDeviceId(Map<String, Object> params);

    long countByDeviceId(Map<String, Object> params);

    Map<String, Object> countDailyDeviceChanges(String date);
}
