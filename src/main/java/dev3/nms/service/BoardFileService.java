package dev3.nms.service;

import dev3.nms.mapper.BoardFileMapper;
import dev3.nms.vo.board.BoardFileDto;
import dev3.nms.vo.common.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardFileService {

    private final BoardFileMapper boardFileMapper;

    @Value("${file.upload.path}")
    private String uploadBasePath;

    /**
     * 목록 조회 (페이지네이션)
     */
    public PageVO<BoardFileDto.PostRes> getPostList(int page, int size, String sort, String order,
                                                     String search, String category, Long userId) {
        int offset = (page - 1) * size;
        List<BoardFileDto.PostRes> posts = boardFileMapper.findPostsPaged(offset, size, sort, order, search, category, userId);
        int totalCount = boardFileMapper.countPosts(search, category, userId);
        return PageVO.of(posts, page, size, totalCount);
    }

    /**
     * 상세 조회 (조회수 증가 + 첨부파일 포함, 비공개글은 작성자만)
     */
    @Transactional
    public BoardFileDto.PostDetailRes getPost(Long postId, Long userId) {
        BoardFileDto.PostDetailRes post = boardFileMapper.findPostById(postId);
        if (post == null) return null;

        // 비공개글은 작성자만 조회 가능
        if ("N".equals(post.getIsPublic()) && !post.getUserId().equals(userId)) {
            return null;
        }

        boardFileMapper.incrementViewCnt(postId);
        post.setAttachments(boardFileMapper.findAttachByPostId(postId));
        return post;
    }

    /**
     * 등록 (게시글 + 첨부파일)
     */
    @Transactional
    public BoardFileDto.PostDetailRes createPost(BoardFileDto.PostCreateReq req,
                                                  List<MultipartFile> files,
                                                  Long userId) throws IOException {
        req.setUserId(userId);
        boardFileMapper.insertPost(req);
        Long postId = req.getPostId();

        // 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            saveFiles(files, postId, userId);
        }

        return getPostWithoutViewIncrement(postId);
    }

    /**
     * 수정 (게시글 + 첨부파일 삭제/추가)
     */
    @Transactional
    public BoardFileDto.PostDetailRes updatePost(Long postId, BoardFileDto.PostUpdateReq req,
                                                  List<MultipartFile> files) throws IOException {
        boardFileMapper.updatePost(postId, req);

        // 삭제 요청된 첨부파일 제거
        if (req.getDeleteAttachIds() != null && !req.getDeleteAttachIds().isEmpty()) {
            boardFileMapper.deleteAttachByIds(req.getDeleteAttachIds());
        }

        // 새 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            BoardFileDto.PostDetailRes post = boardFileMapper.findPostById(postId);
            saveFiles(files, postId, post.getUserId());
        }

        return getPostWithoutViewIncrement(postId);
    }

    /**
     * 삭제 (소프트 삭제)
     */
    @Transactional
    public void deletePost(Long postId) {
        boardFileMapper.deletePost(postId);
    }

    /**
     * 첨부파일 다운로드
     */
    @Transactional
    public BoardFileDto.AttachFileInfo getDownloadFileInfo(Long attachId) {
        boardFileMapper.incrementDownCnt(attachId);
        return boardFileMapper.findAttachFileInfo(attachId);
    }

    public Resource loadFileAsResource(BoardFileDto.AttachFileInfo fileInfo) throws IOException {
        Path filePath = Paths.get(fileInfo.getFilePath()).resolve(fileInfo.getStoredName()).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new IOException("파일을 찾을 수 없습니다: " + fileInfo.getOrgFileName());
        }
        return resource;
    }

    // ======== private ========

    /**
     * 조회수 증가 없이 상세 조회 (등록/수정 후 응답용)
     */
    private BoardFileDto.PostDetailRes getPostWithoutViewIncrement(Long postId) {
        BoardFileDto.PostDetailRes post = boardFileMapper.findPostById(postId);
        if (post != null) {
            post.setAttachments(boardFileMapper.findAttachByPostId(postId));
        }
        return post;
    }

    /**
     * 물리 파일 저장 + DB INSERT
     */
    private void saveFiles(List<MultipartFile> files, Long postId, Long userId) throws IOException {
        LocalDate now = LocalDate.now();
        String subDir = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path dirPath = Paths.get(uploadBasePath, "board", subDir);
        Files.createDirectories(dirPath);

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String orgName = file.getOriginalFilename();
            String ext = "";
            if (orgName != null && orgName.contains(".")) {
                ext = orgName.substring(orgName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID() + ext;

            Path target = dirPath.resolve(storedName);
            file.transferTo(target.toFile());

            BoardFileDto.AttachInsertReq attach = new BoardFileDto.AttachInsertReq();
            attach.setPostId(postId);
            attach.setOrgFileName(orgName);
            attach.setStoredName(storedName);
            attach.setFilePath(dirPath.toString());
            attach.setFileSize(file.getSize());
            attach.setMimeType(file.getContentType());
            attach.setUserId(userId);
            boardFileMapper.insertAttach(attach);
        }
    }
}
