-- ============================================================
-- Auth模块增量改造：人员-角色关联表 + 角色初始化数据
-- 数据库：PostgreSQL
-- 执行顺序：先执行此脚本，再启动应用
-- ============================================================

-- 1. 创建序列
CREATE SEQUENCE IF NOT EXISTS sys_user_role_id_seq;

-- 2. 创建用户-角色关联表
CREATE TABLE "public"."sys_user_role" (
    "id"                    int8            NOT NULL DEFAULT nextval('sys_user_role_id_seq'::regclass),
    "user_id"               int8            NOT NULL,
    "role_code"             varchar(50)     NOT NULL,
    "is_primary"            bool            NOT NULL DEFAULT false,
    "status"                int2            NOT NULL DEFAULT 1,
    "effective_start_time"  timestamptz(6),
    "effective_end_time"    timestamptz(6),
    "source_type"           int2            NOT NULL DEFAULT 1,
    "deleted"               bool            NOT NULL DEFAULT false,
    "created_at"            timestamptz(6)  NOT NULL DEFAULT now(),
    "created_by"            varchar(50)     NOT NULL,
    "updated_at"            timestamptz(6)  NOT NULL DEFAULT now(),
    "updated_by"            varchar(50)     NOT NULL,
    CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "public"."sys_user_role" OWNER TO "postgres";

-- 3. 唯一约束（部分索引，仅约束未删除的记录）
CREATE UNIQUE INDEX "uk_user_role_active" ON "public"."sys_user_role" ("user_id", "role_code") WHERE deleted = false;

-- 4. 普通索引
CREATE INDEX "idx_user_role_user_id"   ON "public"."sys_user_role" ("user_id");
CREATE INDEX "idx_user_role_role_code" ON "public"."sys_user_role" ("role_code");
CREATE INDEX "idx_user_role_status"    ON "public"."sys_user_role" ("status");

-- 5. 字段注释
COMMENT ON TABLE  "public"."sys_user_role"                          IS '用户-角色关联表（支持一人多角色）';
COMMENT ON COLUMN "public"."sys_user_role"."id"                     IS '主键ID';
COMMENT ON COLUMN "public"."sys_user_role"."user_id"                IS '用户ID（关联 sys_user.id）';
COMMENT ON COLUMN "public"."sys_user_role"."role_code"              IS '角色编码（关联 sys_role.role_code，如 SUPER_ADMIN / FOLDER_ADMIN / DEPT_MANAGER / EMPLOYEE）';
COMMENT ON COLUMN "public"."sys_user_role"."is_primary"             IS '是否主角色：true-主角色，false-非主角色（每人最多一个主角色）';
COMMENT ON COLUMN "public"."sys_user_role"."status"                 IS '状态：1-启用，0-停用';
COMMENT ON COLUMN "public"."sys_user_role"."effective_start_time"   IS '生效开始时间（NULL 表示立即生效）';
COMMENT ON COLUMN "public"."sys_user_role"."effective_end_time"     IS '生效结束时间（NULL 表示永久有效）';
COMMENT ON COLUMN "public"."sys_user_role"."source_type"            IS '分配来源：1-手动分配，2-系统初始化，3-同步导入';
COMMENT ON COLUMN "public"."sys_user_role"."deleted"                IS '是否删除：true-已删除';
COMMENT ON COLUMN "public"."sys_user_role"."created_at"             IS '创建时间';
COMMENT ON COLUMN "public"."sys_user_role"."created_by"             IS '创建人';
COMMENT ON COLUMN "public"."sys_user_role"."updated_at"             IS '更新时间';
COMMENT ON COLUMN "public"."sys_user_role"."updated_by"             IS '更新人';

-- ============================================================
-- 6. 初始化四种标准角色到 sys_role 表
--    如果已有同 role_code 的数据，先清理再插入
-- ============================================================
DELETE FROM "public"."sys_role"
 WHERE "role_code" IN ('SUPER_ADMIN', 'FOLDER_ADMIN', 'DEPT_MANAGER', 'EMPLOYEE');

INSERT INTO "public"."sys_role" ("role_code", "role_name", "status", "deleted", "created_by", "updated_by", "remark")
VALUES
    ('SUPER_ADMIN',  '超级管理员',    1, false, 'system', 'system', '拥有系统全部管理权限'),
    ('FOLDER_ADMIN', '文件夹管理员',  1, false, 'system', 'system', '负责文件夹授权与管理'),
    ('DEPT_MANAGER', '部门经理',      1, false, 'system', 'system', '负责本部门人员与业务管理'),
    ('EMPLOYEE',     '普通员工',      1, false, 'system', 'system', '基础操作权限');

-- ============================================================
-- 7. 数据迁移：将 sys_user.role_id 已有数据迁移到 sys_user_role
--    根据旧 role_id 对应的 role_code 创建关联记录
--    此步骤仅在有历史数据时执行，可根据实际情况调整
-- ============================================================
INSERT INTO "public"."sys_user_role" ("user_id", "role_code", "is_primary", "status", "source_type", "deleted", "created_by", "updated_by")
SELECT
    u."id",
    r."role_code",
    true,
    1,
    2,
    false,
    'system',
    'system'
FROM "public"."sys_user" u
JOIN "public"."sys_role" r ON u."role_id" = r."id"
WHERE u."deleted" = false
  AND r."deleted" = false
  AND NOT EXISTS (
      SELECT 1 FROM "public"."sys_user_role" ur
       WHERE ur."user_id" = u."id"
         AND ur."role_code" = r."role_code"
         AND ur."deleted" = false
  );
