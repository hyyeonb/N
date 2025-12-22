package dev3.nms.mapper;

import dev3.nms.vo.topo.TopoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopoMapper {
    List<TopoDto.TopoViewNode> getViewTopologyNodes(Long id);

    List<TopoDto.TopoViewLink> getViewTopologyLinks(Long id);

    Long isTopoGroupId(Long id);

    Long findViewIdByGroupId(Long id);

    Long isTopoDeviceId(Long id);

    Long findViewIdByDeviceId(Long id);

    boolean isTopoViewId(
            @Param("id") Long id,           // groupId 또는 deviceId
            @Param("type") String type      // "group" | "device"
    );

    int insertTopoView(TopoDto.TopoSaveDtoReq req);

    int insertTopoNodes(Long viewId, List<TopoDto.TopoSaveNode> nodes);

    void insertTopoLinks(Long viewId, List<TopoDto.TopoSaveLink> links);

    Long selectViewIdByScope(Long id, String type);

    void deleteTopoLinksByViewId(Long viewId);

    void deleteTopoNodesByViewId(Long viewId);

    String getViewTopologyBackIconData(Long viewId);

    int saveTopologyBackImg(TopoDto.TopoSaveBackImgDtoReq req);

    int saveTopologyGroupImg(TopoDto.TopoSaveImgDtoReq req);

    int saveTopologyModelImg(TopoDto.TopoSaveImgDtoReq req);

    Long findModelIdByDeviceId(TopoDto.TopoSaveImgDtoReq req);
}
