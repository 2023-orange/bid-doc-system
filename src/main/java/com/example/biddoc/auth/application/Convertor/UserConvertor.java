package com.example.biddoc.auth.application.Convertor;

import com.example.biddoc.auth.api.dto.req.UserRegisterReqDTO;
import com.example.biddoc.auth.api.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.domain.enity.SysUser;

public class UserConvertor {

    /**
     * 注册请求 → 用户实体
     */
    public static SysUser toEntity(UserRegisterReqDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword()); // 后续可统一加密
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setDeptId(dto.getDeptId());
        user.setJobLevel(dto.getJobLevel());
        return user;
    }

    /**
     * 用户实体 → 用户详情响应
     */
    public static UserDetailRespDTO toResp(SysUser user) {
        UserDetailRespDTO resp = new UserDetailRespDTO();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setEmail(user.getEmail());
        resp.setMobile(user.getMobile());
        resp.setJobLevel(user.getJobLevel());
        resp.setStatus(user.getStatus());
        return resp;
    }
}

