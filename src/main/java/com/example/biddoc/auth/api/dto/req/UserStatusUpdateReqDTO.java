package com.example.biddoc.auth.api.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateReqDTO {

    @NotNull(message = "状态不能为空")
    private Integer status; // 1-启用 0-禁用
}
