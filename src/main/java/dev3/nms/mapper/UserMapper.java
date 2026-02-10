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
     * 새 사용자 생성 (소셜 로그인)
     */
    void insert(UserVO user);

    /**
     * 새 사용자 생성 (로컬 회원가입)
     */
    void insertLocal(UserVO user);

    /**
     * 로그인 ID로 사용자 조회
     */
    Optional<UserVO> findByLoginId(@Param("LOGIN_ID") String loginId);

    /**
     * 사용자 정보 업데이트
     */
    void update(UserVO user);

    /**
     * 전화번호로 사용자 조회 (전화번호 중복 체크)
     */
    Optional<UserVO> findByPhone(@Param("PHONE") String phone);

    /**
     * 이름과 전화번호로 사용자 조회 (아이디 찾기)
     */
    Optional<UserVO> findByNameAndPhone(@Param("NAME") String name, @Param("PHONE") String phone);

    /**
     * 로그인 ID, 이름, 전화번호로 사용자 조회 (비밀번호 재설정 검증)
     */
    Optional<UserVO> findByLoginIdAndNameAndPhone(@Param("LOGIN_ID") String loginId,
                                                    @Param("NAME") String name,
                                                    @Param("PHONE") String phone);

    /**
     * 비밀번호 변경
     */
    void updatePassword(@Param("USER_ID") Long userId, @Param("PASSWORD") String password);
}
