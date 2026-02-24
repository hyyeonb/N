package dev3.nms.controller;

import dev3.nms.service.UserTopoService;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.topo.UserTopoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-topo")
public class UserTopoController {

    private final UserTopoService userTopoService;

    /**
     * 사용자 메인 토폴로지 조회
     */
    @GetMapping("/{userId:\\d+}")
    public ResponseEntity<ResVO<UserTopoDto.ViewRes>> getUserTopo(@PathVariable Long userId) {
        UserTopoDto.ViewRes result = userTopoService.getUserTopo(userId, 0L);
        ResVO<UserTopoDto.ViewRes> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 사용자 메인 토폴로지 저장 (전체 교체)
     */
    @PutMapping("/{userId:\\d+}")
    public ResponseEntity<ResVO<UserTopoDto.ViewRes>> saveUserTopo(
            @PathVariable Long userId,
            @RequestBody UserTopoDto.SaveReq req
    ) {
        UserTopoDto.ViewRes result = userTopoService.saveUserTopo(userId, 0L, req);
        ResVO<UserTopoDto.ViewRes> response = new ResVO<>(200, "저장 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 사용자 그룹 하위 토폴로지 조회
     */
    @GetMapping("/{userId:\\d+}/group/{groupId:\\d+}")
    public ResponseEntity<ResVO<UserTopoDto.ViewRes>> getUserGroupTopo(
            @PathVariable Long userId,
            @PathVariable Long groupId
    ) {
        UserTopoDto.ViewRes result = userTopoService.getUserTopo(userId, groupId);
        ResVO<UserTopoDto.ViewRes> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 사용자 그룹 하위 토폴로지 저장 (전체 교체)
     */
    @PutMapping("/{userId:\\d+}/group/{groupId:\\d+}")
    public ResponseEntity<ResVO<UserTopoDto.ViewRes>> saveUserGroupTopo(
            @PathVariable Long userId,
            @PathVariable Long groupId,
            @RequestBody UserTopoDto.SaveReq req
    ) {
        UserTopoDto.ViewRes result = userTopoService.saveUserTopo(userId, groupId, req);
        ResVO<UserTopoDto.ViewRes> response = new ResVO<>(200, "저장 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 배경 이미지 저장
     */
    @PutMapping("/back-img")
    public ResponseEntity<ResVO<Boolean>> saveBackImg(
            @RequestBody UserTopoDto.SaveBackImgReq req
    ) {
        boolean result = userTopoService.saveBackImg(req);
        ResVO<Boolean> response = new ResVO<>(200, "배경 이미지 저장 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 노드 커스텀 아이콘 저장
     */
    @PutMapping("/node-img")
    public ResponseEntity<ResVO<Boolean>> saveNodeImg(
            @RequestBody UserTopoDto.SaveNodeImgReq req
    ) {
        boolean result = userTopoService.saveNodeImg(req);
        ResVO<Boolean> response = new ResVO<>(200, "노드 아이콘 저장 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
