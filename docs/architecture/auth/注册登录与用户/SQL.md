````
```
CREATE TABLE "public"."sys_department" (
  "id" int8 NOT NULL DEFAULT nextval('sys_department_id_seq'::regclass),
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" int8,
  "level" int2 NOT NULL,
  "manager_user_id" int8,
  "status" int2 NOT NULL DEFAULT 1,
  "deleted" bool NOT NULL DEFAULT false,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "created_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "extension_data" jsonb,
  CONSTRAINT "sys_department_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "ck_department_level" CHECK (level = ANY (ARRAY[1, 2]))
)
;

ALTER TABLE "public"."sys_department" 
  OWNER TO "postgres";

CREATE INDEX "idx_department_level" ON "public"."sys_department" USING btree (
  "level" "pg_catalog"."int2_ops" ASC NULLS LAST
);

CREATE INDEX "idx_department_parent" ON "public"."sys_department" USING btree (
  "parent_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

CREATE INDEX "idx_department_status" ON "public"."sys_department" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);

COMMENT ON COLUMN "public"."sys_department"."id" IS '部门ID（主键）';

COMMENT ON COLUMN "public"."sys_department"."name" IS '部门名称';

COMMENT ON COLUMN "public"."sys_department"."parent_id" IS '父部门ID（一级部门为NULL）';

COMMENT ON COLUMN "public"."sys_department"."level" IS '部门层级：1-一级部门，2-二级部门';

COMMENT ON COLUMN "public"."sys_department"."manager_user_id" IS '部门经理用户ID';

COMMENT ON COLUMN "public"."sys_department"."status" IS '状态：1-启用，0-停用';

COMMENT ON COLUMN "public"."sys_department"."deleted" IS '是否删除：true-已删除';

COMMENT ON COLUMN "public"."sys_department"."created_at" IS '创建时间';

COMMENT ON COLUMN "public"."sys_department"."created_by" IS '创建人';

COMMENT ON COLUMN "public"."sys_department"."updated_at" IS '更新时间';

COMMENT ON COLUMN "public"."sys_department"."updated_by" IS '更新人';

COMMENT ON COLUMN "public"."sys_department"."remark" IS '备注';

COMMENT ON COLUMN "public"."sys_department"."extension_data" IS '扩展字段（JSON）';

COMMENT ON TABLE "public"."sys_department" IS '部门表（仅支持两级组织结构）';
```

CREATE TABLE "public"."sys_role" (
  "id" int8 NOT NULL DEFAULT nextval('sys_role_id_seq'::regclass),
  "role_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "role_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL DEFAULT 1,
  "deleted" bool NOT NULL DEFAULT false,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "created_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."sys_role" 
  OWNER TO "postgres";

CREATE UNIQUE INDEX "uk_role_code" ON "public"."sys_role" USING btree (
  "role_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

COMMENT ON COLUMN "public"."sys_role"."id" IS '角色ID';

COMMENT ON COLUMN "public"."sys_role"."role_code" IS '角色编码（ADMIN / USER）';

COMMENT ON COLUMN "public"."sys_role"."role_name" IS '角色名称';

COMMENT ON COLUMN "public"."sys_role"."status" IS '状态：1-启用，0-停用';

COMMENT ON COLUMN "public"."sys_role"."deleted" IS '是否删除';

COMMENT ON COLUMN "public"."sys_role"."created_at" IS '创建时间';

COMMENT ON COLUMN "public"."sys_role"."created_by" IS '创建人';

COMMENT ON COLUMN "public"."sys_role"."updated_at" IS '更新时间';

COMMENT ON COLUMN "public"."sys_role"."updated_by" IS '更新人';

COMMENT ON COLUMN "public"."sys_role"."remark" IS '备注';

COMMENT ON TABLE "public"."sys_role" IS '系统角色表（RBAC）';

CREATE TABLE "public"."sys_user" (
  "id" int8 NOT NULL DEFAULT nextval('sys_user_id_seq'::regclass),
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "real_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "email" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "mobile" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_id" int8 NOT NULL,
  "job_level" int2 NOT NULL,
  "role_id" int8 NOT NULL,
  "status" int2 NOT NULL DEFAULT 1,
  "deleted" bool NOT NULL DEFAULT false,
  "last_login_time" timestamptz(6),
  "login_count" int4 NOT NULL DEFAULT 0,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "created_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_by" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "extension_data" jsonb,
  CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "fk_user_dept" FOREIGN KEY ("dept_id") REFERENCES "public"."sys_department" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT "ck_user_job_level" CHECK (job_level = ANY (ARRAY[1, 2, 3]))
)
;

ALTER TABLE "public"."sys_user" 
  OWNER TO "postgres";

COMMENT ON COLUMN "public"."sys_user"."id" IS '用户ID';

COMMENT ON COLUMN "public"."sys_user"."username" IS '登录用户名';

COMMENT ON COLUMN "public"."sys_user"."password" IS '登录密码';

COMMENT ON COLUMN "public"."sys_user"."real_name" IS '真实姓名';

COMMENT ON COLUMN "public"."sys_user"."email" IS '邮箱';

COMMENT ON COLUMN "public"."sys_user"."mobile" IS '手机号';

COMMENT ON COLUMN "public"."sys_user"."dept_id" IS '所属二级部门ID';

COMMENT ON COLUMN "public"."sys_user"."job_level" IS '职级：1-经理，2-主管，3-普通员工';

COMMENT ON COLUMN "public"."sys_user"."role_id" IS '系统角色ID';

COMMENT ON COLUMN "public"."sys_user"."status" IS '状态：1-启用，0-禁用';

COMMENT ON COLUMN "public"."sys_user"."deleted" IS '是否删除';

COMMENT ON COLUMN "public"."sys_user"."last_login_time" IS '最后登录时间';

COMMENT ON COLUMN "public"."sys_user"."login_count" IS '登录次数';

COMMENT ON COLUMN "public"."sys_user"."created_at" IS '创建时间';

COMMENT ON COLUMN "public"."sys_user"."created_by" IS '创建人';

COMMENT ON COLUMN "public"."sys_user"."updated_at" IS '更新时间';

COMMENT ON COLUMN "public"."sys_user"."updated_by" IS '更新人';

COMMENT ON COLUMN "public"."sys_user"."remark" IS '备注';

COMMENT ON COLUMN "public"."sys_user"."extension_data" IS '扩展字段（JSON）';

COMMENT ON TABLE "public"."sys_user" IS '系统用户表';
````

