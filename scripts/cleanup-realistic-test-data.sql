-- 本地开发测试数据清洗与真实业务种子数据重建脚本。
-- 仅用于 bid_doc_system 本地开发库；不修改 flyway_schema_history。

BEGIN;

-- 按业务依赖从叶子到根清理旧测试数据，避免历史烟测数据继续污染页面。
DELETE FROM wf_approval_task_candidate;
DELETE FROM wf_approval_task;
DELETE FROM wf_approval_action_log;
DELETE FROM wf_approval_instance;
DELETE FROM wf_approval_condition;
DELETE FROM wf_approval_node;
DELETE FROM wf_approval_definition;

DELETE FROM bid_project_archive_checklist_snapshot;
DELETE FROM bid_project_archive_document_snapshot;
DELETE FROM bid_project_archive_record;
DELETE FROM bid_project_checklist_document;
DELETE FROM bid_project_checklist_item;
DELETE FROM bid_project_member;
DELETE FROM bid_project_no_sequence;
DELETE FROM bid_checklist_template_item;
DELETE FROM bid_checklist_template;
DELETE FROM bid_project;

DELETE FROM doc_download_log;
DELETE FROM doc_search_history;
DELETE FROM doc_document_use_grant;
DELETE FROM doc_document_tag;
DELETE FROM doc_document_version;
DELETE FROM doc_document;
DELETE FROM doc_folder_favorite;
DELETE FROM doc_folder_grant;
DELETE FROM doc_folder_manager;
DELETE FROM doc_folder;
DELETE FROM doc_tag;

DELETE FROM sys_notification;
DELETE FROM audit_operation_log;

-- 用户表纳入清洗：删除烟测用户及其角色关系，保留真实 seed 用户。
DELETE FROM sys_user_role
WHERE user_id IN (
    SELECT id
    FROM sys_user
    WHERE username ILIKE '%codex-smoke%'
       OR created_by ILIKE '%codex-smoke%'
);

DELETE FROM sys_user
WHERE username ILIKE '%codex-smoke%'
   OR created_by ILIKE '%codex-smoke%';

-- 保证基础角色存在并保持启用。
INSERT INTO sys_role (id, role_code, role_name, status, deleted, created_by, updated_by, remark)
VALUES
    (1, 'SUPER_ADMIN', '超级管理员', 1, false, 'seed', 'seed', '拥有系统全部管理权限'),
    (2, 'FOLDER_ADMIN', '文件夹管理员', 1, false, 'seed', 'seed', '可被设置为具体文件夹管理员'),
    (3, 'DEPT_MANAGER', '部门经理', 1, false, 'seed', 'seed', '负责本部门协作与审核'),
    (4, 'EMPLOYEE', '普通员工', 1, false, 'seed', 'seed', '默认业务使用角色')
ON CONFLICT (id) DO UPDATE SET
    role_code = EXCLUDED.role_code,
    role_name = EXCLUDED.role_name,
    status = EXCLUDED.status,
    deleted = false,
    updated_by = 'seed',
    remark = EXCLUDED.remark;

-- 保留真实部门组织，同时补齐开发库可能缺失的基础部门。
INSERT INTO sys_department (id, name, parent_id, level, manager_user_id, status, deleted, created_by, updated_by, remark, extension_data)
VALUES
    (3001000000000000001, '综合管理部', null, 1, null, 1, false, 'seed', 'seed', '负责行政、人事与公司级制度管理', '{"abbr":"ZHGL","office":"A座12层"}'),
    (3001000000000000002, '市场经营部', null, 1, null, 1, false, 'seed', 'seed', '负责商机、客户与投标经营', '{"office":"B座8层"}'),
    (3001000000000000003, '技术中心', null, 1, null, 1, false, 'seed', 'seed', '负责技术方案、产品与交付支持', '{"office":"C座6层"}'),
    (3001000000000000004, '质量安全部', null, 1, null, 1, false, 'seed', 'seed', '负责质量体系与安全生产资料', '{"office":"A座9层"}'),
    (3001000000000000005, '财务法务部', null, 1, null, 1, false, 'seed', 'seed', '负责财务结算、合同与合规审查', '{"office":"A座10层"}'),
    (3001000000000000011, '行政办公室', 3001000000000000001, 2, null, 1, false, 'seed', 'seed', '综合行政支持', null),
    (3001000000000000012, '人力资源部', 3001000000000000001, 2, null, 1, false, 'seed', 'seed', '人员、培训与组织信息维护', null),
    (3001000000000000021, '投标管理部', 3001000000000000002, 2, null, 1, false, 'seed', 'seed', '投标文件统筹与过程管理', null),
    (3001000000000000022, '华东市场部', 3001000000000000002, 2, null, 1, false, 'seed', 'seed', '华东区域客户与商机跟进', null),
    (3001000000000000023, '华北市场部', 3001000000000000002, 2, null, 1, false, 'seed', 'seed', '华北区域客户与商机跟进', null),
    (3001000000000000031, '技术方案室', 3001000000000000003, 2, null, 1, false, 'seed', 'seed', '售前技术方案与投标技术标', null),
    (3001000000000000032, '产品研发部', 3001000000000000003, 2, null, 1, false, 'seed', 'seed', '产品资料、版本说明与技术沉淀', null),
    (3001000000000000041, '质量体系室', 3001000000000000004, 2, null, 1, false, 'seed', 'seed', '体系认证、内审与过程质量资料', null),
    (3001000000000000042, '安全管理室', 3001000000000000004, 2, null, 1, false, 'seed', 'seed', '安全资质、人员证书与安全方案', null),
    (3001000000000000051, '财务部', 3001000000000000005, 2, null, 1, false, 'seed', 'seed', '收付款、保证金与结算材料', null),
    (3001000000000000052, '法务合规部', 3001000000000000005, 2, null, 1, false, 'seed', 'seed', '合同模板、授权文件与法律审查', null)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    parent_id = EXCLUDED.parent_id,
    level = EXCLUDED.level,
    status = 1,
    deleted = false,
    updated_by = 'seed',
    remark = EXCLUDED.remark,
    extension_data = EXCLUDED.extension_data;

-- 统一保留用户密码为 12345678 的 BCrypt 密文，并去掉旧的 demo 命名。
INSERT INTO sys_user (id, username, password, real_name, email, mobile, dept_id, job_level, status, deleted, last_login_time, login_count, created_by, updated_by, remark, extension_data)
VALUES
    (3002000000000000001, 'admin', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '系统管理员', 'admin@biddoc.local', '13800000001', 3001000000000000001, 1, 1, false, now() - interval '1 hour', 18, 'seed', 'seed', '本地演示超级管理员', '{"avatar":"admin"}'),
    (3002000000000000002, 'zhang_manager', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '张晨', 'zhang.chen@biddoc.local', '13800000002', 3001000000000000021, 1, 1, false, now() - interval '2 hours', 9, 'seed', 'seed', '市场经营部负责人', '{"title":"部门经理"}'),
    (3002000000000000003, 'li_folder_admin', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '李若溪', 'li.ruoxi@biddoc.local', '13800000003', 3001000000000000021, 2, 1, false, now() - interval '3 hours', 12, 'seed', 'seed', '投标文件夹管理员', '{"title":"投标专员"}'),
    (3002000000000000004, 'wang_engineer', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '王一鸣', 'wang.yiming@biddoc.local', '13800000004', 3001000000000000031, 3, 1, false, now() - interval '4 hours', 7, 'seed', 'seed', '技术方案工程师', '{"title":"方案工程师"}'),
    (3002000000000000005, 'chen_quality', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '陈书瑶', 'chen.shuyao@biddoc.local', '13800000005', 3001000000000000041, 2, 1, false, now() - interval '1 day', 5, 'seed', 'seed', '质量体系负责人', '{"title":"质量主管"}'),
    (3002000000000000006, 'zhao_finance', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '赵明远', 'zhao.mingyuan@biddoc.local', '13800000006', 3001000000000000051, 3, 1, false, now() - interval '5 hours', 4, 'seed', 'seed', '财务资料维护人', '{"title":"财务专员"}'),
    (3002000000000000007, 'sun_legal', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '孙嘉言', 'sun.jiayan@biddoc.local', '13800000007', 3001000000000000052, 3, 1, false, now() - interval '6 hours', 3, 'seed', 'seed', '合同与法务资料维护人', '{"title":"法务专员"}'),
    (3002000000000000008, 'he_sales', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '何景行', 'he.jingxing@biddoc.local', '13800000008', 3001000000000000022, 3, 1, false, now() - interval '8 hours', 6, 'seed', 'seed', '华东区域销售', '{"title":"客户经理"}'),
    (3002000000000000009, 'qian_archive', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '钱思源', 'qian.siyuan@biddoc.local', '13800000009', 3001000000000000011, 2, 1, false, now() - interval '10 hours', 2, 'seed', 'seed', '归档管理员', '{"title":"档案管理员"}'),
    (3002000000000000010, 'lin_disabled', '$2a$10$OzJA5n5U16qMzCOVfM6bXueWjC9LO8Dri0e9bTE1wNHd4YE4YdWEO', '林停用', 'disabled@biddoc.local', '13800000010', 3001000000000000012, 3, 0, false, null, 0, 'seed', 'seed', '用于验证禁用账号无法登录', '{"title":"停用账号"}')
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    real_name = EXCLUDED.real_name,
    email = EXCLUDED.email,
    mobile = EXCLUDED.mobile,
    dept_id = EXCLUDED.dept_id,
    job_level = EXCLUDED.job_level,
    status = EXCLUDED.status,
    deleted = false,
    updated_by = 'seed',
    remark = EXCLUDED.remark,
    extension_data = EXCLUDED.extension_data;

DELETE FROM sys_user_role;
INSERT INTO sys_user_role (id, user_id, role_code, is_primary, status, source_type, deleted, created_by, updated_by)
VALUES
    (3002100000000000001, 3002000000000000001, 'SUPER_ADMIN', true, 1, 2, false, 'seed', 'seed'),
    (3002100000000000002, 3002000000000000002, 'DEPT_MANAGER', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000003, 3002000000000000002, 'EMPLOYEE', false, 1, 1, false, 'seed', 'seed'),
    (3002100000000000004, 3002000000000000003, 'FOLDER_ADMIN', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000005, 3002000000000000003, 'EMPLOYEE', false, 1, 1, false, 'seed', 'seed'),
    (3002100000000000006, 3002000000000000004, 'EMPLOYEE', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000007, 3002000000000000005, 'DEPT_MANAGER', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000008, 3002000000000000005, 'EMPLOYEE', false, 1, 1, false, 'seed', 'seed'),
    (3002100000000000009, 3002000000000000006, 'EMPLOYEE', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000010, 3002000000000000007, 'EMPLOYEE', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000011, 3002000000000000008, 'EMPLOYEE', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000012, 3002000000000000009, 'FOLDER_ADMIN', true, 1, 1, false, 'seed', 'seed'),
    (3002100000000000013, 3002000000000000009, 'EMPLOYEE', false, 1, 1, false, 'seed', 'seed'),
    (3002100000000000014, 3002000000000000010, 'EMPLOYEE', true, 1, 1, false, 'seed', 'seed');

UPDATE sys_department SET manager_user_id = 3002000000000000001 WHERE id = 3001000000000000001;
UPDATE sys_department SET manager_user_id = 3002000000000000002 WHERE id = 3001000000000000002;
UPDATE sys_department SET manager_user_id = 3002000000000000004 WHERE id = 3001000000000000003;
UPDATE sys_department SET manager_user_id = 3002000000000000005 WHERE id = 3001000000000000004;
UPDATE sys_department SET manager_user_id = 3002000000000000007 WHERE id = 3001000000000000005;

-- 文件夹树：覆盖根目录、项目目录、标书结构目录、资料库目录和归档目录。
INSERT INTO doc_folder (id, parent_id, name, ancestor_ids, level, sort_no, owner_dept_id, owner_user_id, inherit_permission, status, remark, created_by, updated_by)
VALUES
    (3003000000000000001, 0, '投标项目', '3003000000000000001', 0, 1, 3001000000000000021, 3002000000000000002, true, 1, '公司在投项目资料总目录', 'seed', 'seed'),
    (3003000000000000002, 0, '资质证照', '3003000000000000002', 0, 2, 3001000000000000041, 3002000000000000005, true, 1, '企业资质、体系证书和人员证照', 'seed', 'seed'),
    (3003000000000000003, 0, '合同模板', '3003000000000000003', 0, 3, 3001000000000000052, 3002000000000000007, true, 1, '标准合同和协议模板', 'seed', 'seed'),
    (3003000000000000004, 0, '技术资料库', '3003000000000000004', 0, 4, 3001000000000000031, 3002000000000000004, true, 1, '技术方案、产品材料和实施方法库', 'seed', 'seed'),
    (3003000000000000005, 0, '部门共享', '3003000000000000005', 0, 5, 3001000000000000001, 3002000000000000001, true, 1, '跨部门协作资料入口', 'seed', 'seed'),
    (3003000000000000006, 0, '历史归档', '3003000000000000006', 0, 6, 3001000000000000011, 3002000000000000009, true, 1, '已完成项目和历史资料归档', 'seed', 'seed'),
    (3003000000000000101, 3003000000000000001, '东海医院智能化改造项目', '3003000000000000001,3003000000000000101', 1, 1, 3001000000000000021, 3002000000000000002, true, 1, '医疗行业重点投标项目', 'seed', 'seed'),
    (3003000000000000102, 3003000000000000001, '星河数据中心扩容项目', '3003000000000000001,3003000000000000102', 1, 2, 3001000000000000021, 3002000000000000002, true, 1, '数据中心机房扩容投标资料', 'seed', 'seed'),
    (3003000000000000103, 3003000000000000001, '城轨5号线弱电集成项目', '3003000000000000001,3003000000000000103', 1, 3, 3001000000000000023, 3002000000000000008, true, 1, '轨道交通弱电集成项目', 'seed', 'seed'),
    (3003000000000000104, 3003000000000000001, '智慧园区平台建设项目', '3003000000000000001,3003000000000000104', 1, 4, 3001000000000000022, 3002000000000000008, true, 1, '园区数字化平台建设投标资料', 'seed', 'seed'),
    (3003000000000000105, 3003000000000000001, '政务云运维服务项目', '3003000000000000001,3003000000000000105', 1, 5, 3001000000000000021, 3002000000000000002, true, 1, '政务云年度运维服务投标资料', 'seed', 'seed'),
    (3003000000000000111, 3003000000000000101, '招标文件', '3003000000000000001,3003000000000000101,3003000000000000111', 2, 1, 3001000000000000021, 3002000000000000003, true, 1, '招标公告、澄清和附件', 'seed', 'seed'),
    (3003000000000000112, 3003000000000000101, '商务标', '3003000000000000001,3003000000000000101,3003000000000000112', 2, 2, 3001000000000000021, 3002000000000000003, true, 1, '报价、资质和商务响应', 'seed', 'seed'),
    (3003000000000000113, 3003000000000000101, '技术标', '3003000000000000001,3003000000000000101,3003000000000000113', 2, 3, 3001000000000000031, 3002000000000000004, true, 1, '技术方案和实施组织设计', 'seed', 'seed'),
    (3003000000000000114, 3003000000000000101, '澄清与答疑', '3003000000000000001,3003000000000000101,3003000000000000114', 2, 4, 3001000000000000021, 3002000000000000002, false, 1, '往来澄清、答疑和补遗文件', 'seed', 'seed'),
    (3003000000000000121, 3003000000000000102, '商务资料', '3003000000000000001,3003000000000000102,3003000000000000121', 2, 1, 3001000000000000021, 3002000000000000003, true, 1, '数据中心项目商务资料', 'seed', 'seed'),
    (3003000000000000122, 3003000000000000102, '技术方案', '3003000000000000001,3003000000000000102,3003000000000000122', 2, 2, 3001000000000000031, 3002000000000000004, true, 1, '数据中心扩容技术方案', 'seed', 'seed'),
    (3003000000000000131, 3003000000000000103, '初稿区', '3003000000000000001,3003000000000000103,3003000000000000131', 2, 1, 3001000000000000023, 3002000000000000008, true, 1, '轨交项目初稿资料', 'seed', 'seed'),
    (3003000000000000141, 3003000000000000104, '投标资料', '3003000000000000001,3003000000000000104,3003000000000000141', 2, 1, 3001000000000000022, 3002000000000000008, true, 1, '园区平台项目投标资料', 'seed', 'seed'),
    (3003000000000000151, 3003000000000000105, '服务方案', '3003000000000000001,3003000000000000105,3003000000000000151', 2, 1, 3001000000000000021, 3002000000000000002, true, 1, '政务云运维服务方案资料', 'seed', 'seed'),
    (3003000000000000201, 3003000000000000002, '营业执照', '3003000000000000002,3003000000000000201', 1, 1, 3001000000000000041, 3002000000000000005, true, 1, '营业执照和基础登记信息', 'seed', 'seed'),
    (3003000000000000202, 3003000000000000002, '安全生产许可证', '3003000000000000002,3003000000000000202', 1, 2, 3001000000000000042, 3002000000000000005, true, 1, '安全生产许可证及延期材料', 'seed', 'seed'),
    (3003000000000000203, 3003000000000000002, '体系认证', '3003000000000000002,3003000000000000203', 1, 3, 3001000000000000041, 3002000000000000005, true, 1, 'ISO、信息安全和服务认证', 'seed', 'seed'),
    (3003000000000000204, 3003000000000000002, '人员证书', '3003000000000000002,3003000000000000204', 1, 4, 3001000000000000042, 3002000000000000005, false, 1, '人员资格证书和授权文件', 'seed', 'seed'),
    (3003000000000000301, 3003000000000000003, '采购合同模板', '3003000000000000003,3003000000000000301', 1, 1, 3001000000000000052, 3002000000000000007, true, 1, '采购类合同模板', 'seed', 'seed'),
    (3003000000000000302, 3003000000000000003, '分包合同模板', '3003000000000000003,3003000000000000302', 1, 2, 3001000000000000052, 3002000000000000007, true, 1, '工程和服务分包合同模板', 'seed', 'seed'),
    (3003000000000000303, 3003000000000000003, '保密协议模板', '3003000000000000003,3003000000000000303', 1, 3, 3001000000000000052, 3002000000000000007, true, 1, 'NDA 和项目保密协议', 'seed', 'seed'),
    (3003000000000000401, 3003000000000000004, '标准方案', '3003000000000000004,3003000000000000401', 1, 1, 3001000000000000031, 3002000000000000004, true, 1, '可复用标准技术方案', 'seed', 'seed'),
    (3003000000000000402, 3003000000000000004, '产品彩页', '3003000000000000004,3003000000000000402', 1, 2, 3001000000000000032, 3002000000000000004, true, 1, '产品介绍、彩页和配置清单', 'seed', 'seed'),
    (3003000000000000403, 3003000000000000004, '施工组织设计', '3003000000000000004,3003000000000000403', 1, 3, 3001000000000000031, 3002000000000000004, true, 1, '施工组织与实施计划模板', 'seed', 'seed'),
    (3003000000000000501, 3003000000000000005, '市场经营部共享', '3003000000000000005,3003000000000000501', 1, 1, 3001000000000000021, 3002000000000000002, true, 1, '市场经营部对外共享资料', 'seed', 'seed'),
    (3003000000000000502, 3003000000000000005, '技术中心共享', '3003000000000000005,3003000000000000502', 1, 2, 3001000000000000031, 3002000000000000004, true, 1, '技术中心对外共享资料', 'seed', 'seed'),
    (3003000000000000503, 3003000000000000005, '质量安全部共享', '3003000000000000005,3003000000000000503', 1, 3, 3001000000000000041, 3002000000000000005, true, 1, '质量安全相关共享资料', 'seed', 'seed'),
    (3003000000000000504, 3003000000000000005, '财务法务共享', '3003000000000000005,3003000000000000504', 1, 4, 3001000000000000052, 3002000000000000007, true, 1, '财务与法务共享资料', 'seed', 'seed'),
    (3003000000000000601, 3003000000000000006, '2024已归档项目', '3003000000000000006,3003000000000000601', 1, 1, 3001000000000000011, 3002000000000000009, true, 1, '2024年完成项目归档', 'seed', 'seed'),
    (3003000000000000602, 3003000000000000006, '2025已归档项目', '3003000000000000006,3003000000000000602', 1, 2, 3001000000000000011, 3002000000000000009, true, 1, '2025年完成项目归档', 'seed', 'seed');

INSERT INTO doc_folder_manager (id, folder_id, user_id, manage_scope, created_by, updated_by)
VALUES
    (3003100000000000001, 3003000000000000001, 3002000000000000003, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000002, 3003000000000000113, 3002000000000000004, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000003, 3003000000000000002, 3002000000000000005, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000004, 3003000000000000006, 3002000000000000009, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000005, 3003000000000000301, 3002000000000000007, 'SELF', 'seed', 'seed'),
    (3003100000000000006, 3003000000000000141, 3002000000000000008, 'SELF_AND_DESCENDANTS', 'seed', 'seed');

INSERT INTO doc_folder_grant (id, folder_id, subject_type, subject_id, permission_code, grant_scope, effective_from, effective_to, created_by, updated_by)
VALUES
    (3003200000000000001, 3003000000000000101, 'DEPT', '3001000000000000031', 'FOLDER_VIEW', 'SELF_AND_DESCENDANTS', now() - interval '30 days', null, 'seed', 'seed'),
    (3003200000000000002, 3003000000000000101, 'DEPT', '3001000000000000031', 'FOLDER_CREATE', 'SELF_AND_DESCENDANTS', now() - interval '30 days', null, 'seed', 'seed'),
    (3003200000000000003, 3003000000000000112, 'USER', '3002000000000000006', 'FOLDER_VIEW', 'SELF', now() - interval '20 days', null, 'seed', 'seed'),
    (3003200000000000004, 3003000000000000112, 'USER', '3002000000000000006', 'FOLDER_EDIT', 'SELF', now() - interval '20 days', null, 'seed', 'seed'),
    (3003200000000000005, 3003000000000000114, 'ROLE', 'FOLDER_ADMIN', 'FOLDER_VIEW', 'SELF_AND_DESCENDANTS', now() - interval '10 days', now() + interval '60 days', 'seed', 'seed'),
    (3003200000000000006, 3003000000000000204, 'USER', '3002000000000000008', 'FOLDER_VIEW', 'SELF', now() - interval '5 days', null, 'seed', 'seed'),
    (3003200000000000007, 3003000000000000301, 'DEPT', '3001000000000000051', 'FOLDER_VIEW', 'SELF_AND_DESCENDANTS', now() - interval '15 days', null, 'seed', 'seed'),
    (3003200000000000008, 3003000000000000301, 'DEPT', '3001000000000000051', 'FOLDER_COPY', 'SELF_AND_DESCENDANTS', now() - interval '15 days', null, 'seed', 'seed'),
    (3003200000000000009, 3003000000000000401, 'ROLE', 'EMPLOYEE', 'FOLDER_VIEW', 'SELF', now() - interval '7 days', null, 'seed', 'seed'),
    (3003200000000000010, 3003000000000000501, 'DEPT', '3001000000000000022', 'FOLDER_CREATE', 'SELF_AND_DESCENDANTS', now() - interval '3 days', null, 'seed', 'seed'),
    (3003200000000000011, 3003000000000000502, 'DEPT', '3001000000000000031', 'FOLDER_VIEW', 'SELF_AND_DESCENDANTS', now() - interval '3 days', null, 'seed', 'seed'),
    (3003200000000000012, 3003000000000000601, 'USER', '3002000000000000002', 'FOLDER_AUDIT_VIEW', 'SELF_AND_DESCENDANTS', now() - interval '1 day', null, 'seed', 'seed');

INSERT INTO doc_folder_favorite (id, folder_id, user_id, created_by)
VALUES
    (3003300000000000001, 3003000000000000101, 3002000000000000002, 'seed'),
    (3003300000000000002, 3003000000000000113, 3002000000000000004, 'seed'),
    (3003300000000000003, 3003000000000000203, 3002000000000000005, 'seed'),
    (3003300000000000004, 3003000000000000301, 3002000000000000007, 'seed'),
    (3003300000000000005, 3003000000000000602, 3002000000000000009, 'seed'),
    (3003300000000000006, 3003000000000000141, 3002000000000000008, 'seed');

INSERT INTO doc_tag (id, name, deleted, created_at, created_by)
VALUES
    (3003400000000000001, '商务标', false, now(), 'seed'),
    (3003400000000000002, '技术标', false, now(), 'seed'),
    (3003400000000000003, '资质证照', false, now(), 'seed'),
    (3003400000000000004, '法务审查', false, now(), 'seed'),
    (3003400000000000005, '归档资料', false, now(), 'seed');

-- 文档和版本：覆盖真实投标资料、资质、合同、技术方案和归档材料。
INSERT INTO doc_document (id, folder_id, name, current_version_no, latest_size, latest_mime, owner_user_id, owner_dept_id, status, remark, created_by, updated_by, document_no, document_status, business_category, tender_structure_category, sensitive_level, source_type, effective_date, expire_date, has_expire_date, metadata_completed, invalid_reason)
VALUES
    (3004000000000000001, 3003000000000000112, '东海医院_商务响应文件.pdf', 2, 358400, 'application/pdf', 3002000000000000003, 3001000000000000021, 1, '最终报价前的商务响应文件', 'seed', 'seed', 'DOC-BID-2026-0001', 'APPROVED', 'BID', 'BUSINESS', 'INTERNAL', 'UPLOAD', now() - interval '8 days', null, false, true, null),
    (3004000000000000002, 3003000000000000113, '东海医院_技术方案_v3.docx', 3, 512000, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '技术标主方案，含系统架构和实施计划', 'seed', 'seed', 'DOC-BID-2026-0002', 'APPROVED', 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', now() - interval '7 days', null, false, true, null),
    (3004000000000000003, 3003000000000000111, '东海医院_招标文件.zip', 1, 204800, 'application/zip', 3002000000000000002, 3001000000000000021, 1, '招标公告与附件原件', 'seed', 'seed', 'DOC-BID-2026-0003', 'APPROVED', 'BID', 'TENDER', 'PUBLIC', 'UPLOAD', now() - interval '12 days', null, false, true, null),
    (3004000000000000004, 3003000000000000114, '东海医院_澄清答疑汇总.pdf', 1, 184320, 'application/pdf', 3002000000000000002, 3001000000000000021, 1, '招标人澄清与内部答疑汇总', 'seed', 'seed', 'DOC-BID-2026-0004', 'READY_SUBMIT', 'BID', 'CLARIFICATION', 'INTERNAL', 'MANUAL', now() - interval '3 days', null, false, true, null),
    (3004000000000000005, 3003000000000000121, '星河数据中心_商务偏离表.xlsx', 1, 163840, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 3002000000000000003, 3001000000000000021, 1, '商务条款偏离说明', 'seed', 'seed', 'DOC-BID-2026-0005', 'REJECTED', 'BID', 'BUSINESS', 'SENSITIVE', 'UPLOAD', now() - interval '5 days', null, false, true, '保证金到账证明未补充'),
    (3004000000000000006, 3003000000000000122, '星河数据中心_机房扩容方案.docx', 2, 430080, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '数据中心机房扩容技术方案', 'seed', 'seed', 'DOC-BID-2026-0006', 'APPROVING', 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', now() - interval '4 days', null, false, true, null),
    (3004000000000000007, 3003000000000000131, '城轨5号线_弱电集成初步方案.docx', 1, 389120, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '轨道交通弱电系统初稿', 'seed', 'seed', 'DOC-BID-2026-0007', 'READY_SUBMIT', 'BID', 'TECHNICAL', 'SECRET', 'MANUAL', now() - interval '2 days', null, false, true, null),
    (3004000000000000008, 3003000000000000141, '智慧园区_投标授权书.pdf', 1, 122880, 'application/pdf', 3002000000000000008, 3001000000000000022, 1, '投标代表授权书', 'seed', 'seed', 'DOC-BID-2026-0008', 'APPROVED', 'BID', 'QUALIFICATION', 'INTERNAL', 'UPLOAD', now() - interval '6 days', now() + interval '180 days', true, true, null),
    (3004000000000000009, 3003000000000000151, '政务云运维服务方案.docx', 1, 368640, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '政务云年度运维服务方案', 'seed', 'seed', 'DOC-BID-2026-0009', 'APPROVED', 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', now() - interval '10 days', null, false, true, null),
    (3004000000000000010, 3003000000000000201, '营业执照_2026.pdf', 1, 153600, 'application/pdf', 3002000000000000005, 3001000000000000041, 1, '企业营业执照扫描件', 'seed', 'seed', 'DOC-QUAL-2026-0001', 'APPROVED', 'QUALIFICATION', 'LICENSE', 'PUBLIC', 'UPLOAD', now() - interval '30 days', now() + interval '700 days', true, true, null),
    (3004000000000000011, 3003000000000000202, '安全生产许可证_延期证明.pdf', 1, 155648, 'application/pdf', 3002000000000000005, 3001000000000000042, 1, '安全生产许可证延期证明', 'seed', 'seed', 'DOC-QUAL-2026-0002', 'APPROVED', 'QUALIFICATION', 'LICENSE', 'INTERNAL', 'UPLOAD', now() - interval '28 days', now() + interval '500 days', true, true, null),
    (3004000000000000012, 3003000000000000203, 'ISO9001质量管理体系证书.pdf', 1, 151552, 'application/pdf', 3002000000000000005, 3001000000000000041, 1, '质量管理体系证书', 'seed', 'seed', 'DOC-QUAL-2026-0003', 'APPROVED', 'QUALIFICATION', 'CERTIFICATE', 'PUBLIC', 'UPLOAD', now() - interval '40 days', now() + interval '320 days', true, true, null),
    (3004000000000000013, 3003000000000000203, 'ISO27001信息安全认证证书.pdf', 1, 150528, 'application/pdf', 3002000000000000005, 3001000000000000041, 1, '信息安全管理体系认证证书', 'seed', 'seed', 'DOC-QUAL-2026-0004', 'APPROVED', 'QUALIFICATION', 'CERTIFICATE', 'PUBLIC', 'UPLOAD', now() - interval '40 days', now() + interval '330 days', true, true, null),
    (3004000000000000014, 3003000000000000204, '项目经理一级建造师证书.pdf', 1, 145408, 'application/pdf', 3002000000000000005, 3001000000000000042, 1, '项目经理资格证书', 'seed', 'seed', 'DOC-QUAL-2026-0005', 'READY_SUBMIT', 'QUALIFICATION', 'CERTIFICATE', 'SENSITIVE', 'UPLOAD', now() - interval '11 days', now() + interval '260 days', true, true, null),
    (3004000000000000015, 3003000000000000301, '采购合同模板_标准版.docx', 2, 307200, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000007, 3001000000000000052, 1, '采购合同标准模板', 'seed', 'seed', 'DOC-CONTRACT-2026-0001', 'APPROVED', 'CONTRACT', 'TEMPLATE', 'INTERNAL', 'UPLOAD', now() - interval '22 days', null, false, true, null),
    (3004000000000000016, 3003000000000000302, '服务分包合同模板.docx', 1, 290816, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000007, 3001000000000000052, 1, '服务分包合同模板', 'seed', 'seed', 'DOC-CONTRACT-2026-0002', 'APPROVED', 'CONTRACT', 'TEMPLATE', 'INTERNAL', 'UPLOAD', now() - interval '21 days', null, false, true, null),
    (3004000000000000017, 3003000000000000303, '项目保密协议模板.docx', 1, 201728, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000007, 3001000000000000052, 1, '项目保密协议模板', 'seed', 'seed', 'DOC-CONTRACT-2026-0003', 'READY_SUBMIT', 'CONTRACT', 'TEMPLATE', 'INTERNAL', 'MANUAL', now() - interval '18 days', null, false, true, null),
    (3004000000000000018, 3003000000000000401, '智慧园区标准技术方案.pptx', 1, 512000, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 3002000000000000004, 3001000000000000031, 1, '售前通用方案演示材料', 'seed', 'seed', 'DOC-TECH-2026-0001', 'APPROVED', 'TECH_LIBRARY', 'SOLUTION', 'INTERNAL', 'UPLOAD', now() - interval '16 days', null, false, true, null),
    (3004000000000000019, 3003000000000000402, '边缘网关产品彩页.pdf', 1, 225280, 'application/pdf', 3002000000000000004, 3001000000000000032, 1, '边缘网关产品介绍', 'seed', 'seed', 'DOC-TECH-2026-0002', 'APPROVED', 'TECH_LIBRARY', 'PRODUCT', 'PUBLIC', 'UPLOAD', now() - interval '15 days', null, false, true, null),
    (3004000000000000020, 3003000000000000403, '施工组织设计模板.docx', 1, 337920, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '通用施工组织设计模板', 'seed', 'seed', 'DOC-TECH-2026-0003', 'APPROVED', 'TECH_LIBRARY', 'METHOD', 'INTERNAL', 'UPLOAD', now() - interval '14 days', null, false, true, null),
    (3004000000000000021, 3003000000000000501, '华东重点客户跟进清单.xlsx', 1, 204800, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 3002000000000000008, 3001000000000000022, 1, '客户跟进和商机阶段清单', 'seed', 'seed', 'DOC-SHARE-2026-0001', 'INCOMPLETE', 'MARKET', 'CUSTOMER', 'SENSITIVE', 'MANUAL', now() - interval '8 days', null, false, false, null),
    (3004000000000000022, 3003000000000000502, '技术中心投标支持排班表.xlsx', 1, 112640, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 3002000000000000004, 3001000000000000031, 1, '售前支持排班安排', 'seed', 'seed', 'DOC-SHARE-2026-0002', 'APPROVED', 'TECH_LIBRARY', 'PLAN', 'INTERNAL', 'UPLOAD', now() - interval '7 days', null, false, true, null),
    (3004000000000000023, 3003000000000000504, '投标保证金办理指引.pdf', 1, 132096, 'application/pdf', 3002000000000000006, 3001000000000000051, 1, '保证金办理和退还流程', 'seed', 'seed', 'DOC-SHARE-2026-0003', 'APPROVED', 'FINANCE', 'GUIDE', 'INTERNAL', 'UPLOAD', now() - interval '9 days', null, false, true, null),
    (3004000000000000024, 3003000000000000602, '2025年度投标复盘报告.pdf', 1, 256000, 'application/pdf', 3002000000000000009, 3001000000000000011, 1, '年度投标经验复盘', 'seed', 'seed', 'DOC-ARCHIVE-2025-0001', 'APPROVED', 'ARCHIVE', 'REPORT', 'INTERNAL', 'UPLOAD', now() - interval '60 days', null, false, true, null);

INSERT INTO doc_document_version (id, document_id, version_no, storage_key, size, mime_type, original_filename, content_hash, uploaded_by_user_id, change_log, created_by, updated_by, approval_status, approved_at, rejected_reason)
SELECT 3004100000000000000 + row_number() OVER (ORDER BY d.id),
       d.id,
       1,
       '2026/realistic/' || d.document_no || '-v1',
       d.latest_size,
       d.latest_mime,
       d.name,
       'seed-hash-' || d.document_no || '-v1',
       d.owner_user_id,
       '首版资料上传',
       'seed',
       'seed',
       CASE WHEN d.document_status IN ('APPROVED', 'READY_SUBMIT', 'APPROVING') THEN 'APPROVED' WHEN d.document_status = 'REJECTED' THEN 'REJECTED' ELSE 'PENDING' END,
       CASE WHEN d.document_status IN ('APPROVED', 'READY_SUBMIT', 'APPROVING') THEN now() - interval '6 days' ELSE null END,
       CASE WHEN d.document_status = 'REJECTED' THEN d.invalid_reason ELSE null END
FROM doc_document d;

INSERT INTO doc_document_version (id, document_id, version_no, storage_key, size, mime_type, original_filename, content_hash, uploaded_by_user_id, change_log, created_by, updated_by, approval_status, approved_at, rejected_reason)
VALUES
    (3004100000000000101, 3004000000000000001, 2, '2026/realistic/DOC-BID-2026-0001-v2', 358400, 'application/pdf', '东海医院_商务响应文件.pdf', 'seed-hash-DOC-BID-2026-0001-v2', 3002000000000000003, '补充报价和资质附件', 'seed', 'seed', 'APPROVED', now() - interval '4 days', null),
    (3004100000000000102, 3004000000000000002, 2, '2026/realistic/DOC-BID-2026-0002-v2', 421888, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '东海医院_技术方案_v2.docx', 'seed-hash-DOC-BID-2026-0002-v2', 3002000000000000004, '补充实施计划', 'seed', 'seed', 'APPROVED', now() - interval '5 days', null),
    (3004100000000000103, 3004000000000000002, 3, '2026/realistic/DOC-BID-2026-0002-v3', 512000, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '东海医院_技术方案_v3.docx', 'seed-hash-DOC-BID-2026-0002-v3', 3002000000000000004, '根据澄清意见修订', 'seed', 'seed', 'APPROVED', now() - interval '3 days', null),
    (3004100000000000104, 3004000000000000006, 2, '2026/realistic/DOC-BID-2026-0006-v2', 430080, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '星河数据中心_机房扩容方案.docx', 'seed-hash-DOC-BID-2026-0006-v2', 3002000000000000004, '补充能耗评估章节', 'seed', 'seed', 'PENDING', null, null),
    (3004100000000000105, 3004000000000000015, 2, '2026/realistic/DOC-CONTRACT-2026-0001-v2', 307200, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '采购合同模板_标准版.docx', 'seed-hash-DOC-CONTRACT-2026-0001-v2', 3002000000000000007, '法务审查后定稿', 'seed', 'seed', 'APPROVED', now() - interval '15 days', null);

INSERT INTO doc_document_tag (id, document_id, tag_id, created_at, created_by, deleted)
VALUES
    (3004500000000000001, 3004000000000000001, 3003400000000000001, now(), 'seed', false),
    (3004500000000000002, 3004000000000000002, 3003400000000000002, now(), 'seed', false),
    (3004500000000000003, 3004000000000000010, 3003400000000000003, now(), 'seed', false),
    (3004500000000000004, 3004000000000000015, 3003400000000000004, now(), 'seed', false),
    (3004500000000000005, 3004000000000000024, 3003400000000000005, now(), 'seed', false);

INSERT INTO doc_download_log (id, document_id, version_no, user_id, download_time, ip_address, user_agent)
VALUES
    (3004300000000000001, 3004000000000000001, 2, 3002000000000000002, now() - interval '1 hour', '127.0.0.1', 'Chrome Dev'),
    (3004300000000000002, 3004000000000000002, 3, 3002000000000000004, now() - interval '2 hours', '127.0.0.1', 'Chrome Dev'),
    (3004300000000000003, 3004000000000000008, 1, 3002000000000000008, now() - interval '4 hours', '127.0.0.1', 'Chrome Dev'),
    (3004300000000000004, 3004000000000000015, 2, 3002000000000000006, now() - interval '5 hours', '127.0.0.1', 'Chrome Dev'),
    (3004300000000000005, 3004000000000000018, 1, 3002000000000000008, now() - interval '9 hours', '127.0.0.1', 'Chrome Dev');

INSERT INTO doc_search_history (id, user_id, keyword, folder_id, result_count, search_time)
VALUES
    (3004200000000000001, 3002000000000000002, '东海医院', 3003000000000000101, 4, now() - interval '2 hours'),
    (3004200000000000002, 3002000000000000003, '商务响应', 3003000000000000112, 1, now() - interval '3 hours'),
    (3004200000000000003, 3002000000000000004, '技术方案', null, 6, now() - interval '4 hours'),
    (3004200000000000004, 3002000000000000005, 'ISO9001', 3003000000000000203, 1, now() - interval '5 hours'),
    (3004200000000000005, 3002000000000000007, '合同模板', 3003000000000000301, 3, now() - interval '6 hours');

-- 项目、项目成员和项目清单。
INSERT INTO bid_project (id, project_no, project_name, tender_unit, owner_dept_id, project_type, project_stage, project_status, bid_deadline, folder_id, created_by, updated_by, deleted)
VALUES
    (3005000000000000001, 'ZHGL-202606-001', '东海医院智能化改造项目', '东海市第一人民医院', 3001000000000000021, 'SYSTEM_INTEGRATION', 'REVIEWING', 'NORMAL', now() + interval '12 days', 3003000000000000101, 'seed', 'seed', false),
    (3005000000000000002, 'ZHGL-202606-002', '星河数据中心扩容项目', '星河云计算有限公司', 3001000000000000021, 'DATA_CENTER', 'COLLECTING', 'NORMAL', now() + interval '20 days', 3003000000000000102, 'seed', 'seed', false),
    (3005000000000000003, 'ZHGL-202606-003', '城轨5号线弱电集成项目', '华北城市轨道交通集团', 3001000000000000023, 'RAIL_TRANSIT', 'COMPILING', 'NORMAL', now() + interval '28 days', 3003000000000000103, 'seed', 'seed', false),
    (3005000000000000004, 'ZHGL-202606-004', '智慧园区平台建设项目', '临港科技园运营有限公司', 3001000000000000022, 'SMART_PARK', 'WAITING_SUBMIT', 'NORMAL', now() + interval '6 days', 3003000000000000104, 'seed', 'seed', false),
    (3005000000000000005, 'ZHGL-202605-009', '政务云运维服务项目', '市政务云管理中心', 3001000000000000021, 'OPS_SERVICE', 'ARCHIVED', 'ARCHIVED', now() - interval '12 days', 3003000000000000105, 'seed', 'seed', false);

INSERT INTO bid_project_member (id, project_id, user_id, member_role, created_by, deleted)
VALUES
    (3005100000000000001, 3005000000000000001, 3002000000000000002, 'OWNER', 'seed', false),
    (3005100000000000002, 3005000000000000001, 3002000000000000003, 'MATERIAL_OWNER', 'seed', false),
    (3005100000000000003, 3005000000000000001, 3002000000000000004, 'MEMBER', 'seed', false),
    (3005100000000000004, 3005000000000000001, 3002000000000000006, 'MEMBER', 'seed', false),
    (3005100000000000005, 3005000000000000002, 3002000000000000002, 'OWNER', 'seed', false),
    (3005100000000000006, 3005000000000000002, 3002000000000000003, 'MATERIAL_OWNER', 'seed', false),
    (3005100000000000007, 3005000000000000002, 3002000000000000004, 'MEMBER', 'seed', false),
    (3005100000000000008, 3005000000000000003, 3002000000000000008, 'OWNER', 'seed', false),
    (3005100000000000009, 3005000000000000003, 3002000000000000004, 'MEMBER', 'seed', false),
    (3005100000000000010, 3005000000000000004, 3002000000000000008, 'OWNER', 'seed', false),
    (3005100000000000011, 3005000000000000004, 3002000000000000007, 'MEMBER', 'seed', false),
    (3005100000000000012, 3005000000000000005, 3002000000000000009, 'OWNER', 'seed', false);

INSERT INTO bid_project_no_sequence (id, dept_id, biz_date, current_seq)
VALUES
    (3005200000000000001, 3001000000000000021, CURRENT_DATE, 2),
    (3005200000000000002, 3001000000000000022, CURRENT_DATE, 1),
    (3005200000000000003, 3001000000000000023, CURRENT_DATE, 1);

INSERT INTO bid_checklist_template (id, template_name, project_type, enabled, created_by, updated_by, deleted)
VALUES
    (3005300000000000001, '系统集成项目投标资料清单', 'SYSTEM_INTEGRATION', true, 'seed', 'seed', false),
    (3005300000000000002, '数据中心项目投标资料清单', 'DATA_CENTER', true, 'seed', 'seed', false),
    (3005300000000000003, '运维服务项目投标资料清单', 'OPS_SERVICE', true, 'seed', 'seed', false);

INSERT INTO bid_checklist_template_item (id, template_id, item_name, description, required, business_category, tender_structure_category, suggested_sensitive_level, allowed_source, allowed_file_types, min_count, max_count, sort_order, deleted)
VALUES
    (3005310000000000001, 3005300000000000001, '招标文件原件', '招标公告、招标文件和附件', true, 'BID', 'TENDER', 'PUBLIC', 'UPLOAD', 'pdf,zip', 1, 3, 1, false),
    (3005310000000000002, 3005300000000000001, '商务响应文件', '商务条款响应和报价说明', true, 'BID', 'BUSINESS', 'INTERNAL', 'UPLOAD', 'pdf,docx,xlsx', 1, 2, 2, false),
    (3005310000000000003, 3005300000000000001, '技术方案', '总体技术方案和实施计划', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pptx,pdf', 1, 3, 3, false),
    (3005310000000000004, 3005300000000000001, '资质证照', '营业执照、体系认证和授权书', true, 'QUALIFICATION', 'CERTIFICATE', 'INTERNAL', 'UPLOAD', 'pdf', 2, 6, 4, false),
    (3005310000000000005, 3005300000000000001, '投标保证金资料', '保证金办理凭证和财务说明', false, 'FINANCE', 'BUSINESS', 'SENSITIVE', 'UPLOAD', 'pdf,xlsx', 1, 2, 5, false),
    (3005310000000000006, 3005300000000000002, '商务偏离表', '商务条款偏离说明', true, 'BID', 'BUSINESS', 'SENSITIVE', 'UPLOAD', 'xlsx,pdf', 1, 1, 1, false),
    (3005310000000000007, 3005300000000000002, '机房扩容技术方案', '扩容架构、施工计划和能耗评估', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pdf', 1, 2, 2, false),
    (3005310000000000008, 3005300000000000003, '服务方案', '服务范围、响应 SLA 和团队配置', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pdf', 1, 2, 1, false);

INSERT INTO bid_project_checklist_item (id, project_id, template_item_id, item_name, description, required, business_category, tender_structure_category, sensitive_level, allowed_source, allowed_file_types, min_count, max_count, deadline, owner_user_id, status, sort_order, deleted)
VALUES
    (3005400000000000001, 3005000000000000001, 3005310000000000001, '招标文件原件', '东海医院招标文件和附件', true, 'BID', 'TENDER', 'PUBLIC', 'UPLOAD', 'pdf,zip', 1, 3, now() + interval '5 days', 3002000000000000002, 'COMPLETE', 1, false),
    (3005400000000000002, 3005000000000000001, 3005310000000000002, '商务响应文件', '东海医院商务响应', true, 'BID', 'BUSINESS', 'INTERNAL', 'UPLOAD', 'pdf,docx,xlsx', 1, 2, now() + interval '7 days', 3002000000000000003, 'COMPLETE', 2, false),
    (3005400000000000003, 3005000000000000001, 3005310000000000003, '技术方案', '东海医院技术方案', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pptx,pdf', 1, 3, now() + interval '7 days', 3002000000000000004, 'COMPLETE', 3, false),
    (3005400000000000004, 3005000000000000001, 3005310000000000004, '资质证照', '营业执照、体系认证和授权书', true, 'QUALIFICATION', 'CERTIFICATE', 'INTERNAL', 'UPLOAD', 'pdf', 2, 6, now() + interval '8 days', 3002000000000000005, 'PENDING_REVIEW', 4, false),
    (3005400000000000005, 3005000000000000001, 3005310000000000005, '投标保证金资料', '保证金办理凭证', false, 'FINANCE', 'BUSINESS', 'SENSITIVE', 'UPLOAD', 'pdf,xlsx', 1, 2, now() + interval '9 days', 3002000000000000006, 'PENDING_COLLECT', 5, false),
    (3005400000000000006, 3005000000000000002, 3005310000000000006, '商务偏离表', '星河数据中心商务偏离说明', true, 'BID', 'BUSINESS', 'SENSITIVE', 'UPLOAD', 'xlsx,pdf', 1, 1, now() + interval '10 days', 3002000000000000003, 'NEED_SUPPLEMENT', 1, false),
    (3005400000000000007, 3005000000000000002, 3005310000000000007, '机房扩容技术方案', '星河数据中心扩容技术方案', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pdf', 1, 2, now() + interval '12 days', 3002000000000000004, 'PENDING_REVIEW', 2, false),
    (3005400000000000008, 3005000000000000005, 3005310000000000008, '服务方案', '政务云运维服务方案', true, 'BID', 'TECHNICAL', 'INTERNAL', 'UPLOAD', 'docx,pdf', 1, 2, now() - interval '20 days', 3002000000000000004, 'ARCHIVED', 1, false);

INSERT INTO bid_project_checklist_document (id, checklist_item_id, document_id, version_no, bind_type, created_by, deleted)
VALUES
    (3005500000000000001, 3005400000000000001, 3004000000000000003, 1, 'PRIMARY', 'seed', false),
    (3005500000000000002, 3005400000000000002, 3004000000000000001, 2, 'PRIMARY', 'seed', false),
    (3005500000000000003, 3005400000000000003, 3004000000000000002, 3, 'PRIMARY', 'seed', false),
    (3005500000000000004, 3005400000000000004, 3004000000000000010, 1, 'SUPPORTING', 'seed', false),
    (3005500000000000005, 3005400000000000004, 3004000000000000012, 1, 'SUPPORTING', 'seed', false),
    (3005500000000000006, 3005400000000000006, 3004000000000000005, 1, 'PRIMARY', 'seed', false),
    (3005500000000000007, 3005400000000000007, 3004000000000000006, 2, 'PRIMARY', 'seed', false),
    (3005500000000000008, 3005400000000000008, 3004000000000000009, 1, 'PRIMARY', 'seed', false);

INSERT INTO bid_project_archive_record (id, project_id, archive_no, archive_status, archived_at, archived_by, archive_reason, checklist_total, checklist_complete, document_total, snapshot_hash, remark, deleted, created_by, updated_by)
VALUES
    (3005600000000000001, 3005000000000000005, 'ARCH-202605-009', 'ARCHIVED', now() - interval '8 days', 3002000000000000009, '项目已完成并提交归档', 1, 1, 1, 'archive-hash-3005000000000000005', '政务云运维服务项目归档记录', false, 'seed', 'seed');

INSERT INTO bid_project_archive_checklist_snapshot (id, archive_record_id, project_id, checklist_item_id, template_item_id, item_name, required_flag, item_status, bound_document_count, snapshot_json, deleted, created_by, updated_by)
VALUES
    (3005610000000000001, 3005600000000000001, 3005000000000000005, 3005400000000000008, 3005310000000000008, '服务方案', true, 'ARCHIVED', 1, '{"project":"政务云运维服务项目"}', false, 'seed', 'seed');

INSERT INTO bid_project_archive_document_snapshot (id, archive_record_id, project_id, checklist_item_id, document_id, version_no, document_name, document_status, version_status, expire_at, storage_type, file_size, snapshot_json, deleted, created_by, updated_by)
VALUES
    (3005620000000000001, 3005600000000000001, 3005000000000000005, 3005400000000000008, 3004000000000000009, 1, '政务云运维服务方案.docx', 'APPROVED', 'APPROVED', null, 'local', 368640, '{"documentNo":"DOC-BID-2026-0009"}', false, 'seed', 'seed');

-- 审批定义、节点、实例和任务，覆盖待审、通过、驳回、撤回、终止。
INSERT INTO wf_approval_definition (id, name, scenario, biz_module, biz_type, dept_id, business_category, version, enabled, deleted, created_by, updated_by)
VALUES
    (3009000000000000001, '投标资料审批流程', 'DOCUMENT_APPROVAL', 'DOCUMENT', 'DOCUMENT', 3001000000000000001, 'BID', 1, true, false, 'seed', 'seed'),
    (3009000000000000002, '投标资料版本审批流程', 'DOCUMENT_VERSION_APPROVAL', 'DOCUMENT', 'DOCUMENT_VERSION', 3001000000000000001, 'BID', 1, true, false, 'seed', 'seed'),
    (3009000000000000003, '投标清单项审批流程', 'CHECKLIST_ITEM_APPROVAL', 'PROJECT', 'CHECKLIST_ITEM', 3001000000000000001, 'BID', 1, true, false, 'seed', 'seed');

INSERT INTO wf_approval_node (id, definition_id, node_code, node_name, node_type, approve_mode, assignee_type, assignee_value, sort_order, next_node_code, reject_to_node_code, deleted, created_by, updated_by)
VALUES
    (3009010000000000001, 3009000000000000001, 'START', '提交', 'START', null, null, null, 1, 'DEPT_REVIEW', null, false, 'seed', 'seed'),
    (3009010000000000002, 3009000000000000001, 'DEPT_REVIEW', '部门审核', 'APPROVAL', 'ANY', 'USER', '3002000000000000002', 2, 'END', 'START', false, 'seed', 'seed'),
    (3009010000000000003, 3009000000000000001, 'END', '结束', 'END', null, null, null, 3, null, null, false, 'seed', 'seed'),
    (3009010000000000004, 3009000000000000002, 'START', '提交', 'START', null, null, null, 1, 'TECH_REVIEW', null, false, 'seed', 'seed'),
    (3009010000000000005, 3009000000000000002, 'TECH_REVIEW', '技术负责人审核', 'APPROVAL', 'ANY', 'USER', '3002000000000000004', 2, 'END', 'START', false, 'seed', 'seed'),
    (3009010000000000006, 3009000000000000002, 'END', '结束', 'END', null, null, null, 3, null, null, false, 'seed', 'seed'),
    (3009010000000000007, 3009000000000000003, 'START', '提交', 'START', null, null, null, 1, 'OWNER_REVIEW', null, false, 'seed', 'seed'),
    (3009010000000000008, 3009000000000000003, 'OWNER_REVIEW', '项目负责人审核', 'APPROVAL', 'ANY', 'USER', '3002000000000000002', 2, 'END', 'START', false, 'seed', 'seed'),
    (3009010000000000009, 3009000000000000003, 'END', '结束', 'END', null, null, null, 3, null, null, false, 'seed', 'seed');

INSERT INTO wf_approval_condition (id, definition_id, node_id, condition_code, field_name, operator, compare_value, target_node_code, sort_order, deleted, created_by, updated_by)
VALUES
    (3009020000000000001, 3009000000000000001, 3009010000000000002, 'SECRET_DOC_ESCALATE', 'sensitiveLevel', 'EQ', 'SECRET', 'DEPT_REVIEW', 1, false, 'seed', 'seed');

INSERT INTO wf_approval_instance (id, document_id, submitter_user_id, status, submit_comment, submitted_at, completed_at, deleted, created_by, updated_by, biz_module, biz_type, biz_id, scenario, finished_at, version_no, definition_id, definition_version, current_node_id, current_node_code)
VALUES
    (3009100000000000001, 3004000000000000006, 3002000000000000004, 'PENDING', '请审核星河数据中心扩容方案第二版', now() - interval '12 hours', null, false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT_VERSION', 3004000000000000006, 'DOCUMENT_VERSION_APPROVAL', null, 2, 3009000000000000002, 1, 3009010000000000005, 'TECH_REVIEW'),
    (3009100000000000002, 3004000000000000001, 3002000000000000003, 'APPROVED', '商务响应文件已补齐，请审核', now() - interval '5 days', now() - interval '4 days', false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000001, 'DOCUMENT_APPROVAL', now() - interval '4 days', null, 3009000000000000001, 1, 3009010000000000003, 'END'),
    (3009100000000000003, 3004000000000000005, 3002000000000000003, 'REJECTED', '商务偏离表提交审核', now() - interval '4 days', now() - interval '3 days', false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000005, 'DOCUMENT_APPROVAL', now() - interval '3 days', null, 3009000000000000001, 1, 3009010000000000003, 'END'),
    (3009100000000000004, 3004000000000000007, 3002000000000000004, 'WITHDRAWN', '轨交项目初稿先走内部评审', now() - interval '2 days', now() - interval '1 day 20 hours', false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000007, 'DOCUMENT_APPROVAL', now() - interval '1 day 20 hours', null, 3009000000000000001, 1, 3009010000000000002, 'DEPT_REVIEW'),
    (3009100000000000005, 3004000000000000017, 3002000000000000007, 'TERMINATED', '保密协议模板暂缓启用', now() - interval '3 days', now() - interval '2 days 20 hours', false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000017, 'DOCUMENT_APPROVAL', now() - interval '2 days 20 hours', null, 3009000000000000001, 1, 3009010000000000002, 'DEPT_REVIEW'),
    (3009100000000000006, null, 3002000000000000003, 'APPROVED', '东海医院资质清单项审核', now() - interval '2 days', now() - interval '1 day', false, 'seed', 'seed', 'PROJECT', 'CHECKLIST_ITEM', 3005400000000000004, 'CHECKLIST_ITEM_APPROVAL', now() - interval '1 day', null, 3009000000000000003, 1, 3009010000000000009, 'END'),
    (3009100000000000007, 3004000000000000004, 3002000000000000002, 'PENDING', '东海医院澄清答疑已整理，请投标管理员确认', now() - interval '6 hours', null, false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000004, 'DOCUMENT_APPROVAL', null, null, 3009000000000000001, 1, 3009010000000000002, 'DEPT_REVIEW'),
    (3009100000000000008, 3004000000000000008, 3002000000000000008, 'PENDING', '智慧园区投标授权书需要法务复核', now() - interval '18 hours', null, false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000008, 'DOCUMENT_APPROVAL', null, null, 3009000000000000001, 1, 3009010000000000002, 'DEPT_REVIEW'),
    (3009100000000000009, null, 3002000000000000006, 'PENDING', '东海医院投标保证金资料已提交，请项目负责人审核', now() - interval '4 hours', null, false, 'seed', 'seed', 'PROJECT', 'CHECKLIST_ITEM', 3005400000000000005, 'CHECKLIST_ITEM_APPROVAL', null, null, 3009000000000000003, 1, 3009010000000000008, 'OWNER_REVIEW'),
    (3009100000000000010, 3004000000000000014, 3002000000000000005, 'PENDING', '项目经理证书用于东海医院投标，请加签法务确认授权范围', now() - interval '2 hours', null, false, 'seed', 'seed', 'DOCUMENT', 'DOCUMENT', 3004000000000000014, 'DOCUMENT_APPROVAL', null, null, 3009000000000000001, 1, 3009010000000000002, 'DEPT_REVIEW');

INSERT INTO wf_approval_task (id, instance_id, document_id, approver_user_id, status, comment, handled_at, deleted, created_by, updated_by, definition_id, node_id, node_code, transferred_from_task_id, add_sign)
VALUES
    (3009110000000000001, 3009100000000000001, 3004000000000000006, 3002000000000000004, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000002, 3009010000000000005, 'TECH_REVIEW', null, false),
    (3009110000000000002, 3009100000000000002, 3004000000000000001, 3002000000000000002, 'APPROVED', '商务响应完整，同意提交', now() - interval '4 days', false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000003, 3009100000000000003, 3004000000000000005, 3002000000000000002, 'REJECTED', '请补充保证金到账证明', now() - interval '3 days', false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000004, 3009100000000000004, 3004000000000000007, 3002000000000000008, 'WITHDRAWN', '提交人撤回修改', now() - interval '1 day 20 hours', false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000005, 3009100000000000005, 3004000000000000017, 3002000000000000007, 'TERMINATED', '流程终止', now() - interval '2 days 20 hours', false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000006, 3009100000000000006, null, 3002000000000000002, 'APPROVED', '资质文件齐全', now() - interval '1 day', false, 'seed', 'seed', 3009000000000000003, 3009010000000000008, 'OWNER_REVIEW', null, false),
    (3009110000000000007, 3009100000000000007, 3004000000000000004, 3002000000000000003, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000008, 3009100000000000008, 3004000000000000008, 3002000000000000002, 'TRANSFERRED', '授权书需法务确认签章范围，转交法务复核', now() - interval '10 hours', false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000009, 3009100000000000008, 3004000000000000008, 3002000000000000007, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 3009110000000000008, false),
    (3009110000000000010, 3009100000000000009, null, 3002000000000000002, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000003, 3009010000000000008, 'OWNER_REVIEW', null, false),
    (3009110000000000011, 3009100000000000010, 3004000000000000014, 3002000000000000002, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, false),
    (3009110000000000012, 3009100000000000010, 3004000000000000014, 3002000000000000007, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 3009110000000000011, true),
    (3009110000000000013, 3009100000000000003, 3004000000000000005, 3002000000000000006, 'PENDING', null, null, false, 'seed', 'seed', 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', null, true);

INSERT INTO wf_approval_task_candidate (id, task_id, instance_id, definition_id, node_id, node_code, candidate_type, candidate_value, candidate_user_id, resolved, deleted, created_by, updated_by)
VALUES
    (3009120000000000001, 3009110000000000001, 3009100000000000001, 3009000000000000002, 3009010000000000005, 'TECH_REVIEW', 'USER', '3002000000000000004', 3002000000000000004, true, false, 'seed', 'seed'),
    (3009120000000000002, 3009110000000000002, 3009100000000000002, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000002', 3002000000000000002, true, false, 'seed', 'seed'),
    (3009120000000000003, 3009110000000000003, 3009100000000000003, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000002', 3002000000000000002, true, false, 'seed', 'seed'),
    (3009120000000000004, 3009110000000000007, 3009100000000000007, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000003', 3002000000000000003, true, false, 'seed', 'seed'),
    (3009120000000000005, 3009110000000000009, 3009100000000000008, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000007', 3002000000000000007, true, false, 'seed', 'seed'),
    (3009120000000000006, 3009110000000000010, 3009100000000000009, 3009000000000000003, 3009010000000000008, 'OWNER_REVIEW', 'USER', '3002000000000000002', 3002000000000000002, true, false, 'seed', 'seed'),
    (3009120000000000007, 3009110000000000011, 3009100000000000010, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000002', 3002000000000000002, true, false, 'seed', 'seed'),
    (3009120000000000008, 3009110000000000012, 3009100000000000010, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000007', 3002000000000000007, true, false, 'seed', 'seed'),
    (3009120000000000009, 3009110000000000013, 3009100000000000003, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'USER', '3002000000000000006', 3002000000000000006, true, false, 'seed', 'seed');

INSERT INTO wf_approval_action_log (id, instance_id, task_id, definition_id, node_id, node_code, action_type, action_user_id, action_comment, action_at, before_status, after_status, deleted, created_by, updated_by)
VALUES
    (3009130000000000001, 3009100000000000002, 3009110000000000002, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'APPROVE', 3002000000000000002, '商务响应完整，同意提交', now() - interval '4 days', 'PENDING', 'APPROVED', false, 'seed', 'seed'),
    (3009130000000000002, 3009100000000000003, 3009110000000000003, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'REJECT', 3002000000000000002, '请补充保证金到账证明', now() - interval '3 days', 'PENDING', 'REJECTED', false, 'seed', 'seed'),
    (3009130000000000003, 3009100000000000004, 3009110000000000004, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'WITHDRAW', 3002000000000000004, '提交人撤回修改', now() - interval '1 day 20 hours', 'PENDING', 'WITHDRAWN', false, 'seed', 'seed'),
    (3009130000000000004, 3009100000000000005, 3009110000000000005, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'TERMINATE', 3002000000000000001, '流程终止', now() - interval '2 days 20 hours', 'PENDING', 'TERMINATED', false, 'seed', 'seed'),
    (3009130000000000005, 3009100000000000006, 3009110000000000006, 3009000000000000003, 3009010000000000008, 'OWNER_REVIEW', 'APPROVE', 3002000000000000002, '资质文件齐全', now() - interval '1 day', 'PENDING', 'APPROVED', false, 'seed', 'seed'),
    (3009130000000000006, 3009100000000000008, 3009110000000000008, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'TRANSFER', 3002000000000000002, '授权书需法务确认签章范围，转交法务复核', now() - interval '10 hours', 'PENDING', 'PENDING', false, 'seed', 'seed'),
    (3009130000000000007, 3009100000000000010, 3009110000000000012, 3009000000000000001, 3009010000000000002, 'DEPT_REVIEW', 'ADD_SIGN', 3002000000000000002, '证书用于重点项目，请法务加签确认授权范围', now() - interval '90 minutes', 'PENDING', 'PENDING', false, 'seed', 'seed');

INSERT INTO doc_document_use_grant (id, document_id, version_no, project_id, applicant_id, grantee_id, approval_instance_id, grant_type, scenario, valid_from, valid_until, status, reason, deleted, created_by, updated_by)
VALUES
    (3004600000000000001, 3004000000000000008, 1, 3005000000000000004, 3002000000000000008, 3002000000000000007, 3009100000000000002, 'DOWNLOAD', 'PROJECT_BID', now() - interval '4 days', now() + interval '30 days', 'APPROVED', '投标授权书法务复核', false, 'seed', 'seed'),
    (3004600000000000002, 3004000000000000014, 1, 3005000000000000001, 3002000000000000003, 3002000000000000002, 3009100000000000006, 'PREVIEW', 'PROJECT_BID', now() - interval '1 day', now() + interval '14 days', 'APPROVED', '项目经理证书预览授权', false, 'seed', 'seed');

INSERT INTO sys_notification (id, receiver_user_id, type, title, content, biz_type, biz_id, read_flag, read_at, deleted, created_by)
VALUES
    (3008000000000000001, 3002000000000000004, 'WORKFLOW_TASK', '新的文档版本审批任务', '星河数据中心机房扩容方案第二版等待审核', 'DOCUMENT_VERSION', 3004000000000000006, false, null, false, 'seed'),
    (3008000000000000002, 3002000000000000003, 'WORKFLOW_RESULT', '审批结果', '东海医院商务响应文件已审批通过', 'DOCUMENT', 3004000000000000001, true, now() - interval '3 days', false, 'seed'),
    (3008000000000000003, 3002000000000000003, 'WORKFLOW_RESULT', '审批结果', '星河数据中心商务偏离表被驳回，请补充材料', 'DOCUMENT', 3004000000000000005, false, null, false, 'seed'),
    (3008000000000000004, 3002000000000000002, 'PROJECT_MEMBER', '你已加入投标项目', '你已加入东海医院智能化改造项目', 'PROJECT', 3005000000000000001, true, now() - interval '5 days', false, 'seed'),
    (3008000000000000005, 3002000000000000009, 'PROJECT_ARCHIVED', '投标项目已归档', '政务云运维服务项目已完成归档', 'PROJECT', 3005000000000000005, false, null, false, 'seed'),
    (3008000000000000006, 3002000000000000003, 'WORKFLOW_TASK', '新的澄清答疑审批任务', '东海医院澄清答疑汇总等待你确认', 'DOCUMENT', 3004000000000000004, false, null, false, 'seed'),
    (3008000000000000007, 3002000000000000007, 'WORKFLOW_TASK', '你收到一个转交审批任务', '智慧园区投标授权书已转交给你复核签章范围', 'DOCUMENT', 3004000000000000008, false, null, false, 'seed'),
    (3008000000000000008, 3002000000000000002, 'WORKFLOW_TASK', '新的清单项审批任务', '东海医院投标保证金资料等待项目负责人审核', 'CHECKLIST_ITEM', 3005400000000000005, false, null, false, 'seed'),
    (3008000000000000009, 3002000000000000002, 'WORKFLOW_TASK', '新的证书使用审批任务', '项目经理一级建造师证书用于东海医院投标，等待你审核', 'DOCUMENT', 3004000000000000014, false, null, false, 'seed'),
    (3008000000000000010, 3002000000000000007, 'WORKFLOW_TASK', '你收到一个加签审批任务', '项目经理证书使用申请需要你加签确认授权范围', 'DOCUMENT', 3004000000000000014, false, null, false, 'seed'),
    (3008000000000000011, 3002000000000000006, 'WORKFLOW_TASK', '请补充保证金资料说明', '星河数据中心商务偏离表已驳回，请同步更新保证金资料', 'DOCUMENT', 3004000000000000005, false, null, false, 'seed'),
    (3008000000000000012, 3002000000000000004, 'WORKFLOW_RESULT', '审批处理中', '星河数据中心机房扩容方案已进入技术负责人审核', 'DOCUMENT_VERSION', 3004000000000000006, true, now() - interval '6 hours', false, 'seed'),
    (3008000000000000013, 3002000000000000008, 'WORKFLOW_RESULT', '审批已转交', '智慧园区投标授权书已转交法务合规部复核', 'DOCUMENT', 3004000000000000008, true, now() - interval '8 hours', false, 'seed'),
    (3008000000000000014, 3002000000000000005, 'WORKFLOW_RESULT', '证书使用审批已加签', '项目经理证书使用申请已加签法务复核', 'DOCUMENT', 3004000000000000014, true, now() - interval '1 hour', false, 'seed');

INSERT INTO audit_operation_log (id, module_code, biz_type, biz_id, operation_type, operator_user_id, operator_dept_id, request_id, operation_time, before_data, after_data, extra_data, created_by)
VALUES
    (3004400000000000001, 'PROJECT', 'PROJECT', 3005000000000000001, 'CREATE', 3002000000000000002, 3001000000000000021, 'seed-project-001', now() - interval '9 days', null, '{"projectName":"东海医院智能化改造项目"}', '{"source":"seed"}', 'seed'),
    (3004400000000000002, 'DOCUMENT', 'DOCUMENT', 3004000000000000001, 'UPLOAD', 3002000000000000003, 3001000000000000021, 'seed-doc-001', now() - interval '8 days', null, '{"name":"东海医院_商务响应文件.pdf"}', '{"versionNo":2}', 'seed'),
    (3004400000000000003, 'DOCUMENT', 'DOCUMENT', 3004000000000000002, 'NEW_VERSION', 3002000000000000004, 3001000000000000031, 'seed-doc-002', now() - interval '7 days', '{"versionNo":2}', '{"versionNo":3}', '{"changeLog":"根据澄清意见修订"}', 'seed'),
    (3004400000000000004, 'DOCUMENT', 'DOCUMENT', 3004000000000000001, 'APPROVAL_APPROVE', 3002000000000000002, 3001000000000000021, 'seed-approval-001', now() - interval '4 days', '{"status":"PENDING"}', '{"status":"APPROVED"}', '{"instanceId":"3009100000000000002"}', 'seed'),
    (3004400000000000005, 'DOCUMENT', 'DOCUMENT', 3004000000000000005, 'APPROVAL_REJECT', 3002000000000000002, 3001000000000000021, 'seed-approval-002', now() - interval '3 days', '{"status":"PENDING"}', '{"status":"REJECTED"}', '{"reason":"请补充保证金到账证明"}', 'seed'),
    (3004400000000000006, 'FOLDER', 'FOLDER', 3003000000000000113, 'MANAGER_ADD', 3002000000000000001, 3001000000000000001, 'seed-folder-001', now() - interval '2 days', null, '{"userId":"3002000000000000004"}', '{"manageScope":"SELF_AND_DESCENDANTS"}', 'seed'),
    (3004400000000000007, 'PROJECT', 'PROJECT', 3005000000000000005, 'UPDATE', 3002000000000000009, 3001000000000000011, 'seed-project-002', now() - interval '8 days', '{"projectStage":"SUBMITTED"}', '{"projectStage":"ARCHIVED"}', '{"archiveNo":"ARCH-202605-009"}', 'seed'),
    (3004400000000000008, 'DOCUMENT', 'DOCUMENT', 3004000000000000008, 'APPROVAL_TRANSFER', 3002000000000000002, 3001000000000000021, 'seed-approval-003', now() - interval '10 hours', '{"approver":"3002000000000000002"}', '{"approver":"3002000000000000007"}', '{"reason":"授权书签章范围需法务复核"}', 'seed'),
    (3004400000000000009, 'DOCUMENT', 'DOCUMENT', 3004000000000000014, 'APPROVAL_ADD_SIGN', 3002000000000000002, 3001000000000000021, 'seed-approval-004', now() - interval '90 minutes', null, '{"addSignUser":"3002000000000000007"}', '{"reason":"重点项目证书授权范围确认"}', 'seed');

SELECT setval('sys_department_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_department), 1), true);
SELECT setval('sys_user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_user), 1), true);
SELECT setval('sys_user_role_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_user_role), 1), true);

COMMIT;
