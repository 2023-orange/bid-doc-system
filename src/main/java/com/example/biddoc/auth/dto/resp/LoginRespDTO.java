package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRespDTO {

    /** 登录 Token */
    private String token;

    /** 过期时间（秒） */
    private long expireIn;

    /** 用户信息 */
    private UserDetailRespDTO user;

    /** 当前生效的角色码列表 */
    private List<String> roleCodes;
}
