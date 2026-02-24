package dev3.nms.service;

import dev3.nms.mapper.UserTopoMapper;
import dev3.nms.vo.topo.UserTopoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTopoService {

    private final UserTopoMapper userTopoMapper;

    /**
     * 사용자 토폴로지 조회 (메인: groupId=0, 그룹 하위: groupId=실제값)
     */
    public UserTopoDto.ViewRes getUserTopo(Long userId, Long groupId) {
        UserTopoDto.ViewRes view = userTopoMapper.findView(userId, groupId);
        if (view == null) {
            return null;
        }
        view.setNodes(userTopoMapper.findNodesByViewId(view.getViewId()));
        view.setLinks(userTopoMapper.findLinksByViewId(view.getViewId()));
        return view;
    }

    /**
     * 사용자 토폴로지 저장 (전체 교체)
     */
    @Transactional
    public UserTopoDto.ViewRes saveUserTopo(Long userId, Long groupId, UserTopoDto.SaveReq req) {
        Long viewId = userTopoMapper.findViewId(userId, groupId);

        if (viewId == null) {
            userTopoMapper.insertView(userId, groupId, req);
            viewId = req.getViewId();
        } else {
            userTopoMapper.updateView(viewId, req);
        }

        // 기존 노드/링크 삭제
        userTopoMapper.deleteNodesByViewId(viewId);
        userTopoMapper.deleteLinksByViewId(viewId);

        // 새 노드/링크 bulk INSERT
        if (req.getNodes() != null && !req.getNodes().isEmpty()) {
            userTopoMapper.insertNodes(viewId, req.getNodes());
        }
        if (req.getLinks() != null && !req.getLinks().isEmpty()) {
            List<UserTopoDto.SaveLink> dedupLinks = removeDuplicateLinks(req.getLinks());
            userTopoMapper.insertLinks(viewId, dedupLinks);
        }

        // 저장된 결과 반환
        return getUserTopo(userId, groupId);
    }

    /**
     * 배경 이미지 저장
     */
    public boolean saveBackImg(UserTopoDto.SaveBackImgReq req) {
        Long groupId = req.getGroupId() != null ? req.getGroupId() : 0L;
        Long viewId = userTopoMapper.findViewId(req.getUserId(), groupId);
        if (viewId == null) {
            return false;
        }
        userTopoMapper.updateBackIconData(viewId, req.getImgSrc());
        return true;
    }

    /**
     * 노드 커스텀 아이콘 저장
     */
    public boolean saveNodeImg(UserTopoDto.SaveNodeImgReq req) {
        Long groupId = req.getGroupId() != null ? req.getGroupId() : 0L;
        Long viewId = userTopoMapper.findViewId(req.getUserId(), groupId);
        if (viewId == null) {
            return false;
        }
        userTopoMapper.updateNodeIconData(viewId, req.getNodeKey(), req.getImgSrc());
        return true;
    }

    /**
     * 같은 장비 간 중복 링크 제거 (A→B, B→A 동일 취급)
     */
    private List<UserTopoDto.SaveLink> removeDuplicateLinks(List<UserTopoDto.SaveLink> links) {
        Set<String> seen = new HashSet<>();
        return links.stream().filter(l -> {
            String key1 = l.getSource() + "-" + l.getTarget();
            String key2 = l.getTarget() + "-" + l.getSource();
            if (seen.contains(key1) || seen.contains(key2)) {
                return false;
            }
            seen.add(key1);
            return true;
        }).collect(Collectors.toList());
    }
}
