package com.example.biddoc.auth.application.service.impl;

import com.example.biddoc.auth.application.service.AuthService;
import com.example.biddoc.auth.domain.enity.SysUser;
import com.example.biddoc.auth.domain.repository.SysUserRepository;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;

    @Override
    public Long register(SysUser user) {

        AssertUtil.isTrue(
                userRepository.findByUsername(user.getUsername()) == null,
                ErrorCode.RESOURCE_CONFLICT
        );

        user.setStatus(0); // PENDING
        user.setDeleted(false);
        userRepository.save(user);
        return user.getId();
    }

    @Override
    public SysUser login(String username, String password) {

        SysUser user = userRepository.findByUsername(username);
        AssertUtil.notNull(user, ErrorCode.AUTH_FAILED);

        AssertUtil.isTrue(
                Objects.equals(user.getPassword(), password),
                ErrorCode.AUTH_FAILED
        );

        AssertUtil.isTrue(
                user.getStatus() == 1,
                ErrorCode.ACCOUNT_DISABLED
        );

        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.update(user);

        return user;
    }
}
