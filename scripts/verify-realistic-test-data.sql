-- 本地真实业务种子数据验证脚本。
\set ON_ERROR_STOP on

DO $$
DECLARE
    v_count bigint;
BEGIN
    -- 核心数据量校验。
    SELECT count(*) INTO v_count FROM doc_folder WHERE parent_id = 0 AND deleted = false;
    IF v_count < 6 THEN
        RAISE EXCEPTION 'Expected at least 6 root folders, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM bid_project WHERE deleted = false;
    IF v_count < 4 THEN
        RAISE EXCEPTION 'Expected at least 4 projects, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM doc_document WHERE deleted = false;
    IF v_count < 20 THEN
        RAISE EXCEPTION 'Expected at least 20 documents, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM wf_approval_definition WHERE deleted = false;
    IF v_count < 3 THEN
        RAISE EXCEPTION 'Expected at least 3 workflow definitions, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM wf_approval_instance WHERE deleted = false;
    IF v_count < 10 THEN
        RAISE EXCEPTION 'Expected at least 10 workflow instances, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM wf_approval_task WHERE deleted = false;
    IF v_count < 13 THEN
        RAISE EXCEPTION 'Expected at least 13 workflow tasks, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM sys_notification WHERE deleted = false;
    IF v_count < 14 THEN
        RAISE EXCEPTION 'Expected at least 14 notifications, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM sys_notification WHERE deleted = false AND read_flag = false;
    IF v_count < 8 THEN
        RAISE EXCEPTION 'Expected at least 8 unread notifications, got %', v_count;
    END IF;

    -- 脏数据关键词校验。
    SELECT count(*) INTO v_count
    FROM (
        SELECT username AS value FROM sys_user
        UNION ALL SELECT real_name FROM sys_user
        UNION ALL SELECT name FROM doc_folder
        UNION ALL SELECT name FROM doc_document
        UNION ALL SELECT coalesce(document_no, '') FROM doc_document
        UNION ALL SELECT project_name FROM bid_project
        UNION ALL SELECT project_no FROM bid_project
        UNION ALL SELECT template_name FROM bid_checklist_template
        UNION ALL SELECT item_name FROM bid_checklist_template_item
        UNION ALL SELECT item_name FROM bid_project_checklist_item
        UNION ALL SELECT name FROM wf_approval_definition
        UNION ALL SELECT title FROM sys_notification
        UNION ALL SELECT coalesce(content, '') FROM sys_notification
    ) s
    WHERE value ILIKE '%codex-smoke%'
       OR value ILIKE '%smoke%'
       OR value ILIKE '%e2e%'
       OR value ILIKE '%demo%';
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'Found % dirty keyword values', v_count;
    END IF;

    SELECT count(*) INTO v_count
    FROM doc_folder
    WHERE name ~ '[0-9]{8,}.*folder';
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'Found % timestamp-style folder names', v_count;
    END IF;

    -- 用户清洗和密码格式校验。
    SELECT count(*) INTO v_count
    FROM sys_user
    WHERE username ILIKE '%codex-smoke%'
       OR created_by ILIKE '%codex-smoke%';
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'Found % codex-smoke users', v_count;
    END IF;

    SELECT count(*) INTO v_count
    FROM sys_user
    WHERE deleted = false
      AND (password IS NULL OR password !~ '^\$2[aby]\$10\$');
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'Found % active users without BCrypt password', v_count;
    END IF;

    -- 常见逻辑外键孤儿引用校验。
    SELECT count(*) INTO v_count FROM doc_document d LEFT JOIN doc_folder f ON f.id = d.folder_id WHERE f.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_document.folder_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM doc_document d LEFT JOIN sys_user u ON u.id = d.owner_user_id WHERE u.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_document.owner_user_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM doc_document d LEFT JOIN sys_department dep ON dep.id = d.owner_dept_id WHERE d.owner_dept_id IS NOT NULL AND dep.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_document.owner_dept_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM doc_document_version v LEFT JOIN doc_document d ON d.id = v.document_id WHERE d.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_document_version.document_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM bid_project p LEFT JOIN doc_folder f ON f.id = p.folder_id WHERE p.folder_id IS NOT NULL AND f.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'bid_project.folder_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM bid_project p LEFT JOIN sys_department dep ON dep.id = p.owner_dept_id WHERE dep.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'bid_project.owner_dept_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM wf_approval_task t LEFT JOIN wf_approval_instance i ON i.id = t.instance_id WHERE i.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'wf_approval_task.instance_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM wf_approval_instance i LEFT JOIN doc_document d ON d.id = i.document_id WHERE i.document_id IS NOT NULL AND d.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'wf_approval_instance.document_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM doc_download_log l LEFT JOIN doc_document d ON d.id = l.document_id WHERE d.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_download_log.document_id orphan count %', v_count; END IF;

    SELECT count(*) INTO v_count FROM doc_search_history h LEFT JOIN sys_user u ON u.id = h.user_id WHERE u.id IS NULL;
    IF v_count <> 0 THEN RAISE EXCEPTION 'doc_search_history.user_id orphan count %', v_count; END IF;
END $$;

SELECT 'verification passed' AS result;

SELECT table_name,
       (xpath('/row/c/text()', query_to_xml(format('select count(*) as c from %I.%I', table_schema, table_name), false, true, '')))[1]::text::bigint AS row_count
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
