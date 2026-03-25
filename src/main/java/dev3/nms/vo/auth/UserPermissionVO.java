package dev3.nms.vo.auth;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean isAdmin;
    private boolean allGroupView;
    private List<PageAccessVO> pageAccess;
    private List<GroupAccessVO> groupAccess;
}
