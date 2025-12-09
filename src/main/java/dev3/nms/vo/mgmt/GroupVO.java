package dev3.nms.vo.mgmt;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO {
    private Integer GROUP_ID;
    private Integer PARENT_GROUP_ID; // Changed from PARENT_ID
    private String GROUP_NAME;
    private String ADDRESS;
    private String PHONE;
    private String ICON_NAME; // 그룹 아이콘 이름
    private Integer DEPTH;
    private Integer CREATE_USER_ID;
    private LocalDateTime CREATE_AT;
    private Integer MODIFY_USER_ID;
    private LocalDateTime MODIFY_AT;
    private Integer DELETE_USER_ID;
    private LocalDateTime DELETE_AT;
    private List<GroupVO> children;
}
