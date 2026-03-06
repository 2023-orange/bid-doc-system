package com.example.biddoc.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.auth.constant.SourceTypeEnum;
import com.example.biddoc.auth.dto.req.UserRoleAssignReqDTO;
import com.example.biddoc.auth.dto.resp.UserRoleRespDTO;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignRole(UserRoleAssignReqDTO req) {
        AssertUtil.isTrue(RoleCodeEnum.isValid(req.getRoleCode()), ErrorCode.PARAM_INVALID);

        SysUserRole existing = sysUserRoleMapper.selectOne(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, req.getUserId())
                        .eq(SysUserRole::getRoleCode, req.getRoleCode())
                        .eq(SysUserRole::getDeleted, false)
        );
        AssertUtil.isTrue(existing == null, ErrorCode.RESOURCE_CONFLICT);

        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            sysUserRoleMapper.update(null,
                    Wrappers.<SysUserRole>lambdaUpdate()
                            .set(SysUserRole::getIsPrimary, false)
                            .eq(SysUserRole::getUserId, req.getUserId())
                            .eq(SysUserRole::getIsPrimary, true)
                            .eq(SysUserRole::getDeleted, false)
            );
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(req.getUserId());
        userRole.setRoleCode(req.getRoleCode());
        userRole.setIsPrimary(Boolean.TRUE.equals(req.getIsPrimary()));
        userRole.setStatus(1);
        userRole.setEffectiveStartTime(req.getEffectiveStartTime());
        userRole.setEffectiveEndTime(req.getEffectiveEndTime());
        userRole.setSourceType(SourceTypeEnum.MANUAL.getCode());
        userRole.setDeleted(false);
        sysUserRoleMapper.insert(userRole);

        refreshUserRoleSession(req.getUserId());
        log.info("为用户 {} 分配角色 {}", req.getUserId(), req.getRoleCode());
    }

    @Override
    public void revokeRole(Long userId, String roleCode) {
        SysUserRole existing = sysUserRoleMapper.selectOne(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleCode, roleCode)
                        .eq(SysUserRole::getDeleted, false)
        );
        AssertUtil.notNull(existing, ErrorCode.RESOURCE_NOT_FOUND);

        existing.setDeleted(true);
        sysUserRoleMapper.updateById(existing);

        refreshUserRoleSession(userId);
        log.info("撤销用户 {} 的角色 {}", userId, roleCode);
    }

    @Override
    public List<UserRoleRespDTO> listByUserId(Long userId) {
        List<SysUserRole> roles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getDeleted, false)
                        .orderByDesc(SysUserRole::getIsPrimary)
                        .orderByAsc(SysUserRole::getCreatedAt)
        );
        return roles.stream().map(this::toRespDTO).toList();
    }

    @Override
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        OffsetDateTime now = OffsetDateTime.now();
        List<SysUserRole> roles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                        .select(SysUserRole::getUserId)
                        .eq(SysUserRole::getRoleCode, roleCode)
                        .eq(SysUserRole::getStatus, 1)
                        .eq(SysUserRole::getDeleted, false)
                        .and(w -> w.isNull(SysUserRole::getEffectiveStartTime)
                                .or().le(SysUserRole::getEffectiveStartTime, now))
                        .and(w -> w.isNull(SysUserRole::getEffectiveEndTime)
                                .or().gt(SysUserRole::getEffectiveEndTime, now))
        );
        return roles.stream().map(SysUserRole::getUserId).distinct().toList();
    }

    @Override
    public List<String> getActiveRoleCodes(Long userId) {
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
    }

    @Override
    public String getPrimaryRoleCode(Long userId) {
        SysUserRole primary = sysUserRoleMapper.selectOne(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getIsPrimary, true)
                        .eq(SysUserRole::getStatus, 1)
                        .eq(SysUserRole::getDeleted, false)
                        .last("LIMIT 1")
        );
        if (primary != null) {
            return primary.getRoleCode();
        }
        List<String> codes = getActiveRoleCodes(userId);
        return codes.isEmpty() ? null : codes.get(0);
    }

    @Override
    public void refreshUserRoleSession(Long userId) {
        try {
            var session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                List<String> roleCodes = getActiveRoleCodes(userId);
                session.set("roleCodes", roleCodes);
                log.info("已刷新用户 {} 的角色缓存: {}", userId, roleCodes);
            }
        } catch (Exception e) {
            log.warn("刷新用户 {} 角色缓存失败（可能未登录）: {}", userId, e.getMessage());
        }
    }

    private UserRoleRespDTO toRespDTO(SysUserRole entity) {
        UserRoleRespDTO dto = new UserRoleRespDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setRoleCode(entity.getRoleCode());
        RoleCodeEnum roleEnum = RoleCodeEnum.getByCode(entity.getRoleCode());
        dto.setRoleName(roleEnum != null ? roleEnum.getName() : entity.getRoleCode());
        dto.setIsPrimary(entity.getIsPrimary());
        dto.setStatus(entity.getStatus());
        dto.setEffectiveStartTime(entity.getEffectiveStartTime());
        dto.setEffectiveEndTime(entity.getEffectiveEndTime());
        dto.setSourceType(entity.getSourceType());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

