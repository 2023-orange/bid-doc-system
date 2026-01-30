package com.example.biddoc.auth.application.service.impl;

import com.example.biddoc.auth.application.service.UserService;
import com.example.biddoc.auth.domain.enity.SysUser;
import com.example.biddoc.auth.domain.repository.SysUserRepository;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserRepository userRepository;

    @Override
    public SysUser getById(Long id) {
        SysUser user = userRepository.findById(id);
        AssertUtil.notNull(user, ErrorCode.RESOURCE_NOT_FOUND);
        return user;
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        user.setStatus(status);
        userRepository.update(user);
    }
}

