CREATE TABLE IF NOT EXISTS wf_approval_definition (
    id                BIGINT PRIMARY KEY,
    name              VARCHAR(128) NOT NULL,
    scenario          VARCHAR(64) NOT NULL,
    biz_module        VARCHAR(64) NOT NULL,
    biz_type          VARCHAR(64) NOT NULL,
    dept_id           BIGINT,
    business_category VARCHAR(64),
    version           INTEGER NOT NULL DEFAULT 1,
    enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64),
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_wf_approval_definition_match
    ON wf_approval_definition(scenario, biz_module, biz_type, dept_id, business_category, enabled, version DESC)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wf_approval_definition_active
    ON wf_approval_definition(scenario, biz_module, biz_type, COALESCE(dept_id, -1), COALESCE(business_category, ''), version)
    WHERE deleted = FALSE;

COMMENT ON TABLE wf_approval_definition IS 'Workflow approval definition table';

CREATE TABLE IF NOT EXISTS wf_approval_node (
    id                  BIGINT PRIMARY KEY,
    definition_id       BIGINT NOT NULL,
    node_code           VARCHAR(64) NOT NULL,
    node_name           VARCHAR(128) NOT NULL,
    node_type           VARCHAR(32) NOT NULL,
    approve_mode        VARCHAR(32),
    assignee_type       VARCHAR(32),
    assignee_value      VARCHAR(256),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    next_node_code      VARCHAR(64),
    reject_to_node_code VARCHAR(64),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(64)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wf_approval_node_code
    ON wf_approval_node(definition_id, node_code)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_wf_approval_node_order
    ON wf_approval_node(definition_id, sort_order, created_at)
    WHERE deleted = FALSE;

COMMENT ON TABLE wf_approval_node IS 'Workflow approval node table';

CREATE TABLE IF NOT EXISTS wf_approval_condition (
    id               BIGINT PRIMARY KEY,
    definition_id    BIGINT NOT NULL,
    node_id          BIGINT NOT NULL,
    condition_code   VARCHAR(64) NOT NULL,
    field_name       VARCHAR(64) NOT NULL,
    operator         VARCHAR(32) NOT NULL,
    compare_value    VARCHAR(256),
    target_node_code VARCHAR(64) NOT NULL,
    sort_order       INTEGER NOT NULL DEFAULT 0,
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(64),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_wf_approval_condition_node
    ON wf_approval_condition(node_id, sort_order)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wf_approval_condition_code
    ON wf_approval_condition(node_id, condition_code)
    WHERE deleted = FALSE;

COMMENT ON TABLE wf_approval_condition IS 'Workflow approval condition table';

CREATE TABLE IF NOT EXISTS wf_approval_action_log (
    id              BIGINT PRIMARY KEY,
    instance_id     BIGINT NOT NULL,
    task_id         BIGINT,
    definition_id   BIGINT,
    node_id         BIGINT,
    node_code       VARCHAR(64),
    action_type     VARCHAR(32) NOT NULL,
    action_user_id  BIGINT NOT NULL,
    action_comment  VARCHAR(500),
    action_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_status   VARCHAR(32),
    after_status    VARCHAR(32),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_wf_approval_action_log_instance
    ON wf_approval_action_log(instance_id, action_at DESC)
    WHERE deleted = FALSE;

COMMENT ON TABLE wf_approval_action_log IS 'Workflow approval action log table';

CREATE TABLE IF NOT EXISTS wf_approval_task_candidate (
    id                 BIGINT PRIMARY KEY,
    task_id            BIGINT NOT NULL,
    instance_id        BIGINT NOT NULL,
    definition_id      BIGINT,
    node_id            BIGINT,
    node_code          VARCHAR(64),
    candidate_type     VARCHAR(32) NOT NULL,
    candidate_value    VARCHAR(256),
    candidate_user_id  BIGINT,
    resolved           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(64),
    updated_at         TIMESTAMPTZ,
    updated_by         VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_wf_approval_task_candidate_task
    ON wf_approval_task_candidate(task_id)
    WHERE deleted = FALSE;

COMMENT ON TABLE wf_approval_task_candidate IS 'Workflow approval task candidate snapshot table';

ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS definition_id BIGINT;
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS definition_version INTEGER;
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS current_node_id BIGINT;
ALTER TABLE wf_approval_instance ADD COLUMN IF NOT EXISTS current_node_code VARCHAR(64);

ALTER TABLE wf_approval_task ADD COLUMN IF NOT EXISTS definition_id BIGINT;
ALTER TABLE wf_approval_task ADD COLUMN IF NOT EXISTS node_id BIGINT;
ALTER TABLE wf_approval_task ADD COLUMN IF NOT EXISTS node_code VARCHAR(64);
ALTER TABLE wf_approval_task ADD COLUMN IF NOT EXISTS transferred_from_task_id BIGINT;
ALTER TABLE wf_approval_task ADD COLUMN IF NOT EXISTS add_sign BOOLEAN DEFAULT FALSE;
UPDATE wf_approval_task SET add_sign = FALSE WHERE add_sign IS NULL;
ALTER TABLE wf_approval_task ALTER COLUMN add_sign SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_wf_approval_task_node
    ON wf_approval_task(instance_id, node_id, status)
    WHERE deleted = FALSE;
