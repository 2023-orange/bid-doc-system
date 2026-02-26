package com.example.biddoc.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.service.AuthService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        user.setStatus(0);// PENDING
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
    public SaTokenInfo login(String username, String password) {
        // 1. 使用 MyBatis-Plus 的条件构造器查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
        );

        // 2. 基础校验
        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);
        AssertUtil.isTrue(Objects.equals(user.getPassword(), password), ErrorCode.AUTH_FAILED);
        AssertUtil.isTrue(user.getStatus() == 1, ErrorCode.ACCOUNT_DISABLED);

        // 3. 更新登录统计信息
        user.setLastLoginTime(OffsetDateTime.from(LocalDateTime.now()));
        user.setLoginCount(user.getLoginCount() + 1);
        sysUserMapper.updateById(user); // 使用 updateById 更新

        // 4. 【Sa-Token】执行登录，写入 Redis
        StpUtil.login(user.getId());

        // 5. 【会话缓存】将脱敏后的用户信息存入 Redis Session，后续接口拦截直接从这里读，不用查库
        user.setPassword(null);
        StpUtil.getSession().set("user", user);

        // 6. 返回令牌
        return StpUtil.getTokenInfo();
    }
}