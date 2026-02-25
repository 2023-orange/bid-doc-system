package com.example.biddoc.auth.service.impl;

import com.example.biddoc.auth.convertor.UserConvertor;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.service.UserService;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetailRespDTO getById(Long id) {

        SysUser user = sysUserMapper.selectById(id);
        AssertUtil.notNull(user, ErrorCode.RESOURCE_NOT_FOUND);

        return UserConvertor.toResp(user);
    }

    @Override
    public void changeStatus(Long userId, Integer status) {

        SysUser user = sysUserMapper.selectById(userId);
        AssertUtil.notNull(user, ErrorCode.RESOURCE_NOT_FOUND);

        user.setStatus(status);
        sysUserMapper.updateById(user);
    }
}