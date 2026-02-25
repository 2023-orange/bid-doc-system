package com.example.auth.service;

import com.example.biddoc.auth.service.AuthService;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.repository.SysUserRepository;
import com.example.biddoc.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // 测试用例ID: TEST-AUTH-UNIT-001
    // 测试用例名称: 用户注册成功
    // 测试目标: 验证 register 方法正常流程
    // 测试类型: 单元测试

    @InjectMocks
    private AuthService authService;

    @Mock
    private SysUserRepository userRepository;

    @Test
    void testRegisterSuccess() {
        // 替换为直接构造：
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");

        when(userRepository.save(any())).thenReturn(user);

        Long userId = authService.register(user);

        assertNotNull(userId);
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void testRegisterDuplicateUsername() {
        // 测试用例ID: TEST-AUTH-UNIT-002
        // 测试目标: 重复用户名抛异常

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> {
            authService.register(user);
        });
    }
}

