package com.example.biddoc.auth.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.example.biddoc.auth.entity.SysUser;

public interface AuthService {

    Long register(SysUser user);

    /**
     * 用户登录，返回 Sa-Token 令牌信息
     */
    SaTokenInfo login(String username, String password);
}

