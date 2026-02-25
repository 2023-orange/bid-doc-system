package com.example.biddoc.auth.service.impl;

import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.service.AuthService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ⭐ 修改1：变量名首字母小写（规范问题）
    private final SysUserMapper sysUserMapper;

    private static final Long DEFAULT_ROLE_ID = 2L;

    @Override
    public Long register(SysUser user) {
        SysUser exist = sysUserMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, user.getUsername())
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.isTrue(
                exist == null,
                ErrorCode.RESOURCE_CONFLICT
        );

        user.setStatus(0);
        user.setDeleted(false);

        if (user.getRoleId() == null) {
            user.setRoleId(DEFAULT_ROLE_ID);
        }

        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        sysUserMapper.insert(user);

        return user.getId();
    }

    @Override
    public SysUser login(String username, String password) {

        SysUser user = sysUserMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);

        AssertUtil.isTrue(
                Objects.equals(user.getPassword(), password),
                ErrorCode.AUTH_FAILED
        );

        AssertUtil.isTrue(
                user.getStatus() == 1,
                ErrorCode.ACCOUNT_DISABLED
        );

        user.setLastLoginTime(OffsetDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        sysUserMapper.updateById(user);

        return user;
    }
}