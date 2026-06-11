package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class FolderGrantSaveReqDTO {

    /** 授权主体类型：USER / ROLE / DEPT，服务层二次校验枚举合法性 */
    @NotBlank(message = "subjectType不能为空")
    private String subjectType;

    /** 授权主体ID：USER 传 userId，ROLE 传 roleCode，DEPT 传 deptId */
    @NotBlank(message = "subjectId不能为空")
    @Size(max = 64, message = "subjectId不能超过64个字符")
    private String subjectId;

    /** 权限码列表，服务层校验每项是否在 FolderPermissionCodeEnum 内 */
    @NotEmpty(message = "permissionCodes不能为空")
    private List<String> permissionCodes;

    /** 授权范围：SELF / SELF_AND_DESCENDANTS */
    @NotBlank(message = "grantScope不能为空")
    private String grantScope;

    /** 授权生效时间，为空表示立即生效 */
    private OffsetDateTime effectiveFrom;

    /** 授权失效时间，为空表示永久有效 */
    private OffsetDateTime effectiveTo;
}
