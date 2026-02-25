package com.example.biddoc.auth.service;

import com.example.biddoc.auth.entity.SysUser;

public interface AuthService {

    Long register(SysUser user);

    SysUser login(String username, String password);
}

