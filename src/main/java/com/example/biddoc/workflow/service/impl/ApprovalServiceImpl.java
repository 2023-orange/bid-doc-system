package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.constant.ChecklistItemStatusEnum;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.service.ProjectChecklistService;
import com.example.biddoc.project.service.ProjectPermissionService;
import com.example.biddoc.workflow.dto.resp.ApprovalHistoryRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import com.example.biddoc.workflow.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ApprovalInstanceMapper approvalInstanceMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final FolderMapper folderMapper;
    private final FolderPermissionService folderPermissionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final ProjectChecklistService projectChecklistService;
    private final ProjectChecklistItemMapper projectChecklistItemMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectPermissionService projectPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long documentId, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        folderPermissionService.checkView(folder);

        Long approverUserId = resolveApprover(folder, user.getUserId());
        Long instanceId = IdWorker.getId();
        Long taskId = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(instanceId);
        instance.setDocumentId(documentId);
        instance.setBizModule("DOCUMENT");
        instance.setBizType("DOCUMENT");
        instance.setBizId(documentId);
        instance.setScenario("DOCUMENT_APPROVAL");
        instance.setSubmitterUserId(user.getUserId());
        instance.setStatus(STATUS_PENDING);
        instance.setSubmitComment(comment);
        instance.setSubmittedAt(now);
        instance.setDeleted(Boolean.FALSE);
        approvalInstanceMapper.insert(instance);

        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(taskId);
        task.setInstanceId(instanceId);
        task.setDocumentId(documentId);
        task.setApproverUserId(approverUserId);
        task.setStatus(STATUS_PENDING);
        task.setDeleted(Boolean.FALSE);
        approvalTaskMapper.insert(task);

        // 审批实例创建后立即回写资料状态，保证资料列表与审批待办口径一致。
        documentService.markApproving(documentId);

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("instanceId", String.valueOf(instanceId));
        afterData.put("taskId", String.valueOf(taskId));
        afterData.put("approverUserId", String.valueOf(approverUserId));
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode())
                .afterData(afterData)
                .build());

        notificationService.send(
                approverUserId,
                "WORKFLOW_TASK",
                "新的文档审批任务",
                "请审批文档：" + document.getName(),
                "DOCUMENT",
                documentId
        );
        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitVersion(Long documentId, Integer versionNo, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        DocumentVersionEntity version = documentVersionMapper.selectOne(Wrappers.<DocumentVersionEntity>lambdaQuery()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, versionNo)
                .eq(DocumentVersionEntity::getDeleted, false));
        if (version == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        folderPermissionService.checkView(folder);

        Long approverUserId = resolveApprover(folder, user.getUserId());
        Long instanceId = IdWorker.getId();
        Long taskId = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(instanceId);
        instance.setDocumentId(documentId);
        instance.setBizModule("DOCUMENT");
        instance.setBizType("DOCUMENT_VERSION");
        instance.setBizId(documentId);
        instance.setVersionNo(versionNo);
        instance.setScenario("DOCUMENT_VERSION_APPROVAL");
        instance.setSubmitterUserId(user.getUserId());
        instance.setStatus(STATUS_PENDING);
        instance.setSubmitComment(comment);
        instance.setSubmittedAt(now);
        instance.setDeleted(Boolean.FALSE);
        approvalInstanceMapper.insert(instance);

        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(taskId);
        task.setInstanceId(instanceId);
        task.setDocumentId(documentId);
        task.setApproverUserId(approverUserId);
        task.setStatus(STATUS_PENDING);
        task.setDeleted(Boolean.FALSE);
        approvalTaskMapper.insert(task);

        documentService.markVersionApproving(documentId, versionNo);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT_VERSION")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode())
                .afterData(Map.of("instanceId", String.valueOf(instanceId), "versionNo", versionNo))
                .build());
        notificationService.send(approverUserId, "WORKFLOW_TASK", "新的文档版本审批任务",
                "请审批文档版本：" + document.getName() + " v" + versionNo, "DOCUMENT", documentId);
        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitChecklistItem(Long projectId, Long itemId, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        ProjectChecklistItemEntity item = projectChecklistItemMapper.selectById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted()) || !projectId.equals(item.getProjectId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不存在");
        }
        if (ChecklistItemStatusEnum.ARCHIVED.getCode().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED_READONLY);
        }
        projectPermissionService.checkManageOrOwner(projectId, item.getOwnerUserId());

        Long approverUserId = resolveProjectOwner(projectId);
        Long instanceId = IdWorker.getId();
        Long taskId = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(instanceId);
        instance.setDocumentId(null);
        instance.setBizModule("PROJECT");
        instance.setBizType("CHECKLIST_ITEM");
        instance.setBizId(itemId);
        instance.setScenario("CHECKLIST_ITEM_APPROVAL");
        instance.setSubmitterUserId(user.getUserId());
        instance.setStatus(STATUS_PENDING);
        instance.setSubmitComment(comment);
        instance.setSubmittedAt(now);
        instance.setDeleted(Boolean.FALSE);
        approvalInstanceMapper.insert(instance);

        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(taskId);
        task.setInstanceId(instanceId);
        task.setDocumentId(null);
        task.setApproverUserId(approverUserId);
        task.setStatus(STATUS_PENDING);
        task.setDeleted(Boolean.FALSE);
        approvalTaskMapper.insert(task);

        notificationService.send(approverUserId, "WORKFLOW_TASK", "新的清单项审批任务",
                "请审批清单项：" + item.getItemName(), "CHECKLIST_ITEM", itemId);
        return instanceId;
    }

    @Override
    public List<ApprovalTaskRespDTO> listMyTasks(String status) {
        UserContext.UserInfo user = requireCurrentUser();
        return approvalTaskMapper.selectList(
                Wrappers.<ApprovalTaskEntity>lambdaQuery()
                        .eq(ApprovalTaskEntity::getApproverUserId, user.getUserId())
                        .eq(ApprovalTaskEntity::getDeleted, false)
                        .eq(StringUtils.hasText(status), ApprovalTaskEntity::getStatus, status)
                        .orderByDesc(ApprovalTaskEntity::getCreatedAt)
        ).stream().map(this::toTaskRespDTO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, String comment) {
        handle(taskId, comment, STATUS_APPROVED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String comment) {
        handle(taskId, comment, STATUS_REJECTED);
    }

    @Override
    public List<ApprovalHistoryRespDTO> history(Long documentId) {
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        folderPermissionService.checkView(folder);

        List<ApprovalInstanceEntity> instances = approvalInstanceMapper.selectList(
                Wrappers.<ApprovalInstanceEntity>lambdaQuery()
                        .eq(ApprovalInstanceEntity::getDocumentId, documentId)
                        .eq(ApprovalInstanceEntity::getDeleted, false)
                        .orderByDesc(ApprovalInstanceEntity::getSubmittedAt)
        );
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .afterData(Map.of("queryApprovalHistory", true))
                .build());
        return instances.stream().flatMap(instance -> approvalTaskMapper.selectList(
                Wrappers.<ApprovalTaskEntity>lambdaQuery()
                        .eq(ApprovalTaskEntity::getInstanceId, instance.getId())
                        .eq(ApprovalTaskEntity::getDeleted, false)
        ).stream().map(task -> toHistoryRespDTO(instance, task))).toList();
    }

    @Override
    public List<ApprovalHistoryRespDTO> projectHistory(Long projectId) {
        projectPermissionService.checkView(projectId);
        List<Long> itemIds = projectChecklistItemMapper.selectList(Wrappers.<ProjectChecklistItemEntity>lambdaQuery()
                        .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                        .eq(ProjectChecklistItemEntity::getDeleted, false))
                .stream()
                .map(ProjectChecklistItemEntity::getId)
                .toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }
        List<ApprovalInstanceEntity> instances = approvalInstanceMapper.selectList(
                Wrappers.<ApprovalInstanceEntity>lambdaQuery()
                        .eq(ApprovalInstanceEntity::getBizType, "CHECKLIST_ITEM")
                        .in(ApprovalInstanceEntity::getBizId, itemIds)
                        .eq(ApprovalInstanceEntity::getDeleted, false)
                        .orderByDesc(ApprovalInstanceEntity::getSubmittedAt)
        );
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("PROJECT")
                .bizId(projectId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .afterData(Map.of("queryApprovalHistory", true))
                .build());
        return instances.stream().flatMap(instance -> approvalTaskMapper.selectList(
                Wrappers.<ApprovalTaskEntity>lambdaQuery()
                        .eq(ApprovalTaskEntity::getInstanceId, instance.getId())
                        .eq(ApprovalTaskEntity::getDeleted, false)
        ).stream().map(task -> toHistoryRespDTO(instance, task))).toList();
    }

    private void handle(Long taskId, String comment, String finalStatus) {
        UserContext.UserInfo user = requireCurrentUser();
        ApprovalTaskEntity task = approvalTaskMapper.selectById(taskId);
        if (task == null || Boolean.TRUE.equals(task.getDeleted())) {
            throw new BusinessException(ErrorCode.APPROVAL_TASK_NOT_FOUND);
        }
        if (!Objects.equals(task.getApproverUserId(), user.getUserId()) && !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID);
        }
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_TASK_NOT_PENDING);
        }
        ApprovalInstanceEntity instance = approvalInstanceMapper.selectById(task.getInstanceId());
        if (instance == null || Boolean.TRUE.equals(instance.getDeleted())) {
            throw new BusinessException(ErrorCode.APPROVAL_INSTANCE_NOT_FOUND);
        }

        OffsetDateTime now = OffsetDateTime.now();
        task.setStatus(finalStatus);
        task.setComment(comment);
        task.setHandledAt(now);
        approvalTaskMapper.updateById(task);

        instance.setStatus(finalStatus);
        instance.setCompletedAt(now);
        instance.setFinishedAt(now);
        approvalInstanceMapper.updateById(instance);

        // 审批完成必须回写业务对象；按 bizType 分发，避免把流程结果和业务状态割裂。
        if ("CHECKLIST_ITEM".equals(instance.getBizType())) {
            projectChecklistService.recalculateItemStatus(instance.getBizId());
        } else if ("DOCUMENT_VERSION".equals(instance.getBizType())) {
            documentService.markVersionApprovalResult(instance.getDocumentId(), instance.getVersionNo(),
                    STATUS_APPROVED.equals(finalStatus), comment);
        } else {
            documentService.markApprovalResult(instance.getDocumentId(), STATUS_APPROVED.equals(finalStatus), comment);
        }

        boolean checklistApproval = "CHECKLIST_ITEM".equals(instance.getBizType());
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(checklistApproval ? AuditModuleCodeEnum.PROJECT.getCode() : AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType(instance.getBizType() != null ? instance.getBizType() : "DOCUMENT")
                .bizId(instance.getDocumentId() != null ? instance.getDocumentId() : instance.getBizId())
                .operationType(STATUS_APPROVED.equals(finalStatus)
                        ? AuditOperationTypeEnum.APPROVAL_APPROVE.getCode()
                        : AuditOperationTypeEnum.APPROVAL_REJECT.getCode())
                .afterData(Map.of("taskId", String.valueOf(taskId), "status", finalStatus))
                .build());

        notificationService.send(
                instance.getSubmitterUserId(),
                "WORKFLOW_RESULT",
                "审批结果",
                STATUS_APPROVED.equals(finalStatus) ? "你的审批已通过" : "你的审批已驳回",
                instance.getBizType() != null ? instance.getBizType() : "DOCUMENT",
                instance.getBizId() != null ? instance.getBizId() : instance.getDocumentId()
        );
    }

    private Long resolveProjectOwner(Long projectId) {
        ProjectMemberEntity owner = projectMemberMapper.selectOne(Wrappers.<ProjectMemberEntity>lambdaQuery()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getMemberRole, ProjectMemberRoleEnum.OWNER.getCode())
                .eq(ProjectMemberEntity::getDeleted, false)
                .last("limit 1"));
        if (owner == null) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "项目未配置负责人");
        }
        return owner.getUserId();
    }

    private Long resolveApprover(FolderEntity folder, Long submitterUserId) {
        if (folder.getOwnerUserId() != null && !Objects.equals(folder.getOwnerUserId(), submitterUserId)) {
            return folder.getOwnerUserId();
        }
        // owner 自己提交时仍需要一个处理人；MVP 允许 owner 自审，避免流程无法启动。
        if (folder.getOwnerUserId() != null) {
            return folder.getOwnerUserId();
        }
        throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "未找到审批人");
    }

    private UserContext.UserInfo requireCurrentUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "请先登录");
        }
        return user;
    }

    private ApprovalTaskRespDTO toTaskRespDTO(ApprovalTaskEntity entity) {
        ApprovalTaskRespDTO dto = new ApprovalTaskRespDTO();
        dto.setTaskId(entity.getId());
        dto.setInstanceId(entity.getInstanceId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setApproverUserId(entity.getApproverUserId());
        dto.setStatus(entity.getStatus());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setHandledAt(entity.getHandledAt());
        return dto;
    }

    private ApprovalHistoryRespDTO toHistoryRespDTO(ApprovalInstanceEntity instance, ApprovalTaskEntity task) {
        ApprovalHistoryRespDTO dto = new ApprovalHistoryRespDTO();
        dto.setInstanceId(instance.getId());
        dto.setDocumentId(instance.getDocumentId());
        dto.setVersionNo(instance.getVersionNo());
        dto.setBizType(instance.getBizType());
        dto.setSubmitterUserId(instance.getSubmitterUserId());
        dto.setInstanceStatus(instance.getStatus());
        dto.setSubmitComment(instance.getSubmitComment());
        dto.setSubmittedAt(instance.getSubmittedAt());
        dto.setCompletedAt(instance.getCompletedAt());
        dto.setTaskId(task.getId());
        dto.setApproverUserId(task.getApproverUserId());
        dto.setTaskStatus(task.getStatus());
        dto.setTaskComment(task.getComment());
        dto.setHandledAt(task.getHandledAt());
        return dto;
    }
}
