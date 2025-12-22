package dev3.nms.controller;

import dev3.nms.service.TopoService;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.mgmt.GroupVO;
import dev3.nms.vo.topo.TopoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/topology")
@RequiredArgsConstructor
public class TopoController {

    private final TopoService topoService;

    /**
     * 토포로지 조회
     */
    @GetMapping("/view")
    public ResponseEntity<ResVO<TopoDto.TopoViewDtoRes>> getTopologyView(
            @RequestParam Long id,
            @RequestParam String type
    ) {
        log.warn("id : " + id);
        log.warn("type : " + type);
        TopoDto.TopoViewDtoRes viewDtoRes = topoService.viewTopology(id, type);
        ResVO<TopoDto.TopoViewDtoRes> response = new ResVO<>(200, "조회 성공", viewDtoRes);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 토포로지 저장
     */
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
    @PutMapping("/view-img")
    public ResponseEntity<ResVO<Boolean>> saveTopologyImgView(
            @RequestBody TopoDto.TopoSaveImgDtoReq req
    ) {
        Boolean res = topoService.saveTopologyImg(req);
        ResVO<Boolean> response = new ResVO<>(200, "등록 성공", res);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
