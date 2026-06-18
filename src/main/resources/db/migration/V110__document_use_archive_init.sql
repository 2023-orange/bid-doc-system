-- 资料授权与项目归档快照数据层初始化

CREATE TABLE IF NOT EXISTS doc_document_use_grant (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    project_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    grantee_id BIGINT NOT NULL,
    approval_instance_id BIGINT,
    grant_type VARCHAR(64) NOT NULL,
    scenario VARCHAR(64),
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64)
);

COMMENT ON TABLE doc_document_use_grant IS '资料使用授权记录表，记录项目场景下某用户对指定资料版本的使用权限';
COMMENT ON COLUMN doc_document_use_grant.document_id IS '被授权使用的资料 ID';
COMMENT ON COLUMN doc_document_use_grant.version_no IS 'Granted document version number';
COMMENT ON COLUMN doc_document_use_grant.project_id IS '授权关联的项目 ID';
COMMENT ON COLUMN doc_document_use_grant.applicant_id IS '发起授权申请的用户 ID';
COMMENT ON COLUMN doc_document_use_grant.grantee_id IS '获得资料使用权的用户 ID';
COMMENT ON COLUMN doc_document_use_grant.approval_instance_id IS '授权审批实例 ID';
COMMENT ON COLUMN doc_document_use_grant.grant_type IS '授权类型，例如查看、下载或归档使用';
COMMENT ON COLUMN doc_document_use_grant.scenario IS '授权使用场景';
COMMENT ON COLUMN doc_document_use_grant.valid_from IS '授权生效时间';
COMMENT ON COLUMN doc_document_use_grant.valid_until IS '授权失效时间';
COMMENT ON COLUMN doc_document_use_grant.status IS '授权状态，例如 PENDING、ACTIVE、REVOKED、EXPIRED';
COMMENT ON COLUMN doc_document_use_grant.reason IS 'Grant request or change reason';

CREATE INDEX IF NOT EXISTS idx_doc_use_grant_document
    ON doc_document_use_grant(document_id, version_no)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_use_grant_project_status
    ON doc_document_use_grant(project_id, status)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_use_grant_grantee_valid
    ON doc_document_use_grant(grantee_id, valid_until)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_use_grant_approval_instance
    ON doc_document_use_grant(approval_instance_id)
    WHERE deleted = false AND approval_instance_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS bid_project_archive_record (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    archive_no VARCHAR(64) NOT NULL,
    archive_status VARCHAR(32) NOT NULL,
    archived_at TIMESTAMPTZ,
    archived_by BIGINT,
    archive_reason VARCHAR(500),
    checklist_total INTEGER NOT NULL DEFAULT 0,
    checklist_complete INTEGER NOT NULL DEFAULT 0,
    document_total INTEGER NOT NULL DEFAULT 0,
    snapshot_hash VARCHAR(128),
    remark VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64)
);

COMMENT ON TABLE bid_project_archive_record IS '项目归档记录表，保存一次归档操作的汇总状态与快照校验信息';
COMMENT ON COLUMN bid_project_archive_record.project_id IS '归档所属项目 ID';
COMMENT ON COLUMN bid_project_archive_record.archive_no IS 'Project archive batch number';
COMMENT ON COLUMN bid_project_archive_record.archive_status IS '归档状态，例如 DRAFT、ARCHIVED、FAILED';
COMMENT ON COLUMN bid_project_archive_record.archived_at IS '归档完成时间';
COMMENT ON COLUMN bid_project_archive_record.archived_by IS '执行归档的用户 ID';
COMMENT ON COLUMN bid_project_archive_record.archive_reason IS '归档原因';
COMMENT ON COLUMN bid_project_archive_record.checklist_total IS '归档时清单项总数';
COMMENT ON COLUMN bid_project_archive_record.checklist_complete IS 'Completed checklist item count at archive time';
COMMENT ON COLUMN bid_project_archive_record.document_total IS 'Document snapshot count at archive time';
COMMENT ON COLUMN bid_project_archive_record.snapshot_hash IS 'Archive snapshot hash for later consistency checks';

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_archive_record_no_active
    ON bid_project_archive_record(project_id, archive_no)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_project_archive_record_project_status
    ON bid_project_archive_record(project_id, archive_status)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_project_archive_record_archived_at
    ON bid_project_archive_record(archived_at DESC)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS bid_project_archive_checklist_snapshot (
    id BIGINT PRIMARY KEY,
    archive_record_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    checklist_item_id BIGINT,
    template_item_id BIGINT,
    item_name VARCHAR(200) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT false,
    item_status VARCHAR(64),
    bound_document_count INTEGER NOT NULL DEFAULT 0,
    snapshot_json TEXT,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64)
);

COMMENT ON TABLE bid_project_archive_checklist_snapshot IS 'Project archive checklist snapshot table';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.archive_record_id IS '归档记录 ID';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.project_id IS '归档所属项目 ID';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.checklist_item_id IS '归档时对应的项目清单项 ID';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.template_item_id IS '清单来源模板项 ID';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.item_name IS '归档时清单项名称快照';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.required_flag IS 'Whether the checklist item was required at archive time';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.item_status IS 'Checklist item status at archive time';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.bound_document_count IS 'Bound document count at archive time';
COMMENT ON COLUMN bid_project_archive_checklist_snapshot.snapshot_json IS 'Checklist item extended snapshot JSON';

CREATE INDEX IF NOT EXISTS idx_archive_checklist_snapshot_record
    ON bid_project_archive_checklist_snapshot(archive_record_id)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_archive_checklist_snapshot_project
    ON bid_project_archive_checklist_snapshot(project_id, checklist_item_id)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS bid_project_archive_document_snapshot (
    id BIGINT PRIMARY KEY,
    archive_record_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    checklist_item_id BIGINT,
    document_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_status VARCHAR(64),
    version_status VARCHAR(64),
    expire_at TIMESTAMPTZ,
    storage_type VARCHAR(64),
    file_size BIGINT,
    snapshot_json TEXT,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64)
);

COMMENT ON TABLE bid_project_archive_document_snapshot IS '项目归档文档快照表，固化归档时绑定文档及版本的关键元数据';
COMMENT ON COLUMN bid_project_archive_document_snapshot.archive_record_id IS '归档记录 ID';
COMMENT ON COLUMN bid_project_archive_document_snapshot.project_id IS '归档所属项目 ID';
COMMENT ON COLUMN bid_project_archive_document_snapshot.checklist_item_id IS '归档时关联的项目清单项 ID';
COMMENT ON COLUMN bid_project_archive_document_snapshot.document_id IS '归档时绑定的资料 ID';
COMMENT ON COLUMN bid_project_archive_document_snapshot.version_no IS 'Document version number at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.document_name IS 'Document name snapshot at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.document_status IS 'Document business status at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.version_status IS 'Document version approval status at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.expire_at IS 'Document expiration time at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.storage_type IS 'Document storage type at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.file_size IS 'Document version file size at archive time';
COMMENT ON COLUMN bid_project_archive_document_snapshot.snapshot_json IS 'Document extended snapshot JSON';

CREATE INDEX IF NOT EXISTS idx_archive_document_snapshot_record
    ON bid_project_archive_document_snapshot(archive_record_id)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_archive_document_snapshot_project
    ON bid_project_archive_document_snapshot(project_id, checklist_item_id)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_archive_document_snapshot_document
    ON bid_project_archive_document_snapshot(document_id, version_no)
    WHERE deleted = false;
