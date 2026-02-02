package com.example.auth.controller;

import com.example.biddoc.auth.api.controller.AuthController;
import com.example.biddoc.auth.application.service.AuthService;
import com.example.biddoc.auth.domain.enity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void testLoginSuccess() throws Exception {
        // 测试用例ID: TEST-AUTH-CTRL-001

        // 替换为直接构造：
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        when(authService.login(any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username":"admin",
                      "password":"12345678"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    void testLoginParamInvalid() throws Exception {
        // 测试用例ID: TEST-AUTH-CTRL-002

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "username": "" }
                """))
                .andExpect(status().isBadRequest());
    }
}

