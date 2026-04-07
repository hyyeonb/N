package dev3.nms.mapper;

import dev3.nms.vo.notification.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EmailPrefMapper {
    // Master pref
    EmailPrefVO findByUserId(@Param("userId") Long userId);
    void upsertPref(EmailPrefVO pref);

    // Type × Severity matrix
    List<EmailTypePrefVO> findTypePrefs(@Param("userId") Long userId);
    void deleteTypePrefs(@Param("userId") Long userId);
    void insertTypePrefs(@Param("userId") Long userId, @Param("list") List<EmailTypePrefVO> list);
    EmailTypePrefVO findTypeMode(@Param("userId") Long userId, @Param("alertType") String alertType, @Param("severity") String severity);

    // Device override
    List<EmailDevicePrefVO> findDevicePrefs(@Param("userId") Long userId);
    List<EmailDevicePrefVO> findDevicePrefsByDevice(@Param("userId") Long userId, @Param("deviceId") Integer deviceId);
    void deleteDevicePrefs(@Param("userId") Long userId, @Param("deviceId") Integer deviceId);
    void insertDevicePrefs(@Param("list") List<EmailDevicePrefVO> list);
    void deleteAllDevicePrefs(@Param("userId") Long userId);
    EmailDevicePrefVO findDeviceMode(@Param("userId") Long userId, @Param("deviceId") Integer deviceId, @Param("alertType") String alertType, @Param("severity") String severity);

    // System email
    List<SystemEmailVO> findAllSystemEmails();
    void insertSystemEmail(SystemEmailVO vo);
    void updateSystemEmail(SystemEmailVO vo);
    void deleteSystemEmail(@Param("emailId") Integer emailId);

    // All enabled users for alert processing
    List<EmailPrefVO> findAllEnabled();
}
