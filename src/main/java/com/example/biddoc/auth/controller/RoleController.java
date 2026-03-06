package com.example.biddoc.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.biddoc.auth.dto.req.UserRoleAssignReqDTO;
import com.example.biddoc.auth.dto.resp.UserRoleRespDTO;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserRoleService userRoleService;

    /**
     * 为用户分配角色（仅 SUPER_ADMIN 可操作）
     */
    @SaCheckRole("SUPER_ADMIN")
    @PostMapping("/assign")
    public ApiResponse<Void> assignRole(@Valid @RequestBody UserRoleAssignReqDTO req) {
        userRoleService.assignRole(req);
        return ApiResponse.success();
    }

    /**
     * 撤销用户角色（仅 SUPER_ADMIN 可操作）
     */
    @SaCheckRole("SUPER_ADMIN")
    @DeleteMapping("/revoke")
    public ApiResponse<Void> revokeRole(@RequestParam Long userId,
                                        @RequestParam String roleCode) {
        userRoleService.revokeRole(userId, roleCode);
        return ApiResponse.success();
    }

    /**
     * 查询用户的角色列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<UserRoleRespDTO>> listByUserId(@PathVariable Long userId) {
        return ApiResponse.success(userRoleService.listByUserId(userId));
    }

    /**
     * 查询某角色下的用户ID列表
     */
    @SaCheckRole("SUPER_ADMIN")
    @GetMapping("/code/{roleCode}/users")
    public ApiResponse<List<Long>> listUserIds(@PathVariable String roleCode) {
        return ApiResponse.success(userRoleService.listUserIdsByRoleCode(roleCode));
    }
}

