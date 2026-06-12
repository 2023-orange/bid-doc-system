ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS document_no VARCHAR(64);
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS document_status VARCHAR(64) DEFAULT 'INCOMPLETE';
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS business_category VARCHAR(64);
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS tender_structure_category VARCHAR(64);
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS sensitive_level VARCHAR(64);
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS source_type VARCHAR(64);
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS effective_date TIMESTAMPTZ;
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS expire_date TIMESTAMPTZ;
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS has_expire_date BOOLEAN DEFAULT false;
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS metadata_completed BOOLEAN DEFAULT false;
ALTER TABLE doc_document ADD COLUMN IF NOT EXISTS invalid_reason TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_document_no ON doc_document(document_no) WHERE document_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_doc_document_status ON doc_document(document_status);
