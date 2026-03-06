package dev3.nms.service;

import dev3.nms.mapper.NoticeMapper;
import dev3.nms.vo.common.PageVO;
import dev3.nms.vo.notice.NoticeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 목록 조회 (페이지네이션)
     */
    public PageVO<NoticeDto.PostRes> getPostList(int page, int size, String search) {
        int offset = (page - 1) * size;
        List<NoticeDto.PostRes> posts = noticeMapper.findPostsPaged(offset, size, search);
        int totalCount = noticeMapper.countPosts(search);
        return PageVO.of(posts, page, size, totalCount);
    }

    /**
     * 상세 조회 (조회수 증가)
     */
    @Transactional
    public NoticeDto.PostDetailRes getPost(Long noticeId) {
        NoticeDto.PostDetailRes post = noticeMapper.findPostById(noticeId);
        if (post == null) return null;
        noticeMapper.incrementViewCnt(noticeId);
        return post;
    }

    /**
     * 등록 (긴급공지 시 WebSocket 브로드캐스트)
     */
    @Transactional
    public NoticeDto.PostDetailRes createPost(NoticeDto.PostCreateReq req) {
        noticeMapper.insertPost(req);
        NoticeDto.PostDetailRes saved = noticeMapper.findPostById(req.getNoticeId());

        // 긴급공지면 트랜잭션 커밋 후 WebSocket 브로드캐스트 (트랜잭션 블로킹 방지)
        if ("Y".equals(req.getIsUrgent())) {
            NoticeDto.UrgentNoticeMsg msg = NoticeDto.UrgentNoticeMsg.builder()
                    .noticeId(saved.getNoticeId())
                    .title(saved.getTitle())
                    .content(saved.getContent())
                    .userName(saved.getUserName())
                    .createdAt(saved.getCreatedAt())
                    .build();
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/notice/urgent", msg);
                        log.info("긴급공지 브로드캐스트 - noticeId: {}", saved.getNoticeId());
                    }
                }
            );
        }

        return saved;
    }

    /**
     * 수정
     */
    @Transactional
    public NoticeDto.PostDetailRes updatePost(Long noticeId, NoticeDto.PostUpdateReq req) {
        noticeMapper.updatePost(noticeId, req);
        return noticeMapper.findPostById(noticeId);
    }

    /**
     * 삭제 (소프트)
     */
    @Transactional
    public void deletePost(Long noticeId) {
        noticeMapper.deletePost(noticeId);
    }
}
