package dev3.nms.mapper;

import dev3.nms.vo.auth.SocialAccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SocialAccountMapper {
    /**
     * 소셜 계정 정보 저장
     */
    void insert(SocialAccountVO account);

    /**
     * 소셜 계정 정보 업데이트
     */
    void update(SocialAccountVO account);

    /**
     * 존재 여부 확인
     */
    int countByUserIdAndSocialType(@Param("USER_ID") Long userId,
                                     @Param("SOCIAL_TYPE") String socialType);
}
