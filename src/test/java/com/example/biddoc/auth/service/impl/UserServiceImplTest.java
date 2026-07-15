package com.example.biddoc.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.auth.dto.req.UserCreateReqDTO;
import com.example.biddoc.auth.dto.req.UserPasswordResetReqDTO;
import com.example.biddoc.auth.dto.req.UserUpdateReqDTO;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysDepartmentMapper sysDepartmentMapper = mock(SysDepartmentMapper.class);
    private final SysUserRoleMapper sysUserRoleMapper = mock(SysUserRoleMapper.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(
            sysUserMapper,
            userRoleService,
            sysDepartmentMapper,
            sysUserRoleMapper,
            passwordEncoder
    );

    @Test
    void changeStatusRejectsUnsupportedStatusValue() {
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(1);
        user.setDeleted(false);
        when(sysUserMapper.selectById(10L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(10L, 9));

        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        verify(sysUserMapper, never()).updateById(user);
    }

    @Test
    void listOptionsReturnsUserDisplayFieldsAndDepartmentName() {
        SysUser user = new SysUser();
        user.setId(10L);
        user.setUsername("alice");
        user.setRealName("张三");
        user.setDeptId(20L);
        user.setStatus(1);
        user.setDeleted(false);

        SysDepartment department = new SysDepartment();
        department.setId(20L);
        department.setName("研发部");
        department.setDeleted(false);

        when(sysUserMapper.selectList(any())).thenReturn(List.of(user));
        when(sysDepartmentMapper.selectList(any())).thenReturn(List.of(department));

        var options = service.listOptions("张", null, 1, 20);

        assertEquals(1, options.size());
        assertEquals(10L, options.get(0).getId());
        assertEquals("张三", options.get(0).getRealName());
        assertEquals("alice", options.get(0).getUsername());
        assertEquals(20L, options.get(0).getDeptId());
        assertEquals("研发部", options.get(0).getDeptName());
        assertEquals(1, options.get(0).getStatus());
    }

    @Test
    void pageUsersMapsBooleanStatusAndIncludesDepartmentAndRoleCodes() {
        SysUser user = new SysUser();
        user.setId(10L);
        user.setUsername("alice");
        user.setRealName("张三");
        user.setMobile("13800000000");
        user.setEmail("alice@example.com");
        user.setDeptId(20L);
        user.setStatus(1);
        user.setDeleted(false);

        Page<SysUser> userPage = new Page<>(1, 20, 1);
        userPage.setRecords(List.of(user));
        when(sysUserMapper.selectPage(any(Page.class), any())).thenReturn(userPage);

        SysDepartment department = new SysDepartment();
        department.setId(20L);
        department.setName("研发部");
        department.setDeleted(false);
        when(sysDepartmentMapper.selectList(any())).thenReturn(List.of(department));

        SysUserRole role = new SysUserRole();
        role.setUserId(10L);
        role.setRoleCode("EMPLOYEE");
        role.setStatus(1);
        role.setDeleted(false);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(role));

        var result = service.pageUsers("张", null, Boolean.TRUE, null, 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals("alice", result.getList().get(0).getUsername());
        assertEquals("研发部", result.getList().get(0).getDeptName());
        assertEquals(List.of("EMPLOYEE"), result.getList().get(0).getRoleCodes());
        assertTrue(result.getList().get(0).getStatus());
    }

    @Test
    void createUserStoresEnabledUserAndActiveRoles() {
        SysDepartment department = new SysDepartment();
        department.setId(20L);
        department.setStatus(1);
        department.setDeleted(false);
        when(sysDepartmentMapper.selectById(20L)).thenReturn(department);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");

        UserCreateReqDTO req = new UserCreateReqDTO();
        req.setUsername("alice");
        req.setPassword("Password123");
        req.setRealName("张三");
        req.setMobile("13800000000");
        req.setEmail("alice@example.com");
        req.setDeptId(20L);
        req.setRoleCodes(List.of("DEPT_MANAGER", "EMPLOYEE"));

        Long userId = service.createUser(req);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        assertEquals(userId, userCaptor.getValue().getId());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals(1, userCaptor.getValue().getStatus());
        assertFalse(userCaptor.getValue().getDeleted());

        verify(sysUserRoleMapper).insert(org.mockito.ArgumentMatchers.argThat(role ->
                userId.equals(role.getUserId())
                        && "DEPT_MANAGER".equals(role.getRoleCode())
                        && Boolean.TRUE.equals(role.getIsPrimary())
                        && Integer.valueOf(1).equals(role.getStatus())
                        && Boolean.FALSE.equals(role.getDeleted())));
        verify(sysUserRoleMapper).insert(org.mockito.ArgumentMatchers.argThat(role ->
                userId.equals(role.getUserId())
                        && "EMPLOYEE".equals(role.getRoleCode())
                        && Boolean.FALSE.equals(role.getIsPrimary())));
    }

    @Test
    void updateUserKeepsPasswordWhenPasswordIsBlankAndReplacesRoles() {
        SysUser existing = new SysUser();
        existing.setId(10L);
        existing.setUsername("alice");
        existing.setPassword("old-password");
        existing.setStatus(1);
        existing.setDeleted(false);
        when(sysUserMapper.selectById(10L)).thenReturn(existing);

        SysDepartment department = new SysDepartment();
        department.setId(20L);
        department.setStatus(1);
        department.setDeleted(false);
        when(sysDepartmentMapper.selectById(20L)).thenReturn(department);

        UserUpdateReqDTO req = new UserUpdateReqDTO();
        req.setUsername("alice-new");
        req.setPassword(" ");
        req.setRealName("张三");
        req.setMobile("13800000000");
        req.setEmail("alice@example.com");
        req.setDeptId(20L);
        req.setRoleCodes(List.of("EMPLOYEE"));

        service.updateUser(10L, req);

        verify(sysUserMapper).updateById(org.mockito.ArgumentMatchers.argThat(user ->
                Long.valueOf(10L).equals(user.getId())
                        && "alice-new".equals(user.getUsername())
                        && "old-password".equals(user.getPassword())));
        verify(sysUserRoleMapper).update(any(), any());
        verify(sysUserRoleMapper).insert(org.mockito.ArgumentMatchers.argThat(role ->
                Long.valueOf(10L).equals(role.getUserId())
                        && "EMPLOYEE".equals(role.getRoleCode())
                        && Boolean.TRUE.equals(role.getIsPrimary())));
    }

    @Test
    void resetPasswordEncodesNewPasswordOnlyForExistingUser() {
        SysUser existing = new SysUser();
        existing.setId(10L);
        existing.setDeleted(false);
        when(sysUserMapper.selectById(10L)).thenReturn(existing);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

        UserPasswordResetReqDTO req = new UserPasswordResetReqDTO();
        req.setNewPassword("NewPassword123");

        service.resetPassword(10L, req);

        verify(sysUserMapper).updateById(org.mockito.ArgumentMatchers.argThat(user ->
                Long.valueOf(10L).equals(user.getId())
                        && "encoded-new-password".equals(user.getPassword())));
    }
}
