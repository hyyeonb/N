package dev3.nms.controller;

import dev3.nms.service.BoardFileService;
import dev3.nms.vo.board.BoardFileDto;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.common.ResVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardFileController {

    private final BoardFileService boardFileService;

    /**
     * 목록 조회 (페이지네이션 + 검색 + 카테고리 필터)
     */
    @GetMapping("/posts")
    public ResponseEntity<ResVO<PageVO<BoardFileDto.PostRes>>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "POST_ID") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam Long userId
    ) {
        PageVO<BoardFileDto.PostRes> result = boardFileService.getPostList(page, size, sort, order, search, category, userId);
        ResVO<PageVO<BoardFileDto.PostRes>> response = new ResVO<>(200, "조회 성공", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 상세 조회 (조회수 증가 + 첨부파일 포함, 비공개글은 작성자만)
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ResVO<BoardFileDto.PostDetailRes>> getPost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        BoardFileDto.PostDetailRes post = boardFileService.getPost(postId, userId);
        if (post == null) {
            ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(404, "게시글을 찾을 수 없습니다", null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(200, "조회 성공", post);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 등록 (multipart: post JSON + files)
     */
    @PostMapping("/posts")
    public ResponseEntity<ResVO<BoardFileDto.PostDetailRes>> createPost(
            @RequestPart("post") BoardFileDto.PostCreateReq post,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam Long userId
    ) {
        try {
            BoardFileDto.PostDetailRes result = boardFileService.createPost(post, files, userId);
            ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(200, "등록 성공", result);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("게시글 등록 실패 - {}", e.getMessage());
            ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(500, "등록 실패: " + e.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 수정 (multipart: post JSON + files + 삭제할 첨부ID)
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ResVO<BoardFileDto.PostDetailRes>> updatePost(
            @PathVariable Long postId,
            @RequestPart("post") BoardFileDto.PostUpdateReq post,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            BoardFileDto.PostDetailRes result = boardFileService.updatePost(postId, post, files);
            ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(200, "수정 성공", result);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("게시글 수정 실패 - {}", e.getMessage());
            ResVO<BoardFileDto.PostDetailRes> response = new ResVO<>(500, "수정 실패: " + e.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 삭제 (소프트 삭제)
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ResVO<Void>> deletePost(
            @PathVariable Long postId
    ) {
        boardFileService.deletePost(postId);
        ResVO<Void> response = new ResVO<>(200, "삭제 성공", null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 첨부파일 다운로드
     */
    @GetMapping("/attach/{attachId}/download")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long attachId
    ) {
        try {
            BoardFileDto.AttachFileInfo fileInfo = boardFileService.getDownloadFileInfo(attachId);
            if (fileInfo == null) {
                ResVO<Void> response = new ResVO<>(404, "첨부파일을 찾을 수 없습니다", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Resource resource = boardFileService.loadFileAsResource(fileInfo);
            String encodedFileName = URLEncoder.encode(fileInfo.getOrgFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            log.error("파일 다운로드 실패 - attachId: {}, error: {}", attachId, e.getMessage());
            ResVO<Void> response = new ResVO<>(500, "다운로드 실패: " + e.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
