package com.example.biddoc.auth.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.auth.convertor.UserConvertor;
import com.example.biddoc.auth.dto.req.LoginReqDTO;
import com.example.biddoc.auth.dto.req.UserRegisterReqDTO;
import com.example.biddoc.auth.dto.resp.LoginRespDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.dto.resp.UserRegisterRespDTO;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.service.AuthService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.util.AssertUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserRegisterRespDTO> register(
            @Valid @RequestBody UserRegisterReqDTO req) {
        SysUser user = UserConvertor.toEntity(req);
        Long userId = authService.register(user);
        return ApiResponse.success(new UserRegisterRespDTO(userId, "PENDING"));
    }

    @PostMapping("/login")
    public ApiResponse<LoginRespDTO> login(@Valid @RequestBody LoginReqDTO req) {
        SaTokenInfo tokenInfo = authService.login(req.getUsername(), req.getPassword());

        SysUser user = (SysUser) StpUtil.getSession().get("user");

        @SuppressWarnings("unchecked")
        List<String> roleCodes = (List<String>) StpUtil.getSession().get("roleCodes");
        if (roleCodes == null) {
            roleCodes = Collections.emptyList();
        }

        UserDetailRespDTO userDetail = UserConvertor.toResp(user);
        userDetail.setRoleCodes(roleCodes);

        return ApiResponse.success(new LoginRespDTO(
                tokenInfo.getTokenValue(),
                tokenInfo.getTokenTimeout(),
                userDetail,
                roleCodes
        ));
    }

    /**
     * 获取当前登录用户信息（含角色码）
     */
    @GetMapping("/me")
    public ApiResponse<UserDetailRespDTO> getCurrentUser() {
        SysUser user = (SysUser) StpUtil.getSession().get("user");
        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);

        @SuppressWarnings("unchecked")
        List<String> roleCodes = (List<String>) StpUtil.getSession().get("roleCodes");

        UserDetailRespDTO resp = UserConvertor.toResp(user);
        resp.setRoleCodes(roleCodes);
        return ApiResponse.success(resp);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success();
    }
}

