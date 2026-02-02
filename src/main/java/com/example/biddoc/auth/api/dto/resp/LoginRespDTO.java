package com.example.biddoc.auth.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRespDTO {

    /**
     * 登录 Token
     */
    private String token;

    /**
     * 过期时间（秒）
     */
    private long expireIn;

    /**
     * 用户信息
     */
    private UserDetailRespDTO user;

}
