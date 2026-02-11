package dev3.nms.mapper;

import dev3.nms.vo.board.BoardFileDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardFileMapper {

    // 게시글
    List<BoardFileDto.PostRes> findPostsPaged(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sort") String sort,
            @Param("order") String order,
            @Param("search") String search,
            @Param("category") String category,
            @Param("userId") Long userId
    );

    int countPosts(
            @Param("search") String search,
            @Param("category") String category,
            @Param("userId") Long userId
    );

    BoardFileDto.PostDetailRes findPostById(@Param("postId") Long postId);

    int insertPost(@Param("post") BoardFileDto.PostCreateReq post);

    int updatePost(@Param("postId") Long postId, @Param("post") BoardFileDto.PostUpdateReq post);

    int deletePost(@Param("postId") Long postId);

    int incrementViewCnt(@Param("postId") Long postId);

    // 첨부파일
    List<BoardFileDto.AttachRes> findAttachByPostId(@Param("postId") Long postId);

    BoardFileDto.AttachRes findAttachById(@Param("attachId") Long attachId);

    int insertAttach(@Param("attach") BoardFileDto.AttachInsertReq attach);

    int deleteAttachByIds(@Param("ids") List<Long> ids);

    int deleteAttachByPostId(@Param("postId") Long postId);

    int incrementDownCnt(@Param("attachId") Long attachId);

    BoardFileDto.AttachFileInfo findAttachFileInfo(@Param("attachId") Long attachId);
}
