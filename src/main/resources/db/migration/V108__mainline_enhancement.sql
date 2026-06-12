ALTER TABLE doc_document_version ADD COLUMN IF NOT EXISTS approval_status VARCHAR(32) DEFAULT 'APPROVED';
ALTER TABLE doc_document_version ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE doc_document_version ADD COLUMN IF NOT EXISTS rejected_reason VARCHAR(500);

ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS version_no INTEGER;

CREATE INDEX IF NOT EXISTS idx_doc_document_version_approval
    ON doc_document_version(document_id, approval_status)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_bid_project_checklist_item_status
    ON bid_project_checklist_item(project_id, status)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_wf_approval_instance_biz_v108
    ON wf_approval_instance(biz_module, biz_type, biz_id, submitted_at DESC)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_document_status_expire
    ON doc_document(document_status, expire_date)
    WHERE deleted = false;
