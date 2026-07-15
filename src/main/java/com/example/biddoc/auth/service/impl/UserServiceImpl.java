package com.example.biddoc.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.auth.constant.SourceTypeEnum;
import com.example.biddoc.auth.convertor.UserConvertor;
import com.example.biddoc.auth.dto.req.UserCreateReqDTO;
import com.example.biddoc.auth.dto.req.UserPasswordResetReqDTO;
import com.example.biddoc.auth.dto.req.UserUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.dto.resp.UserManageRespDTO;
import com.example.biddoc.auth.dto.resp.UserOptionRespDTO;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.auth.service.UserService;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_OPTION_SIZE = 20;
    private static final int MAX_OPTION_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_JOB_LEVEL = 3;

    private final SysUserMapper sysUserMapper;
    private final UserRoleService userRoleService;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetailRespDTO getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        AssertUtil.notNull(user, ErrorCode.RESOURCE_NOT_FOUND);

        UserDetailRespDTO resp = UserConvertor.toResp(user);
        List<String> roleCodes = userRoleService.getActiveRoleCodes(id);
        resp.setRoleCodes(roleCodes);
        return resp;
    }

    @Override
    public PageResponse<UserManageRespDTO> pageUsers(String keyword, Long deptId, Boolean status, String roleCode,
                                                     Integer page, Integer size) {
        int current = normalizePage(page);
        int pageSize = normalizePageSize(size);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Integer statusValue = status == null ? null : (Boolean.TRUE.equals(status) ? 1 : 0);

        List<Long> roleFilteredUserIds = null;
        if (StringUtils.hasText(roleCode)) {
            validateRoleCodes(List.of(roleCode));
            roleFilteredUserIds = listActiveUserIdsByRole(roleCode);
            if (roleFilteredUserIds.isEmpty()) {
                return new PageResponse<>(List.of(), current, pageSize, 0, 0, false);
            }
        }

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(current, pageSize),
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getDeleted, false)
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .eq(statusValue != null, SysUser::getStatus, statusValue)
                        .in(roleFilteredUserIds != null, SysUser::getId, roleFilteredUserIds)
                        .and(StringUtils.hasText(trimmedKeyword), wrapper -> wrapper
                                .like(SysUser::getUsername, trimmedKeyword)
                                .or()
                                .like(SysUser::getRealName, trimmedKeyword)
                                .or()
                                .like(SysUser::getMobile, trimmedKeyword))
                        .orderByDesc(SysUser::getUpdatedAt)
                        .orderByAsc(SysUser::getId)
        );

        List<SysUser> users = result.getRecords();
        Map<Long, SysDepartment> departmentMap = loadDepartmentMap(users);
        Map<Long, List<String>> roleCodeMap = loadActiveRoleCodeMap(users);
        Page<UserManageRespDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(users.stream()
                .map(user -> toManageResp(user, departmentMap.get(user.getDeptId()),
                        roleCodeMap.getOrDefault(user.getId(), List.of())))
                .toList());
        return PageResponse.of(dtoPage);
    }

    @Override
    public List<UserOptionRespDTO> listOptions(String keyword, Long deptId, Integer status, Integer size) {
        int limit = normalizeOptionSize(size);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        List<SysUser> users = sysUserMapper.selectList(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getDeleted, false)
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .eq(status != null, SysUser::getStatus, status)
                        .and(StringUtils.hasText(trimmedKeyword), wrapper -> wrapper
                                .like(SysUser::getRealName, trimmedKeyword)
                                .or()
                                .like(SysUser::getUsername, trimmedKeyword))
                        .orderByDesc(SysUser::getStatus)
                        .orderByAsc(SysUser::getRealName)
                        .orderByAsc(SysUser::getId)
                        .last("LIMIT " + limit)
        );

        Map<Long, SysDepartment> departmentMap = loadDepartmentMap(users);
        return users.stream()
                .map(user -> toOption(user, departmentMap.get(user.getDeptId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqDTO req) {
        validateDepartment(req.getDeptId());
        validateRoleCodes(req.getRoleCodes());
        ensureUsernameUnique(req.getUsername(), null);

        SysUser user = new SysUser();
        Long userId = IdWorker.getId();
        user.setId(userId);
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setMobile(req.getMobile());
        user.setEmail(req.getEmail());
        user.setDeptId(req.getDeptId());
        user.setJobLevel(normalizeJobLevel(req.getJobLevel()));
        user.setStatus(1);
        user.setDeleted(false);
        user.setLoginCount(0);
        sysUserMapper.insert(user);

        replaceUserRoles(userId, req.getRoleCodes());
        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateReqDTO req) {
        SysUser existing = requireAvailableUser(userId);
        validateDepartment(req.getDeptId());
        validateRoleCodes(req.getRoleCodes());
        ensureUsernameUnique(req.getUsername(), userId);

        existing.setUsername(req.getUsername());
        if (StringUtils.hasText(req.getPassword())) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        existing.setRealName(req.getRealName());
        existing.setMobile(req.getMobile());
        existing.setEmail(req.getEmail());
        existing.setDeptId(req.getDeptId());
        existing.setJobLevel(normalizeJobLevel(req.getJobLevel()));
        sysUserMapper.updateById(existing);

        replaceUserRoles(userId, req.getRoleCodes());
        userRoleService.refreshUserRoleSession(userId);
    }

    @Override
    public void resetPassword(Long userId, UserPasswordResetReqDTO req) {
        requireAvailableUser(userId);

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        sysUserMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        SysUser user = sysUserMapper.selectById(userId);
        AssertUtil.notNull(user, ErrorCode.RESOURCE_NOT_FOUND);
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户状态只能是启用或禁用");
        }

        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer size) {
        return size == null || size < 1 || size > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : size;
    }

    private int normalizeOptionSize(Integer size) {
        if (size == null) {
            return DEFAULT_OPTION_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_OPTION_SIZE);
    }

    private Map<Long, SysDepartment> loadDepartmentMap(List<SysUser> users) {
        Set<Long> deptIds = users.stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 负责人选择器需要展示部门名，但不能让前端拿 deptId 后再逐个请求部门详情。
        return sysDepartmentMapper.selectList(
                        Wrappers.<SysDepartment>lambdaQuery()
                                .in(SysDepartment::getId, deptIds)
                                .eq(SysDepartment::getDeleted, false)
                ).stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, List<String>> loadActiveRoleCodeMap(List<SysUser> users) {
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        OffsetDateTime now = OffsetDateTime.now();
        return sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery()
                                .in(SysUserRole::getUserId, userIds)
                                .eq(SysUserRole::getStatus, 1)
                                .eq(SysUserRole::getDeleted, false)
                                .and(w -> w.isNull(SysUserRole::getEffectiveStartTime)
                                        .or().le(SysUserRole::getEffectiveStartTime, now))
                                .and(w -> w.isNull(SysUserRole::getEffectiveEndTime)
                                        .or().gt(SysUserRole::getEffectiveEndTime, now))
                                .orderByDesc(SysUserRole::getIsPrimary)
                                .orderByAsc(SysUserRole::getCreatedAt)
                ).stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleCode, Collectors.toList())
                ));
    }

    private List<Long> listActiveUserIdsByRole(String roleCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery()
                                .eq(SysUserRole::getRoleCode, roleCode)
                                .eq(SysUserRole::getStatus, 1)
                                .eq(SysUserRole::getDeleted, false)
                                .and(w -> w.isNull(SysUserRole::getEffectiveStartTime)
                                        .or().le(SysUserRole::getEffectiveStartTime, now))
                                .and(w -> w.isNull(SysUserRole::getEffectiveEndTime)
                                        .or().gt(SysUserRole::getEffectiveEndTime, now))
                ).stream()
                .map(SysUserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void validateDepartment(Long deptId) {
        SysDepartment department = sysDepartmentMapper.selectById(deptId);
        if (department == null || Boolean.TRUE.equals(department.getDeleted())
                || !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部门不存在或已停用");
        }
    }

    private void validateRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "角色不能为空");
        }
        for (String roleCode : roleCodes) {
            if (!RoleCodeEnum.isValid(roleCode)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "角色码不合法");
            }
        }
    }

    private void ensureUsernameUnique(String username, Long excludeUserId) {
        SysUser existing = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, false)
                        .ne(excludeUserId != null, SysUser::getId, excludeUserId)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "用户名已存在");
        }
    }

    private SysUser requireAvailableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return user;
    }

    private int normalizeJobLevel(Integer jobLevel) {
        return jobLevel == null ? DEFAULT_JOB_LEVEL : jobLevel;
    }

    private void replaceUserRoles(Long userId, List<String> roleCodes) {
        // 用户角色是权限来源，更新时先软删旧关系再写入新关系，放在同一事务内避免半更新。
        SysUserRole delete = new SysUserRole();
        delete.setDeleted(true);
        sysUserRoleMapper.update(delete, Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getDeleted, false));

        List<String> distinctRoleCodes = new ArrayList<>(new LinkedHashSet<>(roleCodes));
        for (int i = 0; i < distinctRoleCodes.size(); i++) {
            SysUserRole role = new SysUserRole();
            role.setUserId(userId);
            role.setRoleCode(distinctRoleCodes.get(i));
            role.setIsPrimary(i == 0);
            role.setStatus(1);
            role.setSourceType(SourceTypeEnum.MANUAL.getCode());
            role.setDeleted(false);
            sysUserRoleMapper.insert(role);
        }
    }

    private UserOptionRespDTO toOption(SysUser user, SysDepartment department) {
        UserOptionRespDTO option = new UserOptionRespDTO();
        option.setId(user.getId());
        option.setRealName(user.getRealName());
        option.setUsername(user.getUsername());
        option.setDeptId(user.getDeptId());
        option.setDeptName(department == null ? null : department.getName());
        option.setStatus(user.getStatus());
        return option;
    }

    private UserManageRespDTO toManageResp(SysUser user, SysDepartment department, List<String> roleCodes) {
        UserManageRespDTO resp = new UserManageRespDTO();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setMobile(user.getMobile());
        resp.setEmail(user.getEmail());
        resp.setDeptId(user.getDeptId());
        resp.setDeptName(department == null ? null : department.getName());
        resp.setRoleCodes(roleCodes);
        resp.setStatus(Integer.valueOf(1).equals(user.getStatus()));
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        return resp;
    }
}
