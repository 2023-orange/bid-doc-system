package com.example.biddoc.auth.domain.repository;

import com.example.biddoc.auth.domain.enity.SysUser;

public interface SysUserRepository {

    SysUser findByUsername(String username);

    SysUser findById(Long id);

    void save(SysUser user);

    void update(SysUser user);
}

