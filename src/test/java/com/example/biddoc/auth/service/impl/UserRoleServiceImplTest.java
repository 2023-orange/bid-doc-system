package com.example.biddoc.auth.service.impl;

import com.example.biddoc.auth.dto.req.UserRoleAssignReqDTO;
import com.example.biddoc.auth.entity.SysRole;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.entity.SysUserRole;
import com.example.biddoc.auth.mapper.SysRoleMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.mapper.SysUserRoleMapper;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRoleServiceImplTest {

    private final SysUserRoleMapper sysUserRoleMapper = mock(SysUserRoleMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysRoleMapper sysRoleMapper = mock(SysRoleMapper.class);
    private final UserRoleServiceImpl service = new UserRoleServiceImpl(sysUserRoleMapper, sysUserMapper, sysRoleMapper);

    @Test
    void assignRoleRejectsDisabledUser() {
        SysUser disabled = new SysUser();
        disabled.setId(10L);
        disabled.setStatus(0);
        disabled.setDeleted(false);
        when(sysUserMapper.selectById(10L)).thenReturn(disabled);

        UserRoleAssignReqDTO req = new UserRoleAssignReqDTO();
        req.setUserId(10L);
        req.setRoleCode("EMPLOYEE");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assignRole(req));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        verify(sysUserRoleMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listRoleSummariesReturnsPredefinedRolesAndCountsActiveUsers() {
        SysRole superAdmin = role("SUPER_ADMIN", "超级管理员", "拥有系统全部管理权限");
        SysRole employee = role("EMPLOYEE", "普通员工", "基础操作权限");
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(superAdmin, employee));

        SysUserRole firstEmployee = userRole(10L, "EMPLOYEE");
        SysUserRole secondEmployee = userRole(11L, "EMPLOYEE");
        SysUserRole admin = userRole(12L, "SUPER_ADMIN");
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(firstEmployee, secondEmployee, admin));

        SysUser first = user(10L, 1, false);
        SysUser second = user(11L, 0, false);
        SysUser third = user(12L, 1, false);
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(first, second, third));

        var summaries = service.listRoleSummaries();

        assertEquals(4, summaries.size());
        assertEquals("SUPER_ADMIN", summaries.get(0).getCode());
        assertEquals("拥有系统全部管理权限", summaries.get(0).getDescription());
        assertEquals(1L, summaries.get(0).getUserCount());
        assertEquals("EMPLOYEE", summaries.get(3).getCode());
        assertEquals(2L, summaries.get(3).getUserCount());
    }

    private SysRole role(String code, String name, String remark) {
        SysRole role = new SysRole();
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setRemark(remark);
        role.setStatus(1);
        role.setDeleted(false);
        return role;
    }

    private SysUserRole userRole(Long userId, String roleCode) {
        SysUserRole role = new SysUserRole();
        role.setUserId(userId);
        role.setRoleCode(roleCode);
        role.setStatus(1);
        role.setDeleted(false);
        return role;
    }

    private SysUser user(Long id, Integer status, Boolean deleted) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }
}
