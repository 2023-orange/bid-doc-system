package com.example.biddoc.auth.api.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterReqDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度不能小于8位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @NotNull(message = "职级不能为空")
    private Integer jobLevel;
}
