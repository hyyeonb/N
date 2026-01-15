package dev3.nms.mapper;

import dev3.nms.vo.mgmt.GroupVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface GroupMapper {
    int insertGroup(GroupVO group);

    Optional<GroupVO> findGroupById(Integer groupId);

    List<GroupVO> findAllGroups();

    int updateGroup(GroupVO group);

    int updateGroupIcon(Integer groupId, String iconName);


    /**
     * 특정 그룹의 직접 자식 그룹 목록 조회
     */
    List<GroupVO> findChildrenByParentId(Integer parentId);

    /**
     * 그룹 및 모든 하위 그룹 일괄 소프트 삭제 (재귀적)
     */
    int deleteGroupWithChildren(GroupVO group);

    /**
     * 특정 그룹의 모든 하위 그룹 DEPTH 일괄 업데이트
     */
    int updateChildrenDepth(Integer groupId, Integer baseDepth);

    /**
     * 특정 그룹 및 모든 하위 그룹 ID 목록 조회 (재귀)
     */
    List<Integer> findGroupIdWithDescendants(Integer groupId);
}
