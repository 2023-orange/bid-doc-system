package com.example.biddoc.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.auth.constant.SourceTypeEnum;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import com.example.biddoc.auth.service.AuthService;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserRoleService userRoleService;

    @Override
    public Long register(SysUser user) {
        SysUser exist = sysUserMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, user.getUsername())
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.isTrue(exist == null, ErrorCode.RESOURCE_CONFLICT);

        user.setStatus(0); // PENDING
        user.setDeleted(false);
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        sysUserMapper.insert(user);

        // 注册成功后，自动分配 EMPLOYEE 默认角色
        SysUserRole defaultRole = new SysUserRole();
        defaultRole.setUserId(user.getId());
        defaultRole.setRoleCode(RoleCodeEnum.EMPLOYEE.getCode());
        defaultRole.setIsPrimary(true);
        defaultRole.setStatus(1);
        defaultRole.setSourceType(SourceTypeEnum.SYSTEM_INIT.getCode());
        defaultRole.setDeleted(false);
        defaultRole.setCreatedBy("system");
        defaultRole.setUpdatedBy("system");
        sysUserRoleMapper.insert(defaultRole);

        return user.getId();
    }

    @Override
    public SaTokenInfo login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);
        AssertUtil.isTrue(Objects.equals(user.getPassword(), password), ErrorCode.AUTH_FAILED);
        AssertUtil.isTrue(user.getStatus() == 1, ErrorCode.ACCOUNT_DISABLED);

        user.setLastLoginTime(OffsetDateTime.now());
        user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
        sysUserMapper.updateById(user);

        StpUtil.login(user.getId());

        // 脱敏后存入 Session
        user.setPassword(null);
        StpUtil.getSession().set("user", user);

        // 查询并缓存当前生效的角色码列表到 Session
        List<String> roleCodes = userRoleService.getActiveRoleCodes(user.getId());
        StpUtil.getSession().set("roleCodes", roleCodes);
        log.info("用户 {} 登录成功，角色: {}", username, roleCodes);

        return StpUtil.getTokenInfo();
    }
}