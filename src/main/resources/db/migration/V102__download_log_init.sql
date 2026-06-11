-- Goal-3: 文档统计 - 下载记录表
-- 记录文档下载行为，用于统计热门文档和下载次数

CREATE TABLE IF NOT EXISTS doc_download_log (
    id                  BIGINT          PRIMARY KEY,
    document_id         BIGINT          NOT NULL,
    version_no          INTEGER         NOT NULL,
    user_id             BIGINT          NOT NULL,
    download_time       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address          VARCHAR(50),
    user_agent          VARCHAR(500),
    deleted             BOOLEAN         NOT NULL DEFAULT false,

    CONSTRAINT fk_download_log_document FOREIGN KEY (document_id)
        REFERENCES doc_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_download_log_user FOREIGN KEY (user_id)
        REFERENCES sys_user(id) ON DELETE CASCADE
);

-- 索引：按文档统计下载次数
CREATE INDEX idx_download_log_document
    ON doc_download_log(document_id, download_time DESC)
    WHERE deleted = false;

-- 索引：按用户查询下载历史
CREATE INDEX idx_download_log_user
    ON doc_download_log(user_id, download_time DESC)
    WHERE deleted = false;

-- 索引：按时间范围统计
CREATE INDEX idx_download_log_time
    ON doc_download_log(download_time DESC)
    WHERE deleted = false;

COMMENT ON TABLE doc_download_log IS '文档下载记录表';
COMMENT ON COLUMN doc_download_log.id IS '主键ID';
COMMENT ON COLUMN doc_download_log.document_id IS '文档ID';
COMMENT ON COLUMN doc_download_log.version_no IS '下载的版本号';
COMMENT ON COLUMN doc_download_log.user_id IS '下载用户ID';
COMMENT ON COLUMN doc_download_log.download_time IS '下载时间';
COMMENT ON COLUMN doc_download_log.ip_address IS 'IP地址';
COMMENT ON COLUMN doc_download_log.user_agent IS '用户代理';
COMMENT ON COLUMN doc_download_log.deleted IS '逻辑删除标记';
