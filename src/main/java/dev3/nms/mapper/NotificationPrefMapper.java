package dev3.nms.mapper;

import dev3.nms.vo.auth.NotificationPrefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface NotificationPrefMapper {

    Optional<NotificationPrefVO> findByUserId(@Param("USER_ID") Long userId);

    void insert(NotificationPrefVO pref);

    void update(NotificationPrefVO pref);
}
