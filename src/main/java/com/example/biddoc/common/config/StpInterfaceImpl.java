package com.example.biddoc.common.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色与权限提供器
 * <p>
 * 角色数据来源优先级：Sa-Token Session 缓存 → 数据库查询（并回填缓存）
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 1. 优先从 Sa-Token Session 读取缓存的角色列表（登录时写入，角色变更时刷新）
        try {
            SaSession session = StpUtil.getSessionByLoginId(loginId, false);
            if (session != null) {
                @SuppressWarnings("unchecked")
                List<String> cached = (List<String>) session.get("roleCodes");
                if (cached != null) {
                    return cached;
                }
            }
        } catch (Exception e) {
            log.debug("读取Session角色缓存失败, loginId={}", loginId);
        }

        // 2. 缓存未命中，从 sys_user_role 表查询当前生效角色
        List<String> roleCodes = queryActiveRolesFromDb(loginId);

        // 3. 回填到 Session 以便下次命中缓存
        try {
            SaSession session = StpUtil.getSessionByLoginId(loginId, false);
            if (session != null) {
                session.set("roleCodes", roleCodes);
            }
        } catch (Exception ignored) {
        }

        return roleCodes;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    private List<String> queryActiveRolesFromDb(Object loginId) {
        try {
            Long userId = Long.valueOf(loginId.toString());
            OffsetDateTime now = OffsetDateTime.now();
            List<SysUserRole> roles = sysUserRoleMapper.selectList(
                    Wrappers.<SysUserRole>lambdaQuery()
                            .eq(SysUserRole::getUserId, userId)
                            .eq(SysUserRole::getStatus, 1)
                            .eq(SysUserRole::getDeleted, false)
                            .and(w -> w.isNull(SysUserRole::getEffectiveStartTime)
                                    .or().le(SysUserRole::getEffectiveStartTime, now))
                            .and(w -> w.isNull(SysUserRole::getEffectiveEndTime)
                                    .or().gt(SysUserRole::getEffectiveEndTime, now))
            );
            return roles.stream()
                    .map(SysUserRole::getRoleCode)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.error("查询用户角色失败, loginId={}", loginId, e);
            return new ArrayList<>();
        }
    }
}
