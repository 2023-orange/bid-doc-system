package com.example.biddoc.auth.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.domain.enity.SysUser;
import com.example.biddoc.auth.domain.repository.SysUserRepository;
import com.example.biddoc.auth.infrastructure.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SysUserRepositoryImpl implements SysUserRepository {

    private final SysUserMapper mapper;

    @Override
    public SysUser findByUsername(String username) {
        return mapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
        );
    }

    @Override
    public SysUser findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public void save(SysUser user) {
        mapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        mapper.updateById(user);
    }
}
