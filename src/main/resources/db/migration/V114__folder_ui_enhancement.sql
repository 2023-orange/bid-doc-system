CREATE TABLE IF NOT EXISTS doc_folder_access_log (
    id           BIGINT PRIMARY KEY,
    folder_id    BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    access_type  VARCHAR(20) NOT NULL,
    access_time  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted      BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(64),
    CONSTRAINT ck_doc_folder_access_type
        CHECK (access_type IN ('VIEW', 'DOWNLOAD', 'EDIT'))
);

CREATE INDEX IF NOT EXISTS idx_doc_folder_access_log_folder
    ON doc_folder_access_log(folder_id, access_time DESC)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_folder_access_log_user
    ON doc_folder_access_log(user_id, access_time DESC)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_folder_access_log_folder_user_time
    ON doc_folder_access_log(folder_id, user_id, access_time DESC)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_folder_access_log_type
    ON doc_folder_access_log(access_type, access_time DESC)
    WHERE deleted = false;

COMMENT ON TABLE doc_folder_access_log IS '文件夹访问记录表';
COMMENT ON COLUMN doc_folder_access_log.folder_id IS '访问的文件夹ID';
COMMENT ON COLUMN doc_folder_access_log.user_id IS '访问用户ID';
COMMENT ON COLUMN doc_folder_access_log.access_type IS '访问类型：VIEW/DOWNLOAD/EDIT';
COMMENT ON COLUMN doc_folder_access_log.access_time IS '访问发生时间';

CREATE TABLE IF NOT EXISTS doc_folder_tag (
    id          BIGINT PRIMARY KEY,
    folder_id   BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    deleted     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(64)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_folder_tag_active
    ON doc_folder_tag(folder_id, tag_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_folder_tag_folder
    ON doc_folder_tag(folder_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_doc_folder_tag_tag
    ON doc_folder_tag(tag_id)
    WHERE deleted = false;

COMMENT ON TABLE doc_folder_tag IS '文件夹与标签关系表，复用doc_tag标签词表';
COMMENT ON COLUMN doc_folder_tag.folder_id IS '文件夹ID';
COMMENT ON COLUMN doc_folder_tag.tag_id IS '标签ID，引用doc_tag';
