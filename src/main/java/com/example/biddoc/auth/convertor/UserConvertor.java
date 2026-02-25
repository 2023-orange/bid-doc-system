package com.example.biddoc.auth.convertor;

import com.example.biddoc.auth.dto.req.UserRegisterReqDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.entity.SysUser;

public class UserConvertor {

    /**
     * 注册请求 → 用户实体
     */
    public static SysUser toEntity(UserRegisterReqDTO dto) {

        if (dto == null) {
            return null;
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword()); // 后续统一加密
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

        if (user == null) {
            return null;
        }

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