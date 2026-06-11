create table if not exists doc_tag (
    id          bigint primary key,
    name        varchar(64) not null,
    deleted     boolean not null default false,
    created_at  timestamptz not null default current_timestamp,
    created_by  varchar(64)
);

create unique index if not exists uk_doc_tag_name_active
    on doc_tag(name)
    where deleted = false;

comment on table doc_tag is '文档标签表';

create table if not exists doc_document_tag (
    id          bigint primary key,
    document_id bigint not null,
    tag_id      bigint not null,
    deleted     boolean not null default false,
    created_at  timestamptz not null default current_timestamp,
    created_by  varchar(64)
);

create unique index if not exists uk_doc_document_tag_active
    on doc_document_tag(document_id, tag_id)
    where deleted = false;

create index if not exists idx_doc_document_tag_tag
    on doc_document_tag(tag_id)
    where deleted = false;

comment on table doc_document_tag is '文档与标签关系表';

create table if not exists sys_notification (
    id               bigint primary key,
    receiver_user_id bigint not null,
    type             varchar(64) not null,
    title            varchar(128) not null,
    content          varchar(500),
    biz_type         varchar(64),
    biz_id           bigint,
    read_flag        boolean not null default false,
    read_at          timestamptz,
    deleted          boolean not null default false,
    created_at       timestamptz not null default current_timestamp,
    created_by       varchar(64)
);

create index if not exists idx_sys_notification_receiver_read
    on sys_notification(receiver_user_id, read_flag, created_at desc)
    where deleted = false;

comment on table sys_notification is '站内通知表';

create table if not exists wf_approval_instance (
    id                bigint primary key,
    document_id       bigint not null,
    submitter_user_id bigint not null,
    status            varchar(32) not null,
    submit_comment    varchar(500),
    submitted_at      timestamptz not null default current_timestamp,
    completed_at      timestamptz,
    deleted           boolean not null default false,
    created_at        timestamptz not null default current_timestamp,
    created_by        varchar(64),
    updated_at        timestamptz,
    updated_by        varchar(64)
);

create index if not exists idx_wf_approval_instance_document
    on wf_approval_instance(document_id, submitted_at desc)
    where deleted = false;

comment on table wf_approval_instance is '文档审批实例表';

create table if not exists wf_approval_task (
    id               bigint primary key,
    instance_id      bigint not null,
    document_id      bigint not null,
    approver_user_id bigint not null,
    status           varchar(32) not null,
    comment          varchar(500),
    handled_at       timestamptz,
    deleted          boolean not null default false,
    created_at       timestamptz not null default current_timestamp,
    created_by       varchar(64),
    updated_at       timestamptz,
    updated_by       varchar(64)
);

create index if not exists idx_wf_approval_task_approver_status
    on wf_approval_task(approver_user_id, status, created_at desc)
    where deleted = false;

create index if not exists idx_wf_approval_task_instance
    on wf_approval_task(instance_id)
    where deleted = false;

comment on table wf_approval_task is '文档审批任务表';
