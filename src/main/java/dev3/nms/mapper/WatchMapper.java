package dev3.nms.mapper;

import dev3.nms.vo.watch.WatchGroupVO;
import dev3.nms.vo.watch.WatchGroupDeviceVO;
import dev3.nms.vo.watch.WatchGroupIfVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 관제 그룹 Mapper
 */
@Mapper
public interface WatchMapper {

    // ==================== 관제 그룹 ====================

    /**
     * 모든 관제 그룹 조회
     */
    List<WatchGroupVO> findAllGroups();

    /**
     * 관제 그룹 상세 조회
     */
    WatchGroupVO findGroupById(@Param("watchGroupId") Integer watchGroupId);

    /**
     * 관제 그룹 생성
     */
    void insertGroup(WatchGroupVO group);

    /**
     * 관제 그룹 수정
     */
    void updateGroup(WatchGroupVO group);

    /**
     * 관제 그룹 삭제
     */
    void deleteGroup(@Param("watchGroupId") Integer watchGroupId);

    /**
     * 관제 그룹 이동 (부모 변경)
     */
    void moveGroup(
            @Param("watchGroupId") Integer watchGroupId,
            @Param("parentGroupId") Integer parentGroupId,
            @Param("depth") Integer depth);

    /**
     * 관제 그룹 아이콘 설정
     */
    void updateGroupIcon(
            @Param("watchGroupId") Integer watchGroupId,
            @Param("iconName") String iconName);

    /**
     * 하위 그룹 개수 조회
     */
    int countDescendants(@Param("watchGroupId") Integer watchGroupId);

    /**
     * 하위 그룹 목록 조회
     */
    List<WatchGroupVO> findDescendants(@Param("watchGroupId") Integer watchGroupId);

    // ==================== 장비 그룹 연동 ====================

    /**
     * 이미 연동된 GROUP_ID 목록 조회
     */
    List<Integer> findAllLinkedGroupIds();

    /**
     * 특정 LINKED_GROUP_ID로 관제 그룹 조회
     */
    WatchGroupVO findByLinkedGroupId(@Param("linkedGroupId") Integer linkedGroupId);

    // ==================== 관제 그룹 장비 ====================

    /**
     * 그룹의 장비 목록 조회
     */
    List<WatchGroupDeviceVO> findDevicesByGroupId(@Param("watchGroupId") Integer watchGroupId);

    /**
     * 그룹에 장비 추가
     */
    void insertGroupDevice(WatchGroupDeviceVO device);

    /**
     * 그룹의 모든 장비 삭제
     */
    void deleteGroupDevices(@Param("watchGroupId") Integer watchGroupId);

    // ==================== 관제 그룹 인터페이스 ====================

    /**
     * 장비의 인터페이스 목록 조회
     */
    List<WatchGroupIfVO> findInterfacesByDevice(
            @Param("watchGroupId") Integer watchGroupId,
            @Param("deviceId") Integer deviceId);

    /**
     * 그룹에 인터페이스 추가
     */
    void insertGroupInterface(WatchGroupIfVO iface);

    /**
     * 그룹의 모든 인터페이스 삭제
     */
    void deleteGroupInterfaces(@Param("watchGroupId") Integer watchGroupId);

    /**
     * 특정 장비의 인터페이스 삭제
     */
    void deleteDeviceInterfaces(
            @Param("watchGroupId") Integer watchGroupId,
            @Param("deviceId") Integer deviceId);
}
