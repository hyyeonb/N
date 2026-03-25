package dev3.nms.mapper;

import dev3.nms.vo.auth.PageAccessVO;
import dev3.nms.vo.auth.PageMasterVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PageAccessMapper {

    List<PageMasterVO> findAllPages();

    List<PageAccessVO> findByUserId(@Param("USER_ID") Long userId);

    PageAccessVO findByUserIdAndPageCode(@Param("USER_ID") Long userId, @Param("PAGE_CODE") String pageCode);

    void insertPageAccess(PageAccessVO access);

    void insertPageAccessBatch(@Param("list") List<PageAccessVO> accessList);

    void updatePageAccess(PageAccessVO access);

    void deleteByUserId(@Param("USER_ID") Long userId);

    void deleteByUserIdAndPageCode(@Param("USER_ID") Long userId, @Param("PAGE_CODE") String pageCode);
}
