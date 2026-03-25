package dev3.nms.controller;

import dev3.nms.config.AuditLog;
import dev3.nms.config.RequireEditPermission;
import dev3.nms.service.PermissionService;
import dev3.nms.service.TopoService;
import dev3.nms.util.SessionUtil;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.topo.TopoDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/topology")
@RequiredArgsConstructor
public class TopoController {

    private final TopoService topoService;
    private final PermissionService permissionService;

    /**
     * 토포로지 조회
     */
    @GetMapping("/view")
    public ResponseEntity<ResVO<TopoDto.TopoViewDtoRes>> getTopologyView(
            @RequestParam Long id,
            @RequestParam String type,
            HttpSession session
    ) {
        log.warn("id : " + id);
        log.warn("type : " + type);
        TopoDto.TopoViewDtoRes viewDtoRes = topoService.viewTopology(id, type);

        // 장비 권한 필터링: device 타입 노드 중 접근 불가 장비 제거
        List<Long> accessibleDeviceIds = getAccessibleDeviceIds(session);
        if (accessibleDeviceIds != null && viewDtoRes != null && viewDtoRes.getNodes() != null) {
            List<TopoDto.TopoViewNode> filteredNodes = viewDtoRes.getNodes().stream()
                    .filter(node -> {
                        // group 노드는 통과
                        if (!"device".equals(node.getNodeType())) return true;
                        // device 노드는 접근 가능 목록 확인
                        return node.getDeviceId() != null && accessibleDeviceIds.contains(node.getDeviceId());
                    })
                    .collect(Collectors.toList());
            viewDtoRes.setNodes(filteredNodes);
        }

        ResVO<TopoDto.TopoViewDtoRes> response = new ResVO<>(200, "조회 성공", viewDtoRes);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 토포로지 저장
     */
    @AuditLog(actionType = "UPDATE", targetType = "TOPOLOGY", pageCode = "topology")
    @RequireEditPermission("topology")
    @PutMapping("/view")
    public ResponseEntity<ResVO<Boolean>> saveTopologyView(
            @RequestBody TopoDto.TopoSaveDtoReq req
    ) {
        Boolean res = topoService.saveTopology(req);
        ResVO<Boolean> response = new ResVO<>(200, "등록 성공", res);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 토포로지 백그라운드 배경 저장
     */
    @AuditLog(actionType = "UPDATE", targetType = "TOPOLOGY", pageCode = "topology")
    @RequireEditPermission("topology")
    @PutMapping("/view-back-img")
    public ResponseEntity<ResVO<Boolean>> saveTopologyBackImgView(
            @RequestBody TopoDto.TopoSaveBackImgDtoReq req
    ) {
        Boolean res = topoService.saveTopologyBackImg(req);
        ResVO<Boolean> response = new ResVO<>(200, "등록 성공", res);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 토포로지 노드 이미지 변경
     */
    @AuditLog(actionType = "UPDATE", targetType = "TOPOLOGY", pageCode = "topology")
    @RequireEditPermission("topology")
    @PutMapping("/view-img")
    public ResponseEntity<ResVO<Boolean>> saveTopologyImgView(
            @RequestBody TopoDto.TopoSaveImgDtoReq req
    ) {
        Boolean res = topoService.saveTopologyImg(req);
        ResVO<Boolean> response = new ResVO<>(200, "등록 성공", res);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ========== 헬퍼 ==========

    private List<Long> getAccessibleDeviceIds(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) return List.of();
        return permissionService.getAccessibleDeviceIds(userId);
    }
}
