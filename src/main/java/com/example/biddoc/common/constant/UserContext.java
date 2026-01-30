package com.example.biddoc.common.constant;

import lombok.AllArgsConstructor;
import lombok.Data;

public class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void set(UserInfo user) {
        HOLDER.set(user);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;

        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }
}
