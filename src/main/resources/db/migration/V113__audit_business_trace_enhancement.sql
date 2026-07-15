-- Enhance audit_operation_log for user-facing business trace display.

alter table audit_operation_log
    add column if not exists object_name varchar(255),
    add column if not exists action_summary varchar(500),
    add column if not exists related_biz_type varchar(64),
    add column if not exists related_biz_id bigint,
    add column if not exists client_ip varchar(64),
    add column if not exists user_agent varchar(500);

comment on column audit_operation_log.object_name is '业务对象名称，例如文件名、项目名、文件夹名';
comment on column audit_operation_log.action_summary is '用户可读的一句话操作摘要';
comment on column audit_operation_log.related_biz_type is '关联业务类型，例如 APPROVAL_INSTANCE/PROJECT';
comment on column audit_operation_log.related_biz_id is '关联业务主键，例如审批实例ID';
comment on column audit_operation_log.client_ip is '客户端IP，用于预览、下载等用户行为追溯';
comment on column audit_operation_log.user_agent is '客户端User-Agent，用于预览、下载等用户行为追溯';

create index if not exists idx_audit_operation_related_biz
    on audit_operation_log(related_biz_type, related_biz_id)
    where deleted = false;

create index if not exists idx_audit_operation_object_name
    on audit_operation_log(object_name)
    where deleted = false;
