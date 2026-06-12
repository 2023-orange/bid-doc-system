CREATE TABLE IF NOT EXISTS bid_project (
    id BIGINT PRIMARY KEY,
    project_no VARCHAR(64) NOT NULL UNIQUE,
    project_name VARCHAR(200) NOT NULL,
    tender_unit VARCHAR(200),
    owner_dept_id BIGINT NOT NULL,
    project_type VARCHAR(64),
    project_stage VARCHAR(64) NOT NULL,
    project_status VARCHAR(64) NOT NULL,
    bid_deadline TIMESTAMPTZ,
    folder_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ DEFAULT now(),
    updated_by VARCHAR(64),
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_bid_project_dept ON bid_project(owner_dept_id);
CREATE INDEX IF NOT EXISTS idx_bid_project_stage ON bid_project(project_stage);
CREATE INDEX IF NOT EXISTS idx_bid_project_status ON bid_project(project_status);

CREATE TABLE IF NOT EXISTS bid_project_member (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    created_by VARCHAR(64),
    deleted BOOLEAN DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_bid_project_member_role
    ON bid_project_member(project_id, user_id, member_role)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS bid_project_no_sequence (
    id BIGINT PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    biz_date DATE NOT NULL,
    current_seq INTEGER NOT NULL,
    UNIQUE(dept_id, biz_date)
);
