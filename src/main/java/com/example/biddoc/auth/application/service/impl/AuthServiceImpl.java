package com.example.biddoc.auth.application.service.impl;

import com.example.biddoc.auth.application.service.AuthService;
import com.example.biddoc.auth.domain.enity.SysUser;
import com.example.biddoc.auth.domain.repository.SysUserRepository;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;

    // 定义一个常量，或者从配置文件/枚举中获取
    // 假设数据库中 sys_role 表里，ID 为 2 的是“普通用户”
    private static final Long DEFAULT_ROLE_ID = 2L;

    @Override
    public Long register(SysUser user) {

        AssertUtil.isTrue(
                userRepository.findByUsername(user.getUsername()) == null,
                ErrorCode.RESOURCE_CONFLICT
        );

        user.setStatus(0); // PENDING
        user.setDeleted(false);

        // --- 设置默认角色 ---
        // 如果前端没有传角色（通常注册接口不传），则设置为默认角色
        if (user.getRoleId() == null) {
            user.setRoleId(DEFAULT_ROLE_ID);
        }

        // 【核心修改】注册场景：手动设置为系统
        // 这样设置后，MyBatis Plus 的自动填充器会跳过这两个字段，避免报错
        user.setCreatedBy("system");
        user.setUpdatedBy("system");

        userRepository.save(user);
        return user.getId();
    }

    @Override
    public SysUser login(String username, String password) {

        SysUser user = userRepository.findByUsername(username);
        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);

        AssertUtil.isTrue(
                Objects.equals(user.getPassword(), password),
                ErrorCode.AUTH_FAILED
        );

        AssertUtil.isTrue(
                user.getStatus() == 1,
                ErrorCode.ACCOUNT_DISABLED
        );

        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.update(user);

        return user;
    }
}
