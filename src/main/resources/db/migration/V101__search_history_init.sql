-- Goal-3: 搜索增强 - 搜索历史表
-- 记录用户的搜索关键词，用于搜索历史和热门搜索统计

CREATE TABLE IF NOT EXISTS doc_search_history (
    id                  BIGINT          PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    keyword             VARCHAR(200)    NOT NULL,
    folder_id           BIGINT,
    result_count        INTEGER         NOT NULL DEFAULT 0,
    search_time         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN         NOT NULL DEFAULT false,

    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id)
        REFERENCES sys_user(id) ON DELETE CASCADE
);

-- 索引：按用户查询搜索历史
CREATE INDEX idx_search_history_user_time
    ON doc_search_history(user_id, search_time DESC)
    WHERE deleted = false;

-- 索引：统计热门搜索关键词
CREATE INDEX idx_search_history_keyword
    ON doc_search_history(keyword, search_time DESC)
    WHERE deleted = false;

-- 索引：按时间范围统计
CREATE INDEX idx_search_history_time
    ON doc_search_history(search_time DESC)
    WHERE deleted = false;

COMMENT ON TABLE doc_search_history IS '文档搜索历史记录表';
COMMENT ON COLUMN doc_search_history.id IS '主键ID';
COMMENT ON COLUMN doc_search_history.user_id IS '搜索用户ID';
COMMENT ON COLUMN doc_search_history.keyword IS '搜索关键词';
COMMENT ON COLUMN doc_search_history.folder_id IS '搜索范围文件夹ID（可选）';
COMMENT ON COLUMN doc_search_history.result_count IS '搜索结果数量';
COMMENT ON COLUMN doc_search_history.search_time IS '搜索时间';
COMMENT ON COLUMN doc_search_history.deleted IS '逻辑删除标记';
