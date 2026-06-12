package com.example.biddoc.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    PARAM_INVALID(4001001, "参数校验失败"),
    RESOURCE_CONFLICT(4001002, "资源冲突"),
    BUSINESS_ILLEGAL(4001003, "业务规则非法"),

    // 认证
    AUTH_FAILED(4011001, "认证失败"),
    ACCOUNT_DISABLED(4011002, "账号不可用"),
    LOGIN_LIMITED(4011003, "登录受限"),

    // 权限
    PERMISSION_DENIED(4031001, "权限不足"),
    ROLE_NOT_MATCH(4031002, "角色权限不匹配"),
    FOLDER_PERMISSION_DENIED(4032001, "无文件夹操作权限"),

    // 资源
    RESOURCE_NOT_FOUND(4041001, "资源不存在"),
    FOLDER_NOT_FOUND(4042001, "文件夹不存在"),
    FOLDER_PARENT_NOT_FOUND(4042002, "父文件夹不存在"),
    // 4042003/4042004 用于 folder 模块二级资源（授权记录、管理员记录）的"不存在"语义
    FOLDER_GRANT_NOT_FOUND(4042003, "授权记录不存在"),
    FOLDER_MANAGER_NOT_FOUND(4042004, "管理员记录不存在"),

    // Folder
    FOLDER_NAME_DUPLICATED(4002001, "同级文件夹名称已存在"),
    FOLDER_LEVEL_EXCEEDED(4002002, "文件夹层级超出限制"),
    FOLDER_HAS_CHILDREN(4002003, "当前文件夹存在子节点"),
    // 4002004 起为 folder Phase 3（授权/管理员）业务校验错误码
    FOLDER_GRANT_DUPLICATED(4002004, "授权已存在"),
    FOLDER_GRANT_ON_ROOT_FORBIDDEN(4002005, "根级文件夹不允许授权"),
    FOLDER_GRANT_SUBJECT_INVALID(4002006, "授权主体非法"),
    FOLDER_MANAGER_DUPLICATED(4002007, "该用户已是管理员"),
    // 4002008 起为 folder Phase 4（move/copy/delete）业务校验错误码
    FOLDER_CYCLE_NOT_ALLOWED(4002008, "不能移动到自身或子目录下"),
    FOLDER_MOVE_TARGET_INVALID(4002009, "目标父目录无效"),
    // 4222001：业务前置不满足（语义贴近 HTTP 422），区别于 4032001 的"权限不足"
    FOLDER_MANAGER_REQUIRES_ROLE(4222001, "用户未具备 FOLDER_ADMIN 角色"),

    // Document (4002xxx 业务校验 / 4042xxx 资源 / 4032xxx 权限 / 5005xxx 存储)
    DOCUMENT_FILE_REQUIRED(4002101, "上传文件不能为空"),
    DOCUMENT_SIZE_EXCEEDED(4002102, "文件大小超出限制"),
    DOCUMENT_NAME_INVALID(4002103, "文档名称非法"),
    DOCUMENT_NAME_DUPLICATED(4002104, "同文件夹下文档名称已存在"),
    DOCUMENT_ROOT_FOLDER_FORBIDDEN(4002105, "根级文件夹不允许直接上传文档"),
    DOCUMENT_PREVIEW_UNSUPPORTED(4002106, "当前文件类型不支持预览"),

    DOCUMENT_NOT_FOUND(4042101, "文档不存在"),
    DOCUMENT_VERSION_NOT_FOUND(4042102, "文档版本不存在"),

    DOCUMENT_OWNERSHIP_REQUIRED(4032101, "仅文档所有者、文件夹管理员或超级管理员可执行此操作"),

    STORAGE_WRITE_FAILED(5005001, "文件存储写入失败"),
    STORAGE_OBJECT_NOT_FOUND(5005002, "存储对象不存在"),
    STORAGE_READ_FAILED(5005003, "文件存储读取失败"),

    // Tag / Notify / Workflow
    TAG_NAME_DUPLICATED(4002201, "标签名称已存在"),
    TAG_NOT_FOUND(4042201, "标签不存在"),
    NOTIFICATION_NOT_FOUND(4043001, "通知不存在"),
    APPROVAL_TASK_NOT_FOUND(4044001, "审批任务不存在"),
    APPROVAL_INSTANCE_NOT_FOUND(4044002, "审批实例不存在"),
    APPROVAL_TASK_NOT_PENDING(4004001, "审批任务不是待处理状态"),
    APPROVAL_APPROVER_INVALID(4034001, "无权处理该审批任务"),

    // Project
    PROJECT_OWNER_REQUIRED(4005001, "项目至少需要一个负责人"),
    PROJECT_DEPT_ABBR_REQUIRED(4005002, "项目所属部门未配置事业部缩写"),
    PROJECT_ARCHIVED_READONLY(4005003, "已归档项目不允许修改"),
    PROJECT_ARCHIVE_CHECKLIST_INCOMPLETE(4005004, "必需清单项未完成，不能归档"),
    PROJECT_NOT_FOUND(4045001, "项目不存在"),
    PROJECT_PERMISSION_DENIED(4035001, "无项目操作权限"),

    // Audit
    AUDIT_RECORD_FAILED(5003001, "审计记录失败"),
    AUDIT_QUERY_FAILED(5003002, "审计查询失败"),

    // 系统
    SYSTEM_ERROR(5001001, "系统异常");

    private final int code;
    private final String message;
}
