package com.example.biddoc.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordResetReqDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度不能小于8位")
    private String newPassword;
}
