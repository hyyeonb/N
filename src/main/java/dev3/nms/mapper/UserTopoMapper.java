package dev3.nms.mapper;

import dev3.nms.vo.topo.UserTopoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTopoMapper {

    // 뷰
    Long findViewId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    UserTopoDto.ViewRes findView(@Param("userId") Long userId, @Param("groupId") Long groupId);

    int insertView(@Param("userId") Long userId, @Param("groupId") Long groupId, @Param("req") UserTopoDto.SaveReq req);

    int updateView(@Param("viewId") Long viewId, @Param("req") UserTopoDto.SaveReq req);

    // 노드
    List<UserTopoDto.NodeRes> findNodesByViewId(@Param("viewId") Long viewId);

    int insertNodes(@Param("viewId") Long viewId, @Param("nodes") List<UserTopoDto.SaveNode> nodes);

    int deleteNodesByViewId(@Param("viewId") Long viewId);

    // 링크
    List<UserTopoDto.LinkRes> findLinksByViewId(@Param("viewId") Long viewId);

    int insertLinks(@Param("viewId") Long viewId, @Param("links") List<UserTopoDto.SaveLink> links);

    int deleteLinksByViewId(@Param("viewId") Long viewId);

    // 배경 이미지
    int updateBackIconData(@Param("viewId") Long viewId, @Param("imgSrc") String imgSrc);

    // 노드 커스텀 아이콘
    int updateNodeIconData(@Param("viewId") Long viewId, @Param("nodeKey") String nodeKey, @Param("imgSrc") String imgSrc);
}
