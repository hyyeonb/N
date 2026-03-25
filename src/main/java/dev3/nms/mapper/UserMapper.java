package dev3.nms.mapper;

import dev3.nms.vo.auth.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<UserVO> findBySocialTypeAndSocialId(@Param("SOCIAL_TYPE") String socialType,
                                                   @Param("SOCIAL_ID") String socialId);

    Optional<UserVO> findByEmail(@Param("EMAIL") String email);

    Optional<UserVO> findById(@Param("USER_ID") Long userId);

    void insert(UserVO user);

    void insertLocal(UserVO user);

    Optional<UserVO> findByLoginId(@Param("LOGIN_ID") String loginId);

    void update(UserVO user);

    Optional<UserVO> findByPhone(@Param("PHONE") String phone);

    Optional<UserVO> findByNameAndPhone(@Param("NAME") String name, @Param("PHONE") String phone);

    Optional<UserVO> findByLoginIdAndNameAndPhone(@Param("LOGIN_ID") String loginId,
                                                    @Param("NAME") String name,
                                                    @Param("PHONE") String phone);

    void updatePassword(@Param("USER_ID") Long userId, @Param("PASSWORD") String password);

    List<UserVO> findAll();

    void updateStatus(@Param("USER_ID") Long userId, @Param("STATUS") String status);

    void updateAllGroupView(@Param("USER_ID") Long userId, @Param("ALL_GROUP_VIEW") Boolean allGroupView);

    void updateReviewedAt(@Param("USER_ID") Long userId);

    /**
     * 프로필 정보 수정 (이름, 이메일, 전화번호)
     */
    void updateProfile(@Param("USER_ID") Long userId,
                       @Param("NAME") String name,
                       @Param("EMAIL") String email,
                       @Param("PHONE") String phone);
}
