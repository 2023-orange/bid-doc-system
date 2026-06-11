package com.example.biddoc.auth.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long REMEMBER_ME_TIMEOUT_SECONDS = 5L * 24 * 60 * 60;
    private static final long REMEMBER_ME_ACTIVE_TIMEOUT_SECONDS = 5L * 24 * 60 * 60;
    private static final long DEFAULT_ACTIVE_TIMEOUT_SECONDS = 30L * 60;

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long register(SysUser user) {
        SysUser exist = sysUserMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, user.getUsername())
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.isTrue(exist == null, ErrorCode.RESOURCE_CONFLICT);

        // Goal-0: 注册时使用 BCrypt 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(0); // PENDING
        user.setDeleted(false);
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        sysUserMapper.insert(user);

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
    public SaTokenInfo login(String username, String password, Boolean rememberMe) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
        );

        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);

        // Goal-0: 兼容旧明文密码 + BCrypt 密码校验
        String storedPassword = user.getPassword();
        boolean passwordMatches;

        if (isBcryptHash(storedPassword)) {
            // 标准路径：BCrypt 比对
            try {
                passwordMatches = passwordEncoder.matches(password, storedPassword);
            } catch (IllegalArgumentException e) {
                // BCrypt hash 格式损坏，视为校验失败
                log.warn("用户 {} 的密码 hash 格式异常", username, e);
                passwordMatches = false;
            }
        } else {
            // 兼容路径：旧明文比对
            passwordMatches = Objects.equals(storedPassword, password);
            if (passwordMatches) {
                // 比对成功后立即升级：用 BCrypt 重新 encode 写回 DB
                try {
                    user.setPassword(passwordEncoder.encode(password));
                    sysUserMapper.updateById(user);
                    log.info("用户 {} 密码已自动升级为 BCrypt", username);
                } catch (Exception e) {
                    // 升级失败不阻塞登录（已确认密码正确），留待下次登录再尝试
                    log.warn("用户 {} 密码升级失败，下次登录时重试", username, e);
                }
            }
        }

        AssertUtil.isTrue(passwordMatches, ErrorCode.AUTH_FAILED);
        AssertUtil.isTrue(user.getStatus() == 1, ErrorCode.ACCOUNT_DISABLED);

        user.setLastLoginTime(OffsetDateTime.now());
        user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
        sysUserMapper.updateById(user);

        boolean rememberMeEnabled = Boolean.TRUE.equals(rememberMe);
        SaLoginModel loginModel = SaLoginModel.create()
                .setTimeout(rememberMeEnabled ? REMEMBER_ME_TIMEOUT_SECONDS : -1)
                .setActiveTimeout(rememberMeEnabled ? REMEMBER_ME_ACTIVE_TIMEOUT_SECONDS : DEFAULT_ACTIVE_TIMEOUT_SECONDS);
        StpUtil.login(user.getId(), loginModel);

        user.setPassword(null);
        StpUtil.getSession().set("user", user);

        List<String> roleCodes = userRoleService.getActiveRoleCodes(user.getId());
        StpUtil.getSession().set("roleCodes", roleCodes);
        log.info("用户 {} 登录成功, rememberMe={}, roleCodes={}", username, rememberMeEnabled, roleCodes);

        return StpUtil.getTokenInfo();
    }

    /**
     * 判断密码字符串是否为 BCrypt hash 格式。
     * BCrypt 输出固定 60 字符，以 $2a$ / $2b$ / $2y$ 开头。
     */
    private static boolean isBcryptHash(String stored) {
        return stored != null
                && stored.length() == 60
                && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }
}
