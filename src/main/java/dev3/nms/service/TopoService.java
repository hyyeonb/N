package dev3.nms.service;

import com.sun.jna.platform.mac.SystemB;
import dev3.nms.mapper.DeviceMapper;
import dev3.nms.mapper.TopoMapper;
import dev3.nms.vo.topo.TopoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopoService {

    private final TopoMapper topoMapper;
    /**
     * 토폴로지 조회
     * */
    public TopoDto.TopoViewDtoRes viewTopology(
            Long id,
            String type
    ) {
        try {
            Long viewId = 0L;
            List<TopoDto.TopoViewNode> nodes = new ArrayList<>();
            List<TopoDto.TopoViewLink> links = new ArrayList<>();
            String backIconData = "";
            // viewId 조회
            if ("group".equals(type)) {
                // 그룹 확인
                if (topoMapper.isTopoGroupId(id) != null) {
                    // 그룹 viewId 조회
                    viewId = topoMapper.findViewIdByGroupId(id);
                }
            }
            else if ("device".equals(type)) {
                // 장비 확인
                if (topoMapper.isTopoDeviceId(id) != null) {
                    // 그룹 viewId 조회
                    viewId = topoMapper.findViewIdByDeviceId(id);
                }
            }

            // viewId 없으면 패스
            if (viewId == 0) throw new RuntimeException();

            backIconData = topoMapper.getViewTopologyBackIconData(viewId);

            nodes = topoMapper.getViewTopologyNodes(viewId);
            links = topoMapper.getViewTopologyLinks(viewId);

            return TopoDto.TopoViewDtoRes.builder()
                    .backIconData(backIconData)
                    .nodes(nodes)
                    .links(links)
                    .build();

        } catch (RuntimeException e) {
            log.warn("viewId 조회 안됌");
            return TopoDto.TopoViewDtoRes.builder().build();
        } catch (Exception e) {
            log.warn("topology 조회 실패 - Id: {}, Type: {}", id, type);
            return TopoDto.TopoViewDtoRes.builder().build();
        }
    }
    /**
     * 토폴로지 저장
     * */
    @Transactional
    public Boolean saveTopology(TopoDto.TopoSaveDtoReq req) {
        try {
            Long viewId = 0L;
            // 토폴로지 중복확인
            boolean exists = topoMapper.isTopoViewId(req.getId(), req.getType());
            if (!exists) {
                // view 저장
                topoMapper.insertTopoView(req);

                // node 저장
                viewId = req.getViewId();
                if (viewId == null) {
                    throw new IllegalStateException("VIEW_ID 생성 실패");
                }

            }else {
                // 토폴로지 중복 (업데이트 작업)
                viewId = topoMapper.selectViewIdByScope(req.getId(), req.getType());

                // 기존 노드/링크 전부 삭제
                topoMapper.deleteTopoLinksByViewId(viewId);
                topoMapper.deleteTopoNodesByViewId(viewId);
            }

            // TODO 2) NODE 저장 (12월 11일 작업)
            if (req.getNodes() != null && !req.getNodes().isEmpty()) {
                topoMapper.insertTopoNodes(viewId, req.getNodes());
            }

            // TODO 3) LINK 저장 (12월 11일 작업)
            if (req.getLinks() != null && !req.getLinks().isEmpty()) {
                topoMapper.insertTopoLinks(viewId, req.getLinks());
            }

            return true;
        } catch (RuntimeException e) {
            log.warn("VIEW_ID 생성 실패 - {}", req.toString(), e);
            return false;
        } catch (Exception e) {
            log.warn("topology 저장 실패 - {}", req.toString(), e);
            return false;
        }
    }

    public Boolean saveTopologyBackImg(TopoDto.TopoSaveBackImgDtoReq req) {
        try {
            topoMapper.saveTopologyBackImg(req);
            return true;
        } catch (Exception e) {
            log.warn("topology 배경 이미지 저장 실패 - {}", req.toString(), e);
            return false;
        }
    }

    public Boolean saveTopologyImg(TopoDto.TopoSaveImgDtoReq req) {
        try {
            if ("group".equals(req.getType())) {
                topoMapper.saveTopologyGroupImg(req);
            }else if ("device".equals(req.getType())) {
                Long modelId = topoMapper.findModelIdByDeviceId(req);
                req.setModelId(modelId);
                topoMapper.saveTopologyModelImg(req);
            }else throw new RuntimeException("비정상 type");

            return true;
        } catch (Exception e) {
            log.warn("topology 배경 이미지 저장 실패 - {}", req.toString(), e);
            return false;
        }
    }
}
