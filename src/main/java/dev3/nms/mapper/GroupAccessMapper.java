package dev3.nms.mapper;

import dev3.nms.vo.auth.GroupAccessVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupAccessMapper {

    List<GroupAccessVO> findByUserId(@Param("USER_ID") Long userId);

    List<GroupAccessVO> findByUserIdAndGroupType(@Param("USER_ID") Long userId, @Param("GROUP_TYPE") String groupType);

    void insertGroupAccess(GroupAccessVO access);

    void insertGroupAccessBatch(@Param("list") List<GroupAccessVO> accessList);

    void deleteByUserId(@Param("USER_ID") Long userId);

    void deleteByUserIdAndGroupType(@Param("USER_ID") Long userId, @Param("GROUP_TYPE") String groupType);
}
