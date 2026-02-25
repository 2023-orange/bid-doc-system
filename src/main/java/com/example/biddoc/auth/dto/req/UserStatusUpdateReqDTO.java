package com.example.biddoc.auth.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusUpdateReqDTO {

    @NotNull(message = "状态不能为空")
    private Integer status; // 1-启用 0-禁用
}
