CREATE TABLE IF NOT EXISTS bid_checklist_template (
    id BIGINT PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,
    project_type VARCHAR(64),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ DEFAULT now(),
    updated_by VARCHAR(64),
    deleted BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS bid_checklist_template_item (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    description TEXT,
    required BOOLEAN DEFAULT true,
    business_category VARCHAR(64),
    tender_structure_category VARCHAR(64),
    suggested_sensitive_level VARCHAR(64),
    allowed_source VARCHAR(64),
    allowed_file_types VARCHAR(500),
    min_count INTEGER DEFAULT 1,
    max_count INTEGER,
    sort_order INTEGER DEFAULT 0,
    deleted BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS bid_project_checklist_item (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    template_item_id BIGINT,
    item_name VARCHAR(200) NOT NULL,
    description TEXT,
    required BOOLEAN DEFAULT true,
    business_category VARCHAR(64),
    tender_structure_category VARCHAR(64),
    sensitive_level VARCHAR(64),
    allowed_source VARCHAR(64),
    allowed_file_types VARCHAR(500),
    min_count INTEGER DEFAULT 1,
    max_count INTEGER,
    deadline TIMESTAMPTZ,
    owner_user_id BIGINT,
    status VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    deleted BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS bid_project_checklist_document (
    id BIGINT PRIMARY KEY,
    checklist_item_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    version_no INTEGER,
    bind_type VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT now(),
    created_by VARCHAR(64),
    deleted BOOLEAN DEFAULT false
);
