package com.example.biddoc.auth.application.service;

import com.example.biddoc.auth.domain.enity.SysUser;

public interface UserService {

    SysUser getById(Long id);

    void changeStatus(Long userId, Integer status);
}

