package com.example.biddoc.auth.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.auth.convertor.UserConvertor;
import com.example.biddoc.auth.dto.req.LoginReqDTO;
import com.example.biddoc.auth.dto.req.UserRegisterReqDTO;
import com.example.biddoc.auth.dto.resp.LoginRespDTO;
import com.example.biddoc.auth.dto.resp.UserRegisterRespDTO;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.service.AuthService;
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
    public ApiResponse<LoginRespDTO> login(@Valid @RequestBody LoginReqDTO req) {

        // 1. 调用 Service 登录并获取令牌
        SaTokenInfo tokenInfo = authService.login(req.getUsername(), req.getPassword());

        // 2. 从 Session 缓存提取用户信息
        SysUser user = (SysUser) StpUtil.getSession().get("user");

        // 3. 组装响应
        return ApiResponse.success(
                new LoginRespDTO(
                        tokenInfo.getTokenValue(),
                        tokenInfo.getTokenTimeout(), // 动态返回剩余过期时间
                        UserConvertor.toResp(user)
                )
        );
    }
}

