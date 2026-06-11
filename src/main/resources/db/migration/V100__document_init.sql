-- Goal-1: document 模块 MVP 初始化
-- 创建 doc_document 和 doc_document_version 表

-- ============================================================
-- doc_document (逻辑文档主表)
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_document (
    id                   BIGINT       PRIMARY KEY,
    folder_id            BIGINT       NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    current_version_no   INTEGER      NOT NULL DEFAULT 1,
    latest_size          BIGINT,
    latest_mime          VARCHAR(128),
    owner_user_id        BIGINT       NOT NULL,
    owner_dept_id        BIGINT,
    status               INTEGER      NOT NULL DEFAULT 1,
    remark               VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           VARCHAR(64),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by           VARCHAR(64),
    deleted              BOOLEAN      NOT NULL DEFAULT false
);

COMMENT ON TABLE  doc_document IS '文档主表(逻辑文档,1:N 物理版本)';
COMMENT ON COLUMN doc_document.folder_id          IS '所属 folder,不允许为 0(根级不能直接挂文档)';
COMMENT ON COLUMN doc_document.name               IS '业务文件名,同 folder 下唯一';
COMMENT ON COLUMN doc_document.current_version_no IS '当前指向的版本号,与 doc_document_version.version_no 对应';
COMMENT ON COLUMN doc_document.latest_size        IS '当前版本字节数(冗余,列表免 JOIN)';
COMMENT ON COLUMN doc_document.latest_mime        IS '当前版本 MIME(冗余)';
COMMENT ON COLUMN doc_document.owner_user_id      IS '首次上传者,后续版本变更不修改此字段';
COMMENT ON COLUMN doc_document.owner_dept_id      IS '首次上传者部门(冗余,过滤用)';
COMMENT ON COLUMN doc_document.status             IS '状态,预留(1=正常)';

CREATE INDEX IF NOT EXISTS idx_doc_document_folder_id
    ON doc_document(folder_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_document_owner_user_id
    ON doc_document(owner_user_id) WHERE deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_document_folder_name_active
    ON doc_document(folder_id, name) WHERE deleted = false;

-- ============================================================
-- doc_document_version (文档版本表)
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_document_version (
    id                     BIGINT       PRIMARY KEY,
    document_id            BIGINT       NOT NULL,
    version_no             INTEGER      NOT NULL,
    storage_key            VARCHAR(512) NOT NULL,
    size                   BIGINT       NOT NULL,
    mime_type              VARCHAR(128),
    original_filename      VARCHAR(255),
    content_hash           VARCHAR(64),
    uploaded_by_user_id    BIGINT       NOT NULL,
    change_log             VARCHAR(500),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(64),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             VARCHAR(64),
    deleted                BOOLEAN      NOT NULL DEFAULT false
);

COMMENT ON TABLE  doc_document_version IS '文档版本表,document_id + version_no 唯一';
COMMENT ON COLUMN doc_document_version.version_no        IS '版本号,从 1 单调递增,同 document_id 下唯一';
COMMENT ON COLUMN doc_document_version.storage_key       IS '存储 key,LocalDisk 实现为相对路径,MinIO 实现为 object key';
COMMENT ON COLUMN doc_document_version.size              IS '字节数,与存储中的物理大小一致';
COMMENT ON COLUMN doc_document_version.mime_type         IS '服务端通过 Apache Tika 探测的 MIME,不信任客户端 Content-Type';
COMMENT ON COLUMN doc_document_version.original_filename IS '上传时原始文件名,用于下载默认 filename';
COMMENT ON COLUMN doc_document_version.content_hash      IS 'SHA-256 十六进制,MVP 计算并存,不做去重(预留二期使用)';

CREATE INDEX IF NOT EXISTS idx_doc_document_version_document_id
    ON doc_document_version(document_id, version_no DESC) WHERE deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_document_version_doc_no_active
    ON doc_document_version(document_id, version_no) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_doc_document_version_hash
    ON doc_document_version(content_hash) WHERE deleted = false AND content_hash IS NOT NULL;
