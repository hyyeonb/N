package dev3.nms.controller;

import dev3.nms.service.NoticeService;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import dev3.nms.vo.notice.NoticeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 목록 조회 (페이지네이션 + 검색)
     */
    @GetMapping("/posts")
    public ResponseEntity<ResVO<PageVO<NoticeDto.PostRes>>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        PageVO<NoticeDto.PostRes> result = noticeService.getPostList(page, size, search);
        ResVO<PageVO<NoticeDto.PostRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 상세 조회 (조회수 증가)
     */
    @GetMapping("/posts/{noticeId}")
    public ResponseEntity<ResVO<NoticeDto.PostDetailRes>> getPost(@PathVariable Long noticeId) {
        NoticeDto.PostDetailRes post = noticeService.getPost(noticeId);
        if (post == null) {
            ResVO<NoticeDto.PostDetailRes> response = new ResVO<>(404, "공지사항을 찾을 수 없습니다", null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ResVO<NoticeDto.PostDetailRes> response = new ResVO<>(200, "조회 성공", post);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 등록 (긴급공지면 WebSocket 브로드캐스트)
     */
    @PostMapping("/posts")
    public ResponseEntity<ResVO<NoticeDto.PostDetailRes>> createPost(
            @RequestBody NoticeDto.PostCreateReq req
    ) {
        NoticeDto.PostDetailRes result = noticeService.createPost(req);
        ResVO<NoticeDto.PostDetailRes> response = new ResVO<>(200, "등록 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 수정
     */
    @PutMapping("/posts/{noticeId}")
    public ResponseEntity<ResVO<NoticeDto.PostDetailRes>> updatePost(
            @PathVariable Long noticeId,
            @RequestBody NoticeDto.PostUpdateReq req
    ) {
        NoticeDto.PostDetailRes result = noticeService.updatePost(noticeId, req);
        ResVO<NoticeDto.PostDetailRes> response = new ResVO<>(200, "수정 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 삭제 (소프트)
     */
    @DeleteMapping("/posts/{noticeId}")
    public ResponseEntity<ResVO<Void>> deletePost(@PathVariable Long noticeId) {
        noticeService.deletePost(noticeId);
        ResVO<Void> response = new ResVO<>(200, "삭제 성공", null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
