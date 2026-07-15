ALTER TABLE bid_project
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

COMMENT ON COLUMN bid_project.remark IS '项目备注，用于项目工作台基础信息展示和编辑';
