package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FolderManagerSaveReqDTO {

    /** 被设置为管理员的用户ID，该用户必须持有 FOLDER_ADMIN 角色 */
    @NotNull(message = "userId不能为空")
    private Long userId;

    /** 管理范围：SELF / SELF_AND_DESCENDANTS */
    @NotBlank(message = "manageScope不能为空")
    private String manageScope;
}
