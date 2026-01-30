package com.example.biddoc.auth.api.controller;

import com.example.biddoc.auth.api.dto.req.LoginReqDTO;
import com.example.biddoc.auth.api.dto.req.UserRegisterReqDTO;
import com.example.biddoc.auth.api.dto.resp.LoginRespDTO;
import com.example.biddoc.auth.api.dto.resp.UserRegisterRespDTO;
import com.example.biddoc.auth.application.Convertor.UserConvertor;
import com.example.biddoc.auth.application.service.AuthService;
import com.example.biddoc.auth.domain.enity.SysUser;
import com.example.biddoc.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserRegisterRespDTO> register(
            @Valid @RequestBody UserRegisterReqDTO req) {

        SysUser user = UserConvertor.toEntity(req);
        Long userId = authService.register(user);

        return ApiResponse.success(
                new UserRegisterRespDTO(userId, "PENDING")
        );
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginRespDTO> login(
            @Valid @RequestBody LoginReqDTO req) {

        SysUser user = authService.login(req.getUsername(), req.getPassword());

        // 示例：此处 token 暂时模拟
        String token = "mock-jwt-token";

        return ApiResponse.success(
                new LoginRespDTO(
                        token,
                        7200,
                        UserConvertor.toResp(user)
                )
        );
    }
}

