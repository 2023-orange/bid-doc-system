ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS biz_module VARCHAR(64);
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS biz_type VARCHAR(64);
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS biz_id BIGINT;
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS scenario VARCHAR(64);
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS finished_at TIMESTAMPTZ;
ALTER TABLE wf_approval_instance ALTER COLUMN document_id DROP NOT NULL;
ALTER TABLE wf_approval_task ALTER COLUMN document_id DROP NOT NULL;

UPDATE wf_approval_instance
SET biz_module = COALESCE(biz_module, 'DOCUMENT'),
    biz_type = COALESCE(biz_type, 'DOCUMENT'),
    biz_id = COALESCE(biz_id, document_id),
    scenario = COALESCE(scenario, 'DOCUMENT_APPROVAL')
WHERE document_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_wf_approval_instance_biz
    ON wf_approval_instance(biz_module, biz_type, biz_id, submitted_at DESC)
    WHERE deleted = false;
