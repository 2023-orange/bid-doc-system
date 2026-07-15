package com.example.biddoc.auth.controller;

import com.example.biddoc.auth.service.UserService;
import com.example.biddoc.auth.dto.req.UserCreateReqDTO;
import com.example.biddoc.auth.dto.req.UserPasswordResetReqDTO;
import com.example.biddoc.auth.dto.req.UserStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.req.UserUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.dto.resp.UserManageRespDTO;
import com.example.biddoc.auth.dto.resp.UserOptionRespDTO;
import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户管理列表
     */
    @GetMapping
    public ApiResponse<PageResponse<UserManageRespDTO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(userService.pageUsers(keyword, deptId, status, roleCode, page, size));
    }

    /**
     * 创建后台用户
     */
    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody UserCreateReqDTO req) {
        return ApiResponse.success(Map.of("id", userService.createUser(req)));
    }

    /**
     * 更新后台用户基础信息与角色
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateReqDTO req) {
        userService.updateUser(id, req);
        return ApiResponse.success();
    }

    /**
     * 重置用户密码
     */
    @PatchMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody UserPasswordResetReqDTO req) {
        userService.resetPassword(id, req);
        return ApiResponse.success();
    }

    /**
     * 查询用户选择器选项
     */
    @GetMapping("/options")
    public ApiResponse<List<UserOptionRespDTO>> options(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(userService.listOptions(keyword, deptId, status, size));
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserDetailRespDTO> get(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 启用 / 禁用用户
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateReqDTO req) {

        userService.changeStatus(id, req.getStatus());
        return ApiResponse.success();
    }
}

