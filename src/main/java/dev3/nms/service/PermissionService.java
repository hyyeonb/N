package dev3.nms.service;

import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.GroupAccessMapper;
import dev3.nms.mapper.PageAccessMapper;
import dev3.nms.mapper.UserMapper;
import dev3.nms.mapper.WatchMapper;
import dev3.nms.vo.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PageAccessMapper pageAccessMapper;
    private final GroupAccessMapper groupAccessMapper;
    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper;
    private final WatchMapper watchMapper;

    // Admin 기준: system_admin 페이지에 CAN_EDIT=true
    private static final String ADMIN_PAGE_CODE = "system_admin";

    /**
     * 사용자 권한 전체 조회 (프론트 전달용)
     */
    public UserPermissionVO getUserPermissions(Long userId) {
        List<PageAccessVO> pageAccess = pageAccessMapper.findByUserId(userId);
        List<GroupAccessVO> groupAccess = groupAccessMapper.findByUserId(userId);

        boolean isAdmin = pageAccess.stream()
                .anyMatch(pa -> ADMIN_PAGE_CODE.equals(pa.getPAGE_CODE())
                        && Boolean.TRUE.equals(pa.getCAN_EDIT()));

        return UserPermissionVO.builder()
                .isAdmin(isAdmin)
                .allGroupView(true) // 기본값, AuthService에서 UserVO의 값으로 덮어씀
                .pageAccess(pageAccess)
                .groupAccess(groupAccess)
                .build();
    }

    /**
     * 특정 페이지 VIEW 권한 확인
     */
    public boolean canViewPage(Long userId, String pageCode) {
        PageAccessVO access = pageAccessMapper.findByUserIdAndPageCode(userId, pageCode);
        return access != null && Boolean.TRUE.equals(access.getCAN_VIEW());
    }

    /**
     * 특정 페이지 EDIT 권한 확인
     */
    public boolean canEditPage(Long userId, String pageCode) {
        PageAccessVO access = pageAccessMapper.findByUserIdAndPageCode(userId, pageCode);
        return access != null && Boolean.TRUE.equals(access.getCAN_EDIT());
    }

    /**
     * Admin 여부 확인
     */
    public boolean isAdmin(Long userId) {
        return canEditPage(userId, ADMIN_PAGE_CODE);
    }

    /**
     * 신규 사용자 기본 권한 초기화 (모든 페이지 VIEW=1, EDIT=0)
     */
    public void initializeDefaultPermissions(Long userId) {
        log.info("[PermissionService] 신규 사용자 기본 권한 초기화 - USER_ID: {}", userId);

        List<PageMasterVO> allPages = pageAccessMapper.findAllPages();
        List<PageAccessVO> accessList = allPages.stream()
                .map(page -> PageAccessVO.builder()
                        .USER_ID(userId)
                        .PAGE_CODE(page.getPAGE_CODE())
                        .CAN_VIEW(true)
                        .CAN_EDIT(false)
                        .build())
                .toList();

        if (!accessList.isEmpty()) {
            pageAccessMapper.insertPageAccessBatch(accessList);
        }

        log.info("[PermissionService] 기본 권한 초기화 완료 - {} 페이지", accessList.size());
    }

    // ─── 그룹 권한 ───

    /**
     * 특정 그룹 VIEW 권한 확인
     * allGroupView=true면 전체 허용, 아니면 개별 확인
     */
    public boolean canViewGroup(Long userId, String groupType, Long groupId) {
        if (isAdmin(userId)) return true;

        // allGroupView 체크
        UserVO user = userMapper.findById(userId).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getALL_GROUP_VIEW())) {
            return true;
        }

        List<GroupAccessVO> access = groupAccessMapper.findByUserIdAndGroupType(userId, groupType);
        return access.stream().anyMatch(ga ->
                ga.getGROUP_ID().equals(groupId) && Boolean.TRUE.equals(ga.getCAN_VIEW()));
    }

    /**
     * 특정 그룹 EDIT 권한 확인
     * 그룹 EDIT 권한이 있어도, 해당 그룹 VIEW도 있어야 함
     */
    public boolean canEditGroup(Long userId, String groupType, Long groupId) {
        if (isAdmin(userId)) return true;

        List<GroupAccessVO> access = groupAccessMapper.findByUserIdAndGroupType(userId, groupType);
        return access.stream().anyMatch(ga ->
                ga.getGROUP_ID().equals(groupId) && Boolean.TRUE.equals(ga.getCAN_EDIT()));
    }

    /**
     * 사용자가 접근 가능한 장비 ID 목록 반환 (ASSET 그룹 OR WATCH 그룹)
     * @return null = 전체 허용 (admin 또는 ALL_GROUP_VIEW)
     *         List = 접근 가능 장비 ID (빈 리스트 = 접근 불가)
     */
    public List<Long> getAccessibleDeviceIds(Long userId) {
        if (isAdmin(userId)) return null;

        UserVO user = userMapper.findById(userId).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getALL_GROUP_VIEW())) {
            return null;
        }

        Set<Long> deviceIds = new HashSet<>();

        // ASSET 그룹에 속한 장비
        List<Long> assetGroupIds = getAccessibleGroupIds(userId, "ASSET");
        if (assetGroupIds != null && !assetGroupIds.isEmpty()) {
            deviceIds.addAll(deviceMapper.findDeviceIdsByGroupIds(assetGroupIds));
        }

        // WATCH 그룹에 배정된 장비
        List<Long> watchGroupIds = getAccessibleGroupIds(userId, "WATCH");
        if (watchGroupIds != null && !watchGroupIds.isEmpty()) {
            deviceIds.addAll(watchMapper.findDeviceIdsByWatchGroupIds(watchGroupIds));
        }

        return new ArrayList<>(deviceIds);
    }

    /**
     * 사용자가 접근 가능한 WATCH 그룹 ID 목록 반환
     * @return null = 전체 허용, List = 접근 가능 관제 그룹 ID
     */
    public List<Long> getAccessibleWatchGroupIds(Long userId) {
        return getAccessibleGroupIds(userId, "WATCH");
    }

    /**
     * 사용자가 접근 가능한 ASSET 그룹 ID 목록 반환
     * @return null = 전체 허용, List = 접근 가능 자산 그룹 ID
     */
    public List<Long> getAccessibleAssetGroupIds(Long userId) {
        return getAccessibleGroupIds(userId, "ASSET");
    }

    /**
     * 사용자가 접근 가능한 그룹 ID 목록 반환
     * allGroupView=true면 null 반환 (전체 허용 의미)
     */
    public List<Long> getAccessibleGroupIds(Long userId, String groupType) {
        if (isAdmin(userId)) return null; // null = 전체 허용

        UserVO user = userMapper.findById(userId).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getALL_GROUP_VIEW())) {
            return null; // 전체 허용
        }

        List<GroupAccessVO> access = groupAccessMapper.findByUserIdAndGroupType(userId, groupType);
        return access.stream()
                .filter(ga -> Boolean.TRUE.equals(ga.getCAN_VIEW()))
                .map(GroupAccessVO::getGROUP_ID)
                .toList();
    }
}
