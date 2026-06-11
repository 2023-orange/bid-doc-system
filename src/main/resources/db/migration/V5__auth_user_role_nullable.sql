-- Goal-0 follow-up: sys_user.role_id 兼容字段放开非空约束
-- 当前注册链路以 sys_user_role 作为真实多角色关系来源，sys_user.role_id 仅保留给旧数据兼容。
ALTER TABLE sys_user
    ALTER COLUMN role_id DROP NOT NULL;
