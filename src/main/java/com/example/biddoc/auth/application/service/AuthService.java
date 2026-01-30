package com.example.biddoc.auth.application.service;

import com.example.biddoc.auth.domain.enity.SysUser;

public interface AuthService {

    Long register(SysUser user);

    SysUser login(String username, String password);
}

