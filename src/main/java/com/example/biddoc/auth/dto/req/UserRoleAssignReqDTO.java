package com.example.biddoc.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserRoleAssignReqDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /** 是否设为主角色 */
    private Boolean isPrimary;

    /** 生效开始时间（为空表示立即生效） */
    private OffsetDateTime effectiveStartTime;

    /** 生效结束时间（为空表示永久有效） */
    private OffsetDateTime effectiveEndTime;
}

