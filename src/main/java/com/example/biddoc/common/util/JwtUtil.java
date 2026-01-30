package com.example.biddoc.common.util;

import com.example.biddoc.common.constant.UserContext;

public class JwtUtil {

    public static UserContext.UserInfo parse(String token) {
        // 后续接入 jjwt / auth0
        return new UserContext.UserInfo(1L, "admin", "ADMIN");
    }
}
