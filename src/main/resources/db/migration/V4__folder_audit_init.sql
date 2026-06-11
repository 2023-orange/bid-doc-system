-- PostgreSQL initialization script for folder and audit phase 1

create table if not exists doc_folder (
    id bigint primary key,
    parent_id bigint not null default 0,
    name varchar(128) not null,
    ancestor_ids varchar(1024) not null,
    level integer not null default 0,
    sort_no integer not null default 0,
    owner_dept_id bigint null,
    owner_user_id bigint null,
    inherit_permission boolean not null default true,
    status integer not null default 1,
    remark varchar(500),
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false
);

comment on table doc_folder is '文件夹树节点表';
comment on column doc_folder.id is '主键';
comment on column doc_folder.parent_id is '父节点ID，根级文件夹固定为0';
comment on column doc_folder.name is '文件夹名称';
comment on column doc_folder.ancestor_ids is '祖先链，例如1,1001,1002';
comment on column doc_folder.level is '层级深度，根级文件夹为0';
comment on column doc_folder.sort_no is '排序号';
comment on column doc_folder.owner_dept_id is '所属部门ID，根级可为空';
comment on column doc_folder.owner_user_id is '负责人/创建人';
comment on column doc_folder.inherit_permission is '是否继承父级权限';
comment on column doc_folder.status is '状态，预留';
comment on column doc_folder.remark is '备注';
comment on column doc_folder.created_at is '创建时间';
comment on column doc_folder.created_by is '创建人';
comment on column doc_folder.updated_at is '更新时间';
comment on column doc_folder.updated_by is '更新人';
comment on column doc_folder.deleted is '逻辑删除标记';

create index if not exists idx_doc_folder_parent_id
    on doc_folder(parent_id)
    where deleted = false;

create index if not exists idx_doc_folder_owner_dept_id
    on doc_folder(owner_dept_id)
    where deleted = false;

create index if not exists idx_doc_folder_owner_user_id
    on doc_folder(owner_user_id)
    where deleted = false;

create index if not exists idx_doc_folder_level
    on doc_folder(level)
    where deleted = false;

create index if not exists idx_doc_folder_created_at
    on doc_folder(created_at);

create unique index if not exists uk_doc_folder_parent_name_active
    on doc_folder(parent_id, name)
    where deleted = false;


create table if not exists doc_folder_grant (
    id bigint primary key,
    folder_id bigint not null,
    subject_type varchar(32) not null,
    subject_id varchar(64) not null,
    permission_code varchar(64) not null,
    grant_scope varchar(32) not null default 'SELF',
    effective_from timestamptz null,
    effective_to timestamptz null,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false,
    constraint ck_doc_folder_grant_subject_type
        check (subject_type in ('USER', 'ROLE', 'DEPT')),
    constraint ck_doc_folder_grant_scope
        check (grant_scope in ('SELF', 'SELF_AND_DESCENDANTS'))
);

comment on table doc_folder_grant is '文件夹显式授权表';
comment on column doc_folder_grant.id is '主键';
comment on column doc_folder_grant.folder_id is '文件夹ID';
comment on column doc_folder_grant.subject_type is '授权主体类型：USER/ROLE/DEPT';
comment on column doc_folder_grant.subject_id is '授权主体标识，USER/DEPT存字符串化ID，ROLE存roleCode';
comment on column doc_folder_grant.permission_code is '权限编码';
comment on column doc_folder_grant.grant_scope is '授权范围：SELF/SELF_AND_DESCENDANTS';
comment on column doc_folder_grant.effective_from is '生效开始时间';
comment on column doc_folder_grant.effective_to is '生效结束时间';
comment on column doc_folder_grant.created_at is '创建时间';
comment on column doc_folder_grant.created_by is '创建人';
comment on column doc_folder_grant.updated_at is '更新时间';
comment on column doc_folder_grant.updated_by is '更新人';
comment on column doc_folder_grant.deleted is '逻辑删除标记';

create index if not exists idx_doc_folder_grant_folder_id
    on doc_folder_grant(folder_id)
    where deleted = false;

create index if not exists idx_doc_folder_grant_subject
    on doc_folder_grant(subject_type, subject_id)
    where deleted = false;

create index if not exists idx_doc_folder_grant_effective
    on doc_folder_grant(effective_from, effective_to);

create unique index if not exists uk_doc_folder_grant_active
    on doc_folder_grant(folder_id, subject_type, subject_id, permission_code)
    where deleted = false;


create table if not exists doc_folder_manager (
    id bigint primary key,
    folder_id bigint not null,
    user_id bigint not null,
    manage_scope varchar(32) not null default 'SELF_AND_DESCENDANTS',
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false,
    constraint ck_doc_folder_manager_scope
        check (manage_scope in ('SELF', 'SELF_AND_DESCENDANTS'))
);

comment on table doc_folder_manager is '文件夹管理员表';
comment on column doc_folder_manager.id is '主键';
comment on column doc_folder_manager.folder_id is '文件夹ID';
comment on column doc_folder_manager.user_id is '管理员用户ID';
comment on column doc_folder_manager.manage_scope is '管理范围：SELF/SELF_AND_DESCENDANTS';
comment on column doc_folder_manager.created_at is '创建时间';
comment on column doc_folder_manager.created_by is '创建人';
comment on column doc_folder_manager.updated_at is '更新时间';
comment on column doc_folder_manager.updated_by is '更新人';
comment on column doc_folder_manager.deleted is '逻辑删除标记';

create index if not exists idx_doc_folder_manager_folder_id
    on doc_folder_manager(folder_id)
    where deleted = false;

create index if not exists idx_doc_folder_manager_user_id
    on doc_folder_manager(user_id)
    where deleted = false;

create unique index if not exists uk_doc_folder_manager_active
    on doc_folder_manager(folder_id, user_id)
    where deleted = false;


create table if not exists doc_folder_favorite (
    id bigint primary key,
    folder_id bigint not null,
    user_id bigint not null,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    deleted boolean not null default false
);

comment on table doc_folder_favorite is '用户收藏文件夹关系表';
comment on column doc_folder_favorite.id is '主键';
comment on column doc_folder_favorite.folder_id is '文件夹ID';
comment on column doc_folder_favorite.user_id is '收藏用户ID';
comment on column doc_folder_favorite.created_at is '创建时间';
comment on column doc_folder_favorite.created_by is '创建人';
comment on column doc_folder_favorite.deleted is '逻辑删除标记';

create index if not exists idx_doc_folder_favorite_user_id
    on doc_folder_favorite(user_id)
    where deleted = false;

create index if not exists idx_doc_folder_favorite_folder_id
    on doc_folder_favorite(folder_id)
    where deleted = false;

create unique index if not exists uk_doc_folder_favorite_active
    on doc_folder_favorite(folder_id, user_id)
    where deleted = false;


create table if not exists audit_operation_log (
    id bigint primary key,
    module_code varchar(64) not null,
    biz_type varchar(64) not null,
    biz_id bigint not null,
    operation_type varchar(64) not null,
    operator_user_id bigint,
    operator_dept_id bigint,
    request_id varchar(64),
    operation_time timestamptz not null default now(),
    before_data jsonb,
    after_data jsonb,
    extra_data jsonb,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    deleted boolean not null default false
);

comment on table audit_operation_log is '通用业务审计日志表';
comment on column audit_operation_log.id is '主键';
comment on column audit_operation_log.module_code is '模块编码，例如FOLDER';
comment on column audit_operation_log.biz_type is '业务类型，例如FOLDER';
comment on column audit_operation_log.biz_id is '业务主键';
comment on column audit_operation_log.operation_type is '操作类型，例如CREATE/UPDATE/DELETE/MOVE/COPY';
comment on column audit_operation_log.operator_user_id is '操作人用户ID';
comment on column audit_operation_log.operator_dept_id is '操作人部门ID';
comment on column audit_operation_log.request_id is '请求链路ID，对应traceId';
comment on column audit_operation_log.operation_time is '业务操作时间';
comment on column audit_operation_log.before_data is '操作前数据快照';
comment on column audit_operation_log.after_data is '操作后数据快照';
comment on column audit_operation_log.extra_data is '额外扩展数据';
comment on column audit_operation_log.created_at is '创建时间';
comment on column audit_operation_log.created_by is '创建人';
comment on column audit_operation_log.deleted is '逻辑删除标记';

create index if not exists idx_audit_operation_module_code
    on audit_operation_log(module_code)
    where deleted = false;

create index if not exists idx_audit_operation_biz
    on audit_operation_log(biz_type, biz_id)
    where deleted = false;

create index if not exists idx_audit_operation_type
    on audit_operation_log(operation_type)
    where deleted = false;

create index if not exists idx_audit_operation_operator_user
    on audit_operation_log(operator_user_id)
    where deleted = false;

create index if not exists idx_audit_operation_operator_dept
    on audit_operation_log(operator_dept_id)
    where deleted = false;

create index if not exists idx_audit_operation_time
    on audit_operation_log(operation_time)
    where deleted = false;
