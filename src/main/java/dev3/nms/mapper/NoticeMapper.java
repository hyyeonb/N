package dev3.nms.mapper;

import dev3.nms.vo.notice.NoticeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<NoticeDto.PostRes> findPostsPaged(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("search") String search
    );

    int countPosts(@Param("search") String search);

    NoticeDto.PostDetailRes findPostById(@Param("noticeId") Long noticeId);

    int insertPost(@Param("post") NoticeDto.PostCreateReq post);

    int updatePost(@Param("noticeId") Long noticeId, @Param("post") NoticeDto.PostUpdateReq post);

    int deletePost(@Param("noticeId") Long noticeId);

    int incrementViewCnt(@Param("noticeId") Long noticeId);
}
