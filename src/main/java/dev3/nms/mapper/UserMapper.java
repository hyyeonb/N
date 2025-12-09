package dev3.nms.mapper;

import dev3.nms.vo.auth.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {

    /**
     * 소셜 타입과 소셜 ID로 사용자 조회
     */
    Optional<UserVO> findBySocialTypeAndSocialId(@Param("SOCIAL_TYPE") String socialType,
                                                   @Param("SOCIAL_ID") String socialId);

    /**
     * 이메일로 사용자 조회
     */
    Optional<UserVO> findByEmail(@Param("EMAIL") String email);

    /**
     * 새 사용자 생성
     */
    void insert(UserVO user);

    /**
     * 사용자 정보 업데이트
     */
    void update(UserVO user);
}
