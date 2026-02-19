package dev3.nms.service;

import dev3.nms.mapper.GroupMapper;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.mgmt.TempDeviceVO; // TempDeviceVO import 추가
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupMapper groupMapper;
    private final TempDeviceService tempDeviceService; // TempDeviceService 의존성 주입

    // TODO: 현재 로그인한 사용자 ID를 가져오는 로직 추가 필요 (세션 또는 Spring Security 연동)
    private Integer getCurrentUserId() {
        return 1; // Placeholder for logged-in user ID
    }

    @Transactional
    public GroupVO createGroup(GroupVO group) {
        if (group.getPARENT_GROUP_ID() != null) {
            GroupVO parentGroup = groupMapper.findGroupById(group.getPARENT_GROUP_ID())
                    .orElseThrow(() -> new IllegalArgumentException("Parent group not found with ID: " + group.getPARENT_GROUP_ID()));
            group.setDEPTH(parentGroup.getDEPTH() + 1);
        } else {
            group.setDEPTH(0); // Root group
        }
        group.setCREATE_USER_ID(getCurrentUserId());
        groupMapper.insertGroup(group);
        return group;
    }

    @Transactional(readOnly = true)
    public List<GroupVO> getGroupHierarchy() {
        List<GroupVO> flatList = groupMapper.findAllGroups();
        Map<Integer, GroupVO> groupMap = flatList.stream()
                .collect(Collectors.toMap(GroupVO::getGROUP_ID, group -> group));

        List<GroupVO> hierarchy = new ArrayList<>();
        for (GroupVO group : flatList) {
            if (group.getPARENT_GROUP_ID() == null || group.getPARENT_GROUP_ID() == 0) { // 최상위 그룹
                hierarchy.add(group);
            } else {
                GroupVO parent = groupMap.get(group.getPARENT_GROUP_ID());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    List<GroupVO> children = parent.getChildren();
                    children.add(group);
                }
            }
        }

        // 최상위 그룹을 이름으로 정렬
        hierarchy.sort(Comparator.comparing(GroupVO::getGROUP_NAME));

        // 미등록 장비 노드 추가
        List<TempDeviceVO> allTempDevices = tempDeviceService.findAllTempDevices();
        if (!allTempDevices.isEmpty()) {
            GroupVO unassignedGroup = GroupVO.builder()
                    .GROUP_ID(0)
                    .GROUP_NAME("미등록 장비")
                    .DEPTH(0)
                    .children(new ArrayList<>())
                    .build();
            hierarchy.add(unassignedGroup);
        }
        
        return hierarchy;
    }

    @Transactional(readOnly = true)
    public GroupVO getGroupById(Integer groupId) {
        return groupMapper.findGroupById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found or already deleted with ID: " + groupId));
    }

    @Transactional
    public GroupVO updateGroup(Integer groupId, GroupVO groupUpdates) {
        GroupVO existingGroup = getGroupById(groupId);
        
        // Update fields if provided
        if (groupUpdates.getGROUP_NAME() != null && !groupUpdates.getGROUP_NAME().isEmpty()) {
            existingGroup.setGROUP_NAME(groupUpdates.getGROUP_NAME());
        }

        // 주소와 연락처 업데이트 추가
        if (groupUpdates.getADDRESS() != null) {
            existingGroup.setADDRESS(groupUpdates.getADDRESS());
        }
        if (groupUpdates.getPHONE() != null) {
            existingGroup.setPHONE(groupUpdates.getPHONE());
        }
        
        // Handle parent change (recalculate depth)
        if (groupUpdates.getPARENT_GROUP_ID() != null && !groupUpdates.getPARENT_GROUP_ID().equals(existingGroup.getPARENT_GROUP_ID())) {
            GroupVO newParent = groupMapper.findGroupById(groupUpdates.getPARENT_GROUP_ID())
                                        .orElseThrow(() -> new IllegalArgumentException("New parent group not found with ID: " + groupUpdates.getPARENT_GROUP_ID()));
            existingGroup.setPARENT_GROUP_ID(groupUpdates.getPARENT_GROUP_ID());
            existingGroup.setDEPTH(newParent.getDEPTH() + 1);
        } else if (groupUpdates.getPARENT_GROUP_ID() == null && existingGroup.getPARENT_GROUP_ID() != null) {
            existingGroup.setPARENT_GROUP_ID(null);
            existingGroup.setDEPTH(0);
        }

        existingGroup.setMODIFY_USER_ID(getCurrentUserId());
        groupMapper.updateGroup(existingGroup);
        return existingGroup;
    }

    /**
     * 그룹 삭제 (하위 그룹도 함께 삭제)
     */
    @Transactional
    public void deleteGroup(Integer groupId) {
        // 그룹 존재 확인
        getGroupById(groupId);

        GroupVO groupForDelete = GroupVO.builder()
                                        .GROUP_ID(groupId)
                                        .DELETE_USER_ID(getCurrentUserId())
                                        .build();

        // 자식 그룹도 함께 재귀적으로 삭제
        int updatedRows = groupMapper.deleteGroupWithChildren(groupForDelete);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Group not found or already deleted with ID: " + groupId);
        }
    }

    /**
     * 그룹의 전체 하위 그룹 개수 조회 (WITH RECURSIVE CTE - 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public int countAllDescendants(Integer groupId) {
        return groupMapper.countDescendants(groupId);
    }

    /**
     * 그룹의 전체 하위 그룹 목록 조회 (WITH RECURSIVE CTE - 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public List<GroupVO> getAllDescendants(Integer groupId) {
        return groupMapper.findDescendants(groupId);
    }

    /**
     * 그룹 이동 (하위 그룹도 함께 이동, DEPTH 재계산)
     */
    @Transactional
    public GroupVO moveGroup(Integer groupId, Integer newParentId) {
        GroupVO group = getGroupById(groupId);

        // 자기 자신을 부모로 설정하려는 경우
        if (groupId.equals(newParentId)) {
            throw new IllegalArgumentException("그룹을 자기 자신의 하위로 이동할 수 없습니다.");
        }

        // 자신의 하위 그룹을 부모로 설정하려는 경우 체크
        if (newParentId != null && isDescendant(groupId, newParentId)) {
            throw new IllegalArgumentException("그룹을 자신의 하위 그룹으로 이동할 수 없습니다.");
        }

        int newDepth;
        // 새로운 부모 설정 및 DEPTH 계산
        if (newParentId != null) {
            GroupVO newParent = groupMapper.findGroupById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("새 부모 그룹을 찾을 수 없습니다: " + newParentId));
            group.setPARENT_GROUP_ID(newParentId);
            newDepth = newParent.getDEPTH() + 1;
        } else {
            // 최상위 그룹으로 이동
            group.setPARENT_GROUP_ID(null);
            newDepth = 0;
        }

        group.setDEPTH(newDepth);
        group.setMODIFY_USER_ID(getCurrentUserId());
        groupMapper.updateGroup(group);

        // 하위 그룹들의 DEPTH도 재계산
        groupMapper.updateChildrenDepth(groupId, newDepth);

        return group;
    }

    /**
     * 그룹 아이콘 설정
     */
    @Transactional
    public GroupVO updateGroupIcon(Integer groupId, String iconName) {
        GroupVO existingGroup = getGroupById(groupId);
        existingGroup.setICON_NAME(iconName);
        existingGroup.setMODIFY_USER_ID(getCurrentUserId());
        groupMapper.updateGroupIcon(groupId, iconName);
        return existingGroup;
    }

    /**
     * 특정 그룹이 다른 그룹의 하위인지 확인
     */
    private boolean isDescendant(Integer ancestorId, Integer descendantId) {
        GroupVO current = groupMapper.findGroupById(descendantId).orElse(null);
        while (current != null && current.getPARENT_GROUP_ID() != null) {
            if (current.getPARENT_GROUP_ID().equals(ancestorId)) {
                return true;
            }
            current = groupMapper.findGroupById(current.getPARENT_GROUP_ID()).orElse(null);
        }
        return false;
    }
}
