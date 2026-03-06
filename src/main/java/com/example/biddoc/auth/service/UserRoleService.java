package com.example.biddoc.auth.service;

import com.example.biddoc.auth.dto.req.UserRoleAssignReqDTO;
import com.example.biddoc.auth.dto.resp.UserRoleRespDTO;

import java.util.List;

public interface UserRoleService {

    /** 为用户分配角色 */
    void assignRole(UserRoleAssignReqDTO req);

    /** 撤销用户的某个角色 */
    void revokeRole(Long userId, String roleCode);

    /** 查询某用户的角色列表（含详情） */
    List<UserRoleRespDTO> listByUserId(Long userId);

    /** 查询某角色下的用户ID列表 */
    List<Long> listUserIdsByRoleCode(String roleCode);

    /** 获取用户当前生效的角色码列表（考虑状态、删除、有效期） */
    List<String> getActiveRoleCodes(Long userId);

    /** 获取用户的主角色码（如无主角色返回第一个生效角色） */
    String getPrimaryRoleCode(Long userId);

    /** 刷新用户在 Sa-Token Session 中的角色缓存 */
    void refreshUserRoleSession(Long userId);
}

