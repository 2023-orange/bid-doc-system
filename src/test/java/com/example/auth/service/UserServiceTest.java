package com.example.auth.service;

import com.example.biddoc.auth.service.UserService;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private SysUserRepository userRepository;

    @Test
    void testChangeStatusSuccess() {
        // 测试用例ID: TEST-USER-UNIT-001
        SysUser user = TestDataFactory.newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.changeStatus(1L, 0);

        assertEquals(0, user.getStatus());
    }

    @Test
    void testChangeStatusUserNotFound() {
        // 测试用例ID: TEST-USER-UNIT-002
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            userService.changeStatus(99L, 1);
        });
    }
}

