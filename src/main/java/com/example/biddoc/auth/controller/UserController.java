package com.example.biddoc.auth.controller;

import com.example.biddoc.auth.service.UserService;
import com.example.biddoc.auth.dto.req.UserStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

