-- 本地开发库真实感演示数据种子脚本。
-- 仅用于 bid_doc_system 本地开发库；会清理业务测试数据并重建一套覆盖前端联调场景的数据。
-- 不修改 flyway_schema_history，不依赖 Flyway 自动迁移。

BEGIN;

-- 先清理依赖端和业务日志，避免旧测试数据继续污染前端页面。
DELETE FROM doc_download_log;
DELETE FROM doc_search_history;
DELETE FROM audit_operation_log;
DELETE FROM doc_folder_favorite;
DELETE FROM doc_folder_grant;
DELETE FROM doc_folder_manager;
DELETE FROM doc_document_version;
DELETE FROM doc_document;
DELETE FROM doc_folder;
DELETE FROM sys_user_role;
DELETE FROM sys_user;
DELETE FROM sys_department;

-- 保证基础角色存在；保留已有 sys_role 表结构和语义。
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

-- 部门：覆盖一级部门、二级部门、部门负责人和前端部门选择场景。
INSERT INTO sys_department (id, name, parent_id, level, manager_user_id, status, deleted, created_by, updated_by, remark, extension_data)
VALUES
    (3001000000000000001, '综合管理部', null, 1, null, 1, false, 'seed', 'seed', '负责行政、人事与公司级制度管理', '{"office":"A座12层"}'),
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
    (3001000000000000052, '法务合规部', 3001000000000000005, 2, null, 1, false, 'seed', 'seed', '合同模板、授权文件与法律审查', null);

-- 用户：统一演示密码为 12345678；覆盖启用、停用、多角色、不同部门。
INSERT INTO sys_user (id, username, password, real_name, email, mobile, dept_id, job_level, status, deleted, last_login_time, login_count, created_by, updated_by, remark, extension_data)
VALUES
    (3002000000000000001, 'admin', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '系统管理员', 'admin@biddoc.local', '13800000001', 3001000000000000001, 1, 1, false, now() - interval '1 hour', 18, 'seed', 'seed', '本地演示超级管理员', '{"avatar":"admin"}'),
    (3002000000000000002, 'zhang_manager', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '张晨', 'zhang.chen@biddoc.local', '13800000002', 3001000000000000021, 1, 1, false, now() - interval '2 hours', 9, 'seed', 'seed', '市场经营部负责人', '{"title":"部门经理"}'),
    (3002000000000000003, 'li_folder_admin', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '李若溪', 'li.ruoxi@biddoc.local', '13800000003', 3001000000000000021, 2, 1, false, now() - interval '3 hours', 12, 'seed', 'seed', '投标文件夹管理员', '{"title":"投标专员"}'),
    (3002000000000000004, 'wang_engineer', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '王一鸣', 'wang.yiming@biddoc.local', '13800000004', 3001000000000000031, 3, 1, false, now() - interval '4 hours', 7, 'seed', 'seed', '技术方案工程师', '{"title":"方案工程师"}'),
    (3002000000000000005, 'chen_quality', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '陈书瑶', 'chen.shuyao@biddoc.local', '13800000005', 3001000000000000041, 2, 1, false, now() - interval '1 day', 5, 'seed', 'seed', '质量体系负责人', '{"title":"质量主管"}'),
    (3002000000000000006, 'zhao_finance', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '赵明远', 'zhao.mingyuan@biddoc.local', '13800000006', 3001000000000000051, 3, 1, false, now() - interval '5 hours', 4, 'seed', 'seed', '财务资料维护人', '{"title":"财务专员"}'),
    (3002000000000000007, 'sun_legal', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '孙嘉言', 'sun.jiayan@biddoc.local', '13800000007', 3001000000000000052, 3, 1, false, now() - interval '6 hours', 3, 'seed', 'seed', '合同与法务资料维护人', '{"title":"法务专员"}'),
    (3002000000000000008, 'he_sales', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '何景行', 'he.jingxing@biddoc.local', '13800000008', 3001000000000000022, 3, 1, false, now() - interval '8 hours', 6, 'seed', 'seed', '华东区域销售', '{"title":"客户经理"}'),
    (3002000000000000009, 'qian_archive', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '钱思源', 'qian.siyuan@biddoc.local', '13800000009', 3001000000000000011, 2, 1, false, now() - interval '10 hours', 2, 'seed', 'seed', '归档管理员', '{"title":"档案管理员"}'),
    (3002000000000000010, 'demo_disabled', '$2a$10$vvB22qKvMvDFUbI.IXtvaOZOfUn.LWcJN89xXw5akW6kE4k2T6moS', '林停用', 'disabled@biddoc.local', '13800000010', 3001000000000000012, 3, 0, false, null, 0, 'seed', 'seed', '用于验证禁用账号无法登录', '{"title":"停用账号"}');

UPDATE sys_department SET manager_user_id = 3002000000000000001 WHERE id = 3001000000000000001;
UPDATE sys_department SET manager_user_id = 3002000000000000002 WHERE id = 3001000000000000002;
UPDATE sys_department SET manager_user_id = 3002000000000000004 WHERE id = 3001000000000000003;
UPDATE sys_department SET manager_user_id = 3002000000000000005 WHERE id = 3001000000000000004;
UPDATE sys_department SET manager_user_id = 3002000000000000007 WHERE id = 3001000000000000005;

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

-- 文件夹树：覆盖根级、二级、三级、排序、继承权限开关和多部门所有权。
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
    (3003000000000000111, 3003000000000000101, '招标文件', '3003000000000000001,3003000000000000101,3003000000000000111', 2, 1, 3001000000000000021, 3002000000000000003, true, 1, '招标公告、澄清和附件', 'seed', 'seed'),
    (3003000000000000112, 3003000000000000101, '商务标', '3003000000000000001,3003000000000000101,3003000000000000112', 2, 2, 3001000000000000021, 3002000000000000003, true, 1, '报价、资质和商务响应', 'seed', 'seed'),
    (3003000000000000113, 3003000000000000101, '技术标', '3003000000000000001,3003000000000000101,3003000000000000113', 2, 3, 3001000000000000031, 3002000000000000004, true, 1, '技术方案和实施组织设计', 'seed', 'seed'),
    (3003000000000000114, 3003000000000000101, '澄清与答疑', '3003000000000000001,3003000000000000101,3003000000000000114', 2, 4, 3001000000000000021, 3002000000000000002, false, 1, '往来澄清、答疑和补遗文件', 'seed', 'seed'),
    (3003000000000000121, 3003000000000000102, '商务资料', '3003000000000000001,3003000000000000102,3003000000000000121', 2, 1, 3001000000000000021, 3002000000000000003, true, 1, '数据中心项目商务资料', 'seed', 'seed'),
    (3003000000000000122, 3003000000000000102, '技术方案', '3003000000000000001,3003000000000000102,3003000000000000122', 2, 2, 3001000000000000031, 3002000000000000004, true, 1, '数据中心扩容技术方案', 'seed', 'seed'),
    (3003000000000000131, 3003000000000000103, '初稿区', '3003000000000000001,3003000000000000103,3003000000000000131', 2, 1, 3001000000000000023, 3002000000000000008, true, 1, '轨交项目初稿资料', 'seed', 'seed'),
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

-- 文件夹管理员：覆盖 SELF 和 SELF_AND_DESCENDANTS。
INSERT INTO doc_folder_manager (id, folder_id, user_id, manage_scope, created_by, updated_by)
VALUES
    (3003100000000000001, 3003000000000000001, 3002000000000000003, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000002, 3003000000000000113, 3002000000000000004, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000003, 3003000000000000002, 3002000000000000005, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000004, 3003000000000000006, 3002000000000000009, 'SELF_AND_DESCENDANTS', 'seed', 'seed'),
    (3003100000000000005, 3003000000000000301, 3002000000000000007, 'SELF', 'seed', 'seed');

-- 授权：覆盖 USER、ROLE、DEPT 三类主体，以及多种权限码。
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
    (3003300000000000005, 3003000000000000602, 3002000000000000009, 'seed');

-- 文档和版本：覆盖列表、详情、搜索、热门、版本列表和下载日志页面。
INSERT INTO doc_document (id, folder_id, name, current_version_no, latest_size, latest_mime, owner_user_id, owner_dept_id, status, remark, created_by, updated_by)
VALUES
    (3004000000000000001, 3003000000000000112, '东海医院_商务响应文件.pdf', 2, 3584, 'application/pdf', 3002000000000000003, 3001000000000000021, 1, '最终报价前的商务响应文件', 'seed', 'seed'),
    (3004000000000000002, 3003000000000000113, '东海医院_技术方案_v3.docx', 3, 4096, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '技术标主方案，含系统架构和实施计划', 'seed', 'seed'),
    (3004000000000000003, 3003000000000000111, '东海医院_招标文件.zip', 1, 2048, 'application/zip', 3002000000000000002, 3001000000000000021, 1, '招标公告与附件原件', 'seed', 'seed'),
    (3004000000000000004, 3003000000000000201, '营业执照_2026.pdf', 1, 1536, 'application/pdf', 3002000000000000005, 3001000000000000041, 1, '企业营业执照扫描件', 'seed', 'seed'),
    (3004000000000000005, 3003000000000000203, 'ISO9001质量管理体系证书.pdf', 1, 1536, 'application/pdf', 3002000000000000005, 3001000000000000041, 1, '质量管理体系证书', 'seed', 'seed'),
    (3004000000000000006, 3003000000000000301, '采购合同模板_标准版.docx', 2, 3072, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000007, 3001000000000000052, 1, '采购合同标准模板', 'seed', 'seed'),
    (3004000000000000007, 3003000000000000401, '智慧园区标准技术方案.pptx', 1, 5120, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 3002000000000000004, 3001000000000000031, 1, '售前通用方案演示材料', 'seed', 'seed'),
    (3004000000000000008, 3003000000000000501, '华东重点客户跟进清单.xlsx', 1, 2048, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 3002000000000000008, 3001000000000000022, 1, '客户跟进和商机阶段清单', 'seed', 'seed'),
    (3004000000000000009, 3003000000000000602, '2025年度投标复盘报告.pdf', 1, 2560, 'application/pdf', 3002000000000000009, 3001000000000000011, 1, '年度投标经验复盘', 'seed', 'seed'),
    (3004000000000000010, 3003000000000000122, '星河数据中心_机房扩容方案.docx', 1, 4096, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 3002000000000000004, 3001000000000000031, 1, '数据中心机房扩容技术方案', 'seed', 'seed');

INSERT INTO doc_document_version (id, document_id, version_no, storage_key, size, mime_type, original_filename, content_hash, uploaded_by_user_id, change_log, created_by, updated_by)
VALUES
    (3004100000000000001, 3004000000000000001, 1, '2026/06/11/dev-demo-3004000000000000001-v1.pdf', 2048, 'application/pdf', '东海医院_商务响应文件_初稿.pdf', 'demo-hash-0001-v1', 3002000000000000003, '初稿上传', 'seed', 'seed'),
    (3004100000000000002, 3004000000000000001, 2, '2026/06/11/dev-demo-3004000000000000001-v2.pdf', 3584, 'application/pdf', '东海医院_商务响应文件.pdf', 'demo-hash-0001-v2', 3002000000000000003, '补充报价和资质附件', 'seed', 'seed'),
    (3004100000000000003, 3004000000000000002, 1, '2026/06/11/dev-demo-3004000000000000002-v1.docx', 2048, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '东海医院_技术方案_v1.docx', 'demo-hash-0002-v1', 3002000000000000004, '技术方案初稿', 'seed', 'seed'),
    (3004100000000000004, 3004000000000000002, 2, '2026/06/11/dev-demo-3004000000000000002-v2.docx', 3072, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '东海医院_技术方案_v2.docx', 'demo-hash-0002-v2', 3002000000000000004, '补充实施计划', 'seed', 'seed'),
    (3004100000000000005, 3004000000000000002, 3, '2026/06/11/dev-demo-3004000000000000002-v3.docx', 4096, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '东海医院_技术方案_v3.docx', 'demo-hash-0002-v3', 3002000000000000004, '根据澄清意见修订', 'seed', 'seed'),
    (3004100000000000006, 3004000000000000003, 1, '2026/06/11/dev-demo-3004000000000000003-v1.zip', 2048, 'application/zip', '东海医院_招标文件.zip', 'demo-hash-0003-v1', 3002000000000000002, '招标文件原件归档', 'seed', 'seed'),
    (3004100000000000007, 3004000000000000004, 1, '2026/06/11/dev-demo-3004000000000000004-v1.pdf', 1536, 'application/pdf', '营业执照_2026.pdf', 'demo-hash-0004-v1', 3002000000000000005, '年度证照更新', 'seed', 'seed'),
    (3004100000000000008, 3004000000000000005, 1, '2026/06/11/dev-demo-3004000000000000005-v1.pdf', 1536, 'application/pdf', 'ISO9001质量管理体系证书.pdf', 'demo-hash-0005-v1', 3002000000000000005, '证书扫描件上传', 'seed', 'seed'),
    (3004100000000000009, 3004000000000000006, 1, '2026/06/11/dev-demo-3004000000000000006-v1.docx', 2048, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '采购合同模板_草稿.docx', 'demo-hash-0006-v1', 3002000000000000007, '合同模板草稿', 'seed', 'seed'),
    (3004100000000000010, 3004000000000000006, 2, '2026/06/11/dev-demo-3004000000000000006-v2.docx', 3072, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '采购合同模板_标准版.docx', 'demo-hash-0006-v2', 3002000000000000007, '法务审查后定稿', 'seed', 'seed'),
    (3004100000000000011, 3004000000000000007, 1, '2026/06/11/dev-demo-3004000000000000007-v1.pptx', 5120, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', '智慧园区标准技术方案.pptx', 'demo-hash-0007-v1', 3002000000000000004, '标准方案上传', 'seed', 'seed'),
    (3004100000000000012, 3004000000000000008, 1, '2026/06/11/dev-demo-3004000000000000008-v1.xlsx', 2048, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '华东重点客户跟进清单.xlsx', 'demo-hash-0008-v1', 3002000000000000008, '客户清单初始化', 'seed', 'seed'),
    (3004100000000000013, 3004000000000000009, 1, '2026/06/11/dev-demo-3004000000000000009-v1.pdf', 2560, 'application/pdf', '2025年度投标复盘报告.pdf', 'demo-hash-0009-v1', 3002000000000000009, '归档报告上传', 'seed', 'seed'),
    (3004100000000000014, 3004000000000000010, 1, '2026/06/11/dev-demo-3004000000000000010-v1.docx', 4096, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '星河数据中心_机房扩容方案.docx', 'demo-hash-0010-v1', 3002000000000000004, '数据中心方案上传', 'seed', 'seed');

INSERT INTO doc_search_history (id, user_id, keyword, folder_id, result_count, search_time)
VALUES
    (3004200000000000001, 3002000000000000002, '东海医院', 3003000000000000101, 3, now() - interval '2 hours'),
    (3004200000000000002, 3002000000000000003, '商务响应', 3003000000000000112, 1, now() - interval '3 hours'),
    (3004200000000000003, 3002000000000000004, '技术方案', null, 4, now() - interval '4 hours'),
    (3004200000000000004, 3002000000000000005, 'ISO9001', 3003000000000000203, 1, now() - interval '5 hours'),
    (3004200000000000005, 3002000000000000007, '合同模板', 3003000000000000301, 1, now() - interval '6 hours'),
    (3004200000000000006, 3002000000000000008, '客户跟进', 3003000000000000501, 1, now() - interval '8 hours'),
    (3004200000000000007, 3002000000000000009, '归档', 3003000000000000602, 1, now() - interval '1 day'),
    (3004200000000000008, 3002000000000000001, '营业执照', 3003000000000000201, 1, now() - interval '1 day 2 hours');

INSERT INTO doc_download_log (id, document_id, version_no, user_id, download_time, ip_address, user_agent)
VALUES
    (3004300000000000001, 3004000000000000001, 2, 3002000000000000002, now() - interval '1 hour', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000002, 3004000000000000002, 3, 3002000000000000004, now() - interval '2 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000003, 3004000000000000002, 3, 3002000000000000003, now() - interval '3 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000004, 3004000000000000004, 1, 3002000000000000008, now() - interval '4 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000005, 3004000000000000006, 2, 3002000000000000006, now() - interval '5 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000006, 3004000000000000007, 1, 3002000000000000002, now() - interval '7 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000007, 3004000000000000007, 1, 3002000000000000008, now() - interval '9 hours', '127.0.0.1', 'Chrome Dev Demo'),
    (3004300000000000008, 3004000000000000009, 1, 3002000000000000009, now() - interval '1 day', '127.0.0.1', 'Chrome Dev Demo');

INSERT INTO audit_operation_log (id, module_code, biz_type, biz_id, operation_type, operator_user_id, operator_dept_id, request_id, operation_time, before_data, after_data, extra_data, created_by)
VALUES
    (3004400000000000001, 'FOLDER', 'FOLDER', 3003000000000000001, 'CREATE', 3002000000000000001, 3001000000000000001, 'seed-folder-001', now() - interval '3 days', null, '{"name":"投标项目"}', '{"source":"seed"}', 'seed'),
    (3004400000000000002, 'FOLDER', 'FOLDER', 3003000000000000113, 'MANAGER_ADD', 3002000000000000001, 3001000000000000001, 'seed-folder-002', now() - interval '2 days', null, '{"userId":"3002000000000000004"}', '{"manageScope":"SELF_AND_DESCENDANTS"}', 'seed'),
    (3004400000000000003, 'FOLDER', 'FOLDER', 3003000000000000112, 'GRANT_ADD', 3002000000000000003, 3001000000000000021, 'seed-folder-003', now() - interval '1 day 20 hours', null, '{"subjectType":"USER","subjectId":"3002000000000000006"}', '{"permissionCode":"FOLDER_VIEW"}', 'seed'),
    (3004400000000000004, 'DOCUMENT', 'DOCUMENT', 3004000000000000001, 'UPLOAD', 3002000000000000003, 3001000000000000021, 'seed-doc-001', now() - interval '1 day 10 hours', null, '{"name":"东海医院_商务响应文件.pdf"}', '{"versionNo":2}', 'seed'),
    (3004400000000000005, 'DOCUMENT', 'DOCUMENT', 3004000000000000002, 'NEW_VERSION', 3002000000000000004, 3001000000000000031, 'seed-doc-002', now() - interval '20 hours', '{"versionNo":2}', '{"versionNo":3}', '{"changeLog":"根据澄清意见修订"}', 'seed'),
    (3004400000000000006, 'DOCUMENT', 'DOCUMENT', 3004000000000000007, 'DOWNLOAD', 3002000000000000008, 3001000000000000022, 'seed-doc-003', now() - interval '9 hours', null, '{"name":"智慧园区标准技术方案.pptx"}', '{"ip":"127.0.0.1"}', 'seed'),
    (3004400000000000007, 'FOLDER', 'FOLDER', 3003000000000000602, 'FAVORITE', 3002000000000000009, 3001000000000000011, 'seed-folder-004', now() - interval '8 hours', null, '{"folderId":"3003000000000000602"}', '{"source":"front-demo"}', 'seed'),
    (3004400000000000008, 'DOCUMENT', 'DOCUMENT', 3004000000000000006, 'DOWNLOAD', 3002000000000000006, 3001000000000000051, 'seed-doc-004', now() - interval '5 hours', null, '{"name":"采购合同模板_标准版.docx"}', '{"ip":"127.0.0.1"}', 'seed');

-- 调整序列，避免后续接口插入时复用旧 ID。
SELECT setval('sys_department_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_department), 1), true);
SELECT setval('sys_user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_user), 1), true);
SELECT setval('sys_user_role_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_user_role), 1), true);

COMMIT;
