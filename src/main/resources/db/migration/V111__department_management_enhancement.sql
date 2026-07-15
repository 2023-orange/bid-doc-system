-- 部门管理增强：支持多级组织树、人工排序和更清晰的管理字段契约。

ALTER TABLE sys_department
    ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;

ALTER TABLE sys_department
    DROP CONSTRAINT IF EXISTS ck_department_level;

ALTER TABLE sys_department
    ADD CONSTRAINT ck_department_level CHECK (level BETWEEN 1 AND 5);

CREATE INDEX IF NOT EXISTS idx_department_sort_order
    ON sys_department (parent_id, sort_order, id);

COMMENT ON COLUMN sys_department.parent_id IS '父部门ID，一级部门为NULL';
COMMENT ON COLUMN sys_department.level IS '部门层级：1-5，一级部门parent_id为NULL';
COMMENT ON COLUMN sys_department.sort_order IS '同级部门排序值，越小越靠前';
COMMENT ON TABLE sys_department IS '部门表（最多支持五级组织结构）';
