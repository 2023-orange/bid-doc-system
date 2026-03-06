package com.example.biddoc.common.constant;

import com.example.biddoc.auth.constant.RoleCodeEnum;
import lombok.Data;

import java.util.Collections;
import java.util.List;

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
    public static class UserInfo {
        private Long userId;
        private String username;
        private List<String> roleCodes;
        private Long deptId;

        public UserInfo(Long userId, String username, List<String> roleCodes, Long deptId) {
            this.userId = userId;
            this.username = username;
            this.roleCodes = roleCodes != null ? roleCodes : Collections.emptyList();
            this.deptId = deptId;
        }

        /** 向下兼容旧代码：3 参数构造器 */
        @Deprecated
        public UserInfo(Long userId, String username, String role) {
            this(userId, username, role != null ? List.of(role) : Collections.emptyList(), null);
        }

        /** 向下兼容：获取首个角色码 */
        public String getRole() {
            return roleCodes != null && !roleCodes.isEmpty() ? roleCodes.get(0) : null;
        }

        public boolean hasRole(String roleCode) {
            return roleCodes != null && roleCodes.contains(roleCode);
        }

        public boolean hasAnyRole(String... codes) {
            if (roleCodes == null) return false;
            for (String code : codes) {
                if (roleCodes.contains(code)) return true;
            }
            return false;
        }

        public boolean isAdmin() {
            return hasRole(RoleCodeEnum.SUPER_ADMIN.getCode());
        }

        public boolean isSuperAdmin() {
            return hasRole(RoleCodeEnum.SUPER_ADMIN.getCode());
        }

        public boolean isDeptManager() {
            return hasRole(RoleCodeEnum.DEPT_MANAGER.getCode());
        }

        public boolean isFolderAdmin() {
            return hasRole(RoleCodeEnum.FOLDER_ADMIN.getCode());
        }
    }
}
