package dev3.nms.service;

import dev3.nms.config.SessionAuthFilter;
import dev3.nms.mapper.GroupAccessMapper;
import dev3.nms.mapper.PageAccessMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.vo.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final PageAccessMapper pageAccessMapper;
    private final GroupAccessMapper groupAccessMapper;
    private final PermissionService permissionService;
    private final SessionAuthFilter sessionAuthFilter;

    /**
     * 전체 사용자 목록 조회
     */
    public List<UserVO> getAllUsers() {
        List<UserVO> users = userMapper.findAll();
        users.forEach(u -> u.setPASSWORD(null)); // 비밀번호 제외
        return users;
    }

    /**
     * 특정 사용자 상세 + 권한 조회
     */
    public Map<String, Object> getUserDetail(Long userId) {
        UserVO user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        user.setPASSWORD(null);

        UserPermissionVO permissions = permissionService.getUserPermissions(userId);
        permissions.setAllGroupView(Boolean.TRUE.equals(user.getALL_GROUP_VIEW()));

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", permissions);
        return result;
    }

    /**
     * 페이지 권한 일괄 수정 (DELETE + INSERT)
     */
    @Transactional
    public void updatePageAccess(Long userId, List<PageAccessVO> accessList, Long modifyUserId) {
        log.info("[AdminService] 페이지 권한 수정 - USER_ID: {}, 건수: {}", userId, accessList.size());

        pageAccessMapper.deleteByUserId(userId);

        accessList.forEach(a -> {
            a.setUSER_ID(userId);
            a.setMODIFY_USER_ID(modifyUserId);
        });

        if (!accessList.isEmpty()) {
            pageAccessMapper.insertPageAccessBatch(accessList);
        }
    }

    /**
     * 그룹 권한 수정 (DELETE + INSERT)
     */
    @Transactional
    public void updateGroupAccess(Long userId, List<GroupAccessVO> accessList, Long modifyUserId) {
        log.info("[AdminService] 그룹 권한 수정 - USER_ID: {}, 건수: {}", userId, accessList.size());

        groupAccessMapper.deleteByUserId(userId);

        accessList.forEach(a -> {
            a.setUSER_ID(userId);
            a.setMODIFY_USER_ID(modifyUserId);
        });

        if (!accessList.isEmpty()) {
            groupAccessMapper.insertGroupAccessBatch(accessList);
        }
    }

    /**
     * 계정 상태 변경
     */
    public void updateUserStatus(Long userId, String status) {
        log.info("[AdminService] 사용자 상태 변경 - USER_ID: {}, STATUS: {}", userId, status);
        userMapper.updateStatus(userId, status);
        sessionAuthFilter.evictUser(userId); // 캐시 즉시 무효화
    }

    /**
     * 전체 그룹 조회 권한 토글
     */
    public void updateAllGroupView(Long userId, Boolean allGroupView) {
        log.info("[AdminService] 전체 그룹 조회 변경 - USER_ID: {}, ALL_GROUP_VIEW: {}", userId, allGroupView);
        userMapper.updateAllGroupView(userId, allGroupView);
    }

    /**
     * 사용자 확인 처리
     */
    public void reviewUser(Long userId) {
        log.info("[AdminService] 사용자 확인 처리 - USER_ID: {}", userId);
        userMapper.updateReviewedAt(userId);
    }

    /**
     * 다른 사용자로부터 권한 복사
     */
    @Transactional
    public void copyPermissions(Long targetUserId, Long sourceUserId, Long modifyUserId) {
        log.info("[AdminService] 권한 복사 - SOURCE: {}, TARGET: {}", sourceUserId, targetUserId);

        // 페이지 권한 복사
        List<PageAccessVO> sourcePages = pageAccessMapper.findByUserId(sourceUserId);
        pageAccessMapper.deleteByUserId(targetUserId);

        sourcePages.forEach(a -> {
            a.setUSER_ID(targetUserId);
            a.setMODIFY_USER_ID(modifyUserId);
        });

        if (!sourcePages.isEmpty()) {
            pageAccessMapper.insertPageAccessBatch(sourcePages);
        }

        // 그룹 권한 복사
        List<GroupAccessVO> sourceGroups = groupAccessMapper.findByUserId(sourceUserId);
        groupAccessMapper.deleteByUserId(targetUserId);

        sourceGroups.forEach(a -> {
            a.setUSER_ID(targetUserId);
            a.setMODIFY_USER_ID(modifyUserId);
        });

        if (!sourceGroups.isEmpty()) {
            groupAccessMapper.insertGroupAccessBatch(sourceGroups);
        }

        // ALL_GROUP_VIEW 복사
        UserVO sourceUser = userMapper.findById(sourceUserId).orElse(null);
        if (sourceUser != null) {
            userMapper.updateAllGroupView(targetUserId, sourceUser.getALL_GROUP_VIEW());
        }
    }

    /**
     * 전체 페이지 마스터 목록 조회
     */
    public List<PageMasterVO> getAllPages() {
        return pageAccessMapper.findAllPages();
    }
}
