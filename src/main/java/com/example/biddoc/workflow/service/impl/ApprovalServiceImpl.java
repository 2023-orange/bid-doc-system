package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
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
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.service.ProjectChecklistService;
import com.example.biddoc.project.service.ProjectPermissionService;
import com.example.biddoc.workflow.dto.resp.ApprovalHandleResultRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalHistoryRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;
import com.example.biddoc.workflow.entity.ApprovalActionLogEntity;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskCandidateEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalActionLogMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskCandidateMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import com.example.biddoc.workflow.service.ApprovalFlowEngineService;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String STATUS_TERMINATED = "TERMINATED";
    private static final String STATUS_TRANSFERRED = "TRANSFERRED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String SCENARIO_DOCUMENT_APPROVAL = "DOCUMENT_APPROVAL";
    private static final String SCENARIO_DOCUMENT_VERSION_APPROVAL = "DOCUMENT_VERSION_APPROVAL";
    private static final String SCENARIO_CHECKLIST_ITEM_APPROVAL = "CHECKLIST_ITEM_APPROVAL";
    private static final String BIZ_MODULE_DOCUMENT = "DOCUMENT";
    private static final String BIZ_MODULE_PROJECT = "PROJECT";
    private static final String BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final String BIZ_TYPE_DOCUMENT_VERSION = "DOCUMENT_VERSION";
    private static final String BIZ_TYPE_CHECKLIST_ITEM = "CHECKLIST_ITEM";
    private static final int USER_STATUS_ENABLED = 1;

    private final ApprovalInstanceMapper approvalInstanceMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ApprovalActionLogMapper approvalActionLogMapper;
    private final ApprovalDefinitionMapper approvalDefinitionMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalTaskCandidateMapper approvalTaskCandidateMapper;
    private final SysUserMapper sysUserMapper;
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
    private final ProjectMapper projectMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ApprovalFlowEngineService approvalFlowEngineService;

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

        ApprovalDefinitionEntity definition = approvalFlowEngineService.matchDefinition(
                SCENARIO_DOCUMENT_APPROVAL,
                BIZ_MODULE_DOCUMENT,
                BIZ_TYPE_DOCUMENT,
                document.getOwnerDeptId() != null ? document.getOwnerDeptId() : user.getDeptId(),
                document.getBusinessCategory());
        ApprovalInstanceEntity instance = createPendingInstance(definition, documentId, BIZ_MODULE_DOCUMENT,
                BIZ_TYPE_DOCUMENT, documentId, null, SCENARIO_DOCUMENT_APPROVAL, user, comment);
        ApprovalTaskEntity task;
        if (definition != null) {
            task = approvalFlowEngineService.createFirstTask(instance, folder, null, user);
            approvalInstanceMapper.updateById(instance);
        } else {
            task = createPendingTask(instance, resolveApprover(folder, user.getUserId()), null, null,
                    null, Boolean.FALSE);
        }

        // 审批实例创建后立即回写资料状态，保证资料列表与审批待办口径一致。
        documentService.markApproving(documentId);
        recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), user.getUserId(),
                comment, null, STATUS_PENDING);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), Map.of(
                "instanceId", String.valueOf(instance.getId()),
                "taskId", String.valueOf(task.getId()),
                "approverUserId", String.valueOf(task.getApproverUserId())));
        notifyTask(task, instance, "新的文档审批任务", "请审批文档：" + document.getName());
        return instance.getId();
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

        ApprovalDefinitionEntity definition = approvalFlowEngineService.matchDefinition(
                SCENARIO_DOCUMENT_VERSION_APPROVAL,
                BIZ_MODULE_DOCUMENT,
                BIZ_TYPE_DOCUMENT_VERSION,
                document.getOwnerDeptId() != null ? document.getOwnerDeptId() : user.getDeptId(),
                document.getBusinessCategory());
        ApprovalInstanceEntity instance = createPendingInstance(definition, documentId, BIZ_MODULE_DOCUMENT,
                BIZ_TYPE_DOCUMENT_VERSION, documentId, versionNo, SCENARIO_DOCUMENT_VERSION_APPROVAL, user, comment);
        ApprovalTaskEntity task;
        if (definition != null) {
            task = approvalFlowEngineService.createFirstTask(instance, folder, null, user);
            approvalInstanceMapper.updateById(instance);
        } else {
            task = createPendingTask(instance, resolveApprover(folder, user.getUserId()), null, null,
                    null, Boolean.FALSE);
        }

        documentService.markVersionApproving(documentId, versionNo);
        recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), user.getUserId(),
                comment, null, STATUS_PENDING);
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("instanceId", String.valueOf(instance.getId()));
        afterData.put("taskId", String.valueOf(task.getId()));
        afterData.put("approverUserId", String.valueOf(task.getApproverUserId()));
        afterData.put("versionNo", versionNo);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), afterData);
        notifyTask(task, instance, "新的文档版本审批任务", "请审批文档版本：" + document.getName() + " v" + versionNo);
        return instance.getId();
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

        ProjectEntity project = projectMapper.selectById(projectId);
        Long deptId = project != null && project.getOwnerDeptId() != null ? project.getOwnerDeptId() : user.getDeptId();
        ApprovalDefinitionEntity definition = approvalFlowEngineService.matchDefinition(
                SCENARIO_CHECKLIST_ITEM_APPROVAL,
                BIZ_MODULE_PROJECT,
                BIZ_TYPE_CHECKLIST_ITEM,
                deptId,
                item.getBusinessCategory());
        ApprovalInstanceEntity instance = createPendingInstance(definition, null, BIZ_MODULE_PROJECT,
                BIZ_TYPE_CHECKLIST_ITEM, itemId, null, SCENARIO_CHECKLIST_ITEM_APPROVAL, user, comment);
        ApprovalTaskEntity task;
        if (definition != null) {
            task = approvalFlowEngineService.createFirstTask(instance, null, projectId, user);
            approvalInstanceMapper.updateById(instance);
        } else {
            task = createPendingTask(instance, resolveProjectOwner(projectId), null, null,
                    null, Boolean.FALSE);
        }

        markChecklistItemStatus(itemId, ChecklistItemStatusEnum.PENDING_REVIEW.getCode());
        recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), user.getUserId(),
                comment, null, STATUS_PENDING);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), Map.of(
                "instanceId", String.valueOf(instance.getId()),
                "taskId", String.valueOf(task.getId()),
                "approverUserId", String.valueOf(task.getApproverUserId())));
        notifyTask(task, instance, "新的清单项审批任务", "请审批清单项：" + item.getItemName());
        return instance.getId();
    }

    @Override
    public List<ApprovalTaskRespDTO> listMyTasks(String status) {
        UserContext.UserInfo user = requireCurrentUser();
        var query = Wrappers.<ApprovalTaskEntity>lambdaQuery()
                .eq(ApprovalTaskEntity::getDeleted, false)
                .eq(StringUtils.hasText(status), ApprovalTaskEntity::getStatus, status);
        // 超级管理员承担全局审批监管职责，可以查看所有审批任务；普通用户仍只能查看分配给自己的任务。
        if (!user.isSuperAdmin()) {
            query.eq(ApprovalTaskEntity::getApproverUserId, user.getUserId());
        }
        return approvalTaskMapper.selectList(query.orderByDesc(ApprovalTaskEntity::getCreatedAt))
                .stream().map(task -> toTaskRespDTO(task, user)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalHandleResultRespDTO approve(Long taskId, String comment) {
        return handle(taskId, comment, STATUS_APPROVED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalHandleResultRespDTO reject(Long taskId, String comment) {
        return handle(taskId, comment, STATUS_REJECTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long instanceId, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        ApprovalInstanceEntity instance = requirePendingInstance(instanceId);
        if (!Objects.equals(instance.getSubmitterUserId(), user.getUserId()) && !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "仅发起人可撤回审批");
        }
        List<ApprovalTaskEntity> tasks = pendingTasks(instanceId);
        for (ApprovalTaskEntity task : tasks) {
            closeTask(task, STATUS_WITHDRAWN, comment);
        }
        OffsetDateTime now = OffsetDateTime.now();
        instance.setStatus(STATUS_WITHDRAWN);
        instance.setCompletedAt(now);
        instance.setFinishedAt(now);
        approvalInstanceMapper.updateById(instance);
        rollbackBusiness(instance, comment, false);
        recordActionLog(instance, null, null, AuditOperationTypeEnum.APPROVAL_WITHDRAW.getCode(),
                user.getUserId(), comment, STATUS_PENDING, STATUS_WITHDRAWN);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_WITHDRAW.getCode(),
                Map.of("instanceId", String.valueOf(instanceId)));
        notifySubmitter(instance, "审批已撤回", "你的审批已被撤回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, Long targetUserId, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "转交人不能为空");
        }
        ApprovalTaskEntity task = requirePendingTask(taskId);
        validateTargetUser(targetUserId, task.getApproverUserId(), "转交人");
        if (!Objects.equals(task.getApproverUserId(), user.getUserId()) && !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "仅当前处理人可转交");
        }
        ApprovalInstanceEntity instance = requireInstance(task.getInstanceId());
        closeTask(task, STATUS_TRANSFERRED, comment);
        ApprovalTaskEntity newTask = createTransferredTask(instance, task, targetUserId);
        recordAction(instance, newTask, AuditOperationTypeEnum.APPROVAL_TRANSFER.getCode(), user.getUserId(),
                comment, STATUS_PENDING, STATUS_PENDING);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_TRANSFER.getCode(), Map.of(
                "taskId", String.valueOf(taskId),
                "targetUserId", String.valueOf(targetUserId),
                "newTaskId", String.valueOf(newTask.getId())));
        notificationService.send(targetUserId, "WORKFLOW_TASK", "你收到一个转交审批任务",
                StringUtils.hasText(comment) ? comment : "请继续处理审批任务",
                instance.getBizType() != null ? instance.getBizType() : BIZ_TYPE_DOCUMENT,
                instance.getBizId() != null ? instance.getBizId() : instance.getDocumentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(Long taskId, Long assigneeUserId, String comment) {
        UserContext.UserInfo user = requireCurrentUser();
        if (assigneeUserId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "加签人不能为空");
        }
        ApprovalTaskEntity task = requirePendingTask(taskId);
        validateTargetUser(assigneeUserId, task.getApproverUserId(), "加签人");
        if (!Objects.equals(task.getApproverUserId(), user.getUserId()) && !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "仅当前处理人可加签");
        }
        ApprovalInstanceEntity instance = requireInstance(task.getInstanceId());
        ApprovalTaskEntity addSignTask = createAddSignTask(instance, task, assigneeUserId);
        recordAction(instance, addSignTask, AuditOperationTypeEnum.APPROVAL_ADD_SIGN.getCode(), user.getUserId(),
                comment, STATUS_PENDING, STATUS_PENDING);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_ADD_SIGN.getCode(), Map.of(
                "taskId", String.valueOf(taskId),
                "assigneeUserId", String.valueOf(assigneeUserId),
                "addSignTaskId", String.valueOf(addSignTask.getId())));
        notificationService.send(assigneeUserId, "WORKFLOW_TASK", "你收到一个加签审批任务",
                StringUtils.hasText(comment) ? comment : "请协助处理审批任务",
                instance.getBizType() != null ? instance.getBizType() : BIZ_TYPE_DOCUMENT,
                instance.getBizId() != null ? instance.getBizId() : instance.getDocumentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long instanceId, String reason) {
        UserContext.UserInfo user = requireCurrentUser();
        if (!user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_MATCH, "仅超级管理员可终止审批");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "终止原因不能为空");
        }
        ApprovalInstanceEntity instance = requirePendingInstance(instanceId);
        for (ApprovalTaskEntity task : pendingTasks(instanceId)) {
            closeTask(task, STATUS_CANCELLED, reason);
        }
        OffsetDateTime now = OffsetDateTime.now();
        instance.setStatus(STATUS_TERMINATED);
        instance.setCompletedAt(now);
        instance.setFinishedAt(now);
        approvalInstanceMapper.updateById(instance);
        rollbackBusiness(instance, reason, true);
        recordActionLog(instance, null, null, AuditOperationTypeEnum.APPROVAL_TERMINATE.getCode(),
                user.getUserId(), reason, STATUS_PENDING, STATUS_TERMINATED);
        recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_TERMINATE.getCode(),
                Map.of("instanceId", String.valueOf(instanceId), "reason", reason));
        notifySubmitter(instance, "审批已终止", "你的审批被超级管理员终止");
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
        return instances.stream().flatMap(instance -> enrichHistory(instance)).toList();
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
        return instances.stream().flatMap(instance -> enrichHistory(instance)).toList();
    }

    private ApprovalHandleResultRespDTO handle(Long taskId, String comment, String finalStatus) {
        UserContext.UserInfo user = requireCurrentUser();
        ApprovalTaskEntity task = requirePendingTask(taskId);
        if (!Objects.equals(task.getApproverUserId(), user.getUserId()) && !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID);
        }
        ApprovalInstanceEntity instance = requireInstance(task.getInstanceId());

        OffsetDateTime now = OffsetDateTime.now();
        task.setStatus(finalStatus);
        task.setComment(comment);
        task.setHandledAt(now);
        approvalTaskMapper.updateById(task);

        if (STATUS_REJECTED.equals(finalStatus)) {
            closeOtherPendingTasks(instance.getId(), task.getId(), STATUS_CANCELLED, comment);
            finishInstance(instance, finalStatus);
            applyBusinessApprovalResult(instance, comment, false);
            recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_REJECT.getCode(), user.getUserId(),
                    comment, STATUS_PENDING, STATUS_REJECTED);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_REJECT.getCode(), Map.of(
                    "taskId", String.valueOf(taskId),
                    "status", STATUS_REJECTED));
            notifySubmitter(instance, "审批结果", "你的审批已驳回");
            return handleResult(task.getStatus(), instance.getStatus(), true, null);
        }

        if (instance.getDefinitionId() == null || task.getNodeId() == null) {
            closeOtherPendingTasks(instance.getId(), task.getId(), STATUS_CANCELLED, comment);
            finishInstance(instance, STATUS_APPROVED);
            applyBusinessApprovalResult(instance, comment, true);
            recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), user.getUserId(),
                    comment, STATUS_PENDING, STATUS_APPROVED);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), Map.of(
                    "taskId", String.valueOf(taskId),
                    "status", STATUS_APPROVED,
                    "fallback", true));
            notifySubmitter(instance, "审批结果", "你的审批已通过");
            return handleResult(task.getStatus(), instance.getStatus(), true, null);
        }

        if (hasPendingSiblingTask(task)) {
            recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), user.getUserId(),
                    comment, STATUS_PENDING, STATUS_PENDING);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), Map.of(
                    "taskId", String.valueOf(taskId),
                    "status", STATUS_PENDING,
                    "waitingSiblingTask", true));
            return handleResult(task.getStatus(), instance.getStatus(), false, null);
        }

        ApprovalTaskEntity nextTask = approvalFlowEngineService.createNextTask(instance, task,
                resolveFolderForInstance(instance), resolveProjectIdForInstance(instance), user);
        approvalInstanceMapper.updateById(instance);
        if (nextTask == null) {
            closeOtherPendingTasks(instance.getId(), task.getId(), STATUS_CANCELLED, comment);
            finishInstance(instance, STATUS_APPROVED);
            applyBusinessApprovalResult(instance, comment, true);
            recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), user.getUserId(),
                    comment, STATUS_PENDING, STATUS_APPROVED);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), Map.of(
                    "taskId", String.valueOf(taskId),
                    "status", STATUS_APPROVED));
            notifySubmitter(instance, "审批结果", "你的审批已通过");
            return handleResult(task.getStatus(), instance.getStatus(), true, null);
        } else {
            instance.setStatus(STATUS_PENDING);
            approvalInstanceMapper.updateById(instance);
            recordAction(instance, task, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), user.getUserId(),
                    comment, STATUS_PENDING, STATUS_PENDING);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_APPROVE.getCode(), Map.of(
                    "taskId", String.valueOf(taskId),
                    "status", STATUS_PENDING,
                    "nextTaskId", String.valueOf(nextTask.getId())));
            recordAction(instance, nextTask, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), user.getUserId(),
                    null, null, STATUS_PENDING);
            recordApprovalAudit(instance, AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode(), Map.of(
                    "taskId", String.valueOf(nextTask.getId()),
                    "approverUserId", String.valueOf(nextTask.getApproverUserId()),
                    "status", STATUS_PENDING));
            notifyTask(nextTask, instance, "新的审批节点待办", "请继续处理审批任务");
            return handleResult(task.getStatus(), instance.getStatus(), false, nextTask);
        }
    }

    private ApprovalHandleResultRespDTO handleResult(String taskStatus, String instanceStatus,
                                                     boolean completed, ApprovalTaskEntity nextTask) {
        ApprovalHandleResultRespDTO dto = new ApprovalHandleResultRespDTO();
        dto.setTaskStatus(taskStatus);
        dto.setInstanceStatus(instanceStatus);
        dto.setCompleted(completed);
        dto.setNextTaskId(nextTask != null ? nextTask.getId() : null);
        dto.setNextNodeName(nextTask != null ? resolveNodeName(nextTask.getNodeId()) : null);
        return dto;
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

    private ApprovalTaskRespDTO toTaskRespDTO(ApprovalTaskEntity entity, UserContext.UserInfo user) {
        ApprovalTaskRespDTO dto = new ApprovalTaskRespDTO();
        dto.setTaskId(entity.getId());
        dto.setInstanceId(entity.getInstanceId());
        dto.setDocumentId(entity.getDocumentId());
        ApprovalInstanceEntity instance = approvalInstanceMapper.selectById(entity.getInstanceId());
        if (instance != null) {
            dto.setDefinitionName(resolveDefinitionName(instance.getDefinitionId()));
            dto.setBizModule(instance.getBizModule());
            dto.setBizId(instance.getBizId());
            dto.setInstanceStatus(instance.getStatus());
            dto.setInstanceStatusName(statusName(instance.getStatus()));
            dto.setSubmitterName(resolveUserName(instance.getSubmitterUserId()));
            fillBusinessInfo(dto, instance);
        }
        dto.setNodeName(resolveNodeName(entity.getNodeId()));
        dto.setApproverUserId(entity.getApproverUserId());
        dto.setApproverName(resolveUserName(entity.getApproverUserId()));
        dto.setHandlerUserId(resolveHandlerUserId(entity));
        dto.setHandlerName(resolveUserName(dto.getHandlerUserId()));
        dto.setStatus(entity.getStatus());
        dto.setComment(entity.getComment());
        fillActionFlags(dto, entity, instance, user);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setHandledAt(entity.getHandledAt());
        return dto;
    }

    private void fillBusinessInfo(ApprovalTaskRespDTO dto, ApprovalInstanceEntity instance) {
        if (instance.getDocumentId() != null) {
            DocumentEntity document = documentMapper.selectById(instance.getDocumentId());
            if (document != null) {
                dto.setDocumentName(document.getName());
                dto.setDocumentNo(document.getDocumentNo());
            }
        }

        if (BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType()) && instance.getBizId() != null) {
            ProjectChecklistItemEntity item = projectChecklistItemMapper.selectById(instance.getBizId());
            if (item != null) {
                dto.setChecklistItemName(item.getItemName());
                ProjectEntity project = projectMapper.selectById(item.getProjectId());
                if (project != null) {
                    dto.setProjectName(project.getProjectName());
                }
            }
        }
        dto.setBusinessTitle(buildBusinessTitle(dto, instance));
    }

    private String buildBusinessTitle(ApprovalTaskRespDTO dto, ApprovalInstanceEntity instance) {
        if (BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType())) {
            if (StringUtils.hasText(dto.getProjectName()) && StringUtils.hasText(dto.getChecklistItemName())) {
                return dto.getProjectName() + " / " + dto.getChecklistItemName();
            }
            return StringUtils.hasText(dto.getChecklistItemName()) ? dto.getChecklistItemName() : dto.getProjectName();
        }
        if (BIZ_TYPE_DOCUMENT_VERSION.equals(instance.getBizType())) {
            String versionSuffix = instance.getVersionNo() != null ? " v" + instance.getVersionNo() : "";
            return StringUtils.hasText(dto.getDocumentName()) ? dto.getDocumentName() + versionSuffix : null;
        }
        return dto.getDocumentName();
    }

    private Long resolveHandlerUserId(ApprovalTaskEntity task) {
        ApprovalActionLogEntity log = approvalActionLogMapper.selectOne(Wrappers.<ApprovalActionLogEntity>lambdaQuery()
                .eq(ApprovalActionLogEntity::getTaskId, task.getId())
                .eq(ApprovalActionLogEntity::getDeleted, false)
                .orderByDesc(ApprovalActionLogEntity::getActionAt)
                .orderByDesc(ApprovalActionLogEntity::getCreatedAt)
                .last("limit 1"));
        return log != null ? log.getActionUserId() : null;
    }

    private void fillActionFlags(ApprovalTaskRespDTO dto, ApprovalTaskEntity task,
                                 ApprovalInstanceEntity instance, UserContext.UserInfo user) {
        boolean pendingTask = STATUS_PENDING.equals(task.getStatus());
        boolean currentHandler = Objects.equals(task.getApproverUserId(), user.getUserId());
        boolean canHandleTask = pendingTask && (currentHandler || user.isSuperAdmin());
        boolean pendingInstance = instance != null && STATUS_PENDING.equals(instance.getStatus());
        boolean submitter = instance != null && Objects.equals(instance.getSubmitterUserId(), user.getUserId());

        // 这些字段只用于前端按钮展示；真正的权限边界仍由对应动作接口再次校验。
        dto.setCanApprove(canHandleTask);
        dto.setCanReject(canHandleTask);
        dto.setCanTransfer(canHandleTask);
        dto.setCanAddSign(canHandleTask);
        dto.setCanWithdraw(pendingInstance && (submitter || user.isSuperAdmin()));
        dto.setCanTerminate(pendingInstance && user.isSuperAdmin());
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String statusName(String status) {
        if (STATUS_PENDING.equals(status)) {
            return "待审批";
        }
        if (STATUS_APPROVED.equals(status)) {
            return "已通过";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "已驳回";
        }
        if (STATUS_WITHDRAWN.equals(status)) {
            return "已撤回";
        }
        if (STATUS_TERMINATED.equals(status)) {
            return "已终止";
        }
        if (STATUS_TRANSFERRED.equals(status)) {
            return "已转交";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }
        return status;
    }

    private ApprovalHistoryRespDTO toHistoryRespDTO(ApprovalInstanceEntity instance, ApprovalTaskEntity task) {
        ApprovalHistoryRespDTO dto = baseHistoryRespDTO(instance);
        dto.setNodeName(resolveNodeName(task.getNodeId()));
        dto.setTaskId(task.getId());
        dto.setApproverUserId(task.getApproverUserId());
        dto.setTaskStatus(task.getStatus());
        dto.setTaskComment(task.getComment());
        dto.setHandledAt(task.getHandledAt());
        dto.setActionType(task.getStatus());
        dto.setActionComment(task.getComment());
        dto.setActionTime(task.getHandledAt());
        dto.setHandlerUserId(task.getApproverUserId());
        return dto;
    }

    private ApprovalInstanceEntity createPendingInstance(ApprovalDefinitionEntity definition, Long documentId,
                                                         String bizModule, String bizType, Long bizId,
                                                         Integer versionNo, String scenario,
                                                         UserContext.UserInfo user, String comment) {
        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(IdWorker.getId());
        instance.setDocumentId(documentId);
        instance.setBizModule(bizModule);
        instance.setBizType(bizType);
        instance.setBizId(bizId);
        instance.setVersionNo(versionNo);
        instance.setScenario(scenario);
        if (definition != null) {
            instance.setDefinitionId(definition.getId());
            instance.setDefinitionVersion(definition.getVersion());
        }
        instance.setSubmitterUserId(user.getUserId());
        instance.setStatus(STATUS_PENDING);
        instance.setSubmitComment(comment);
        instance.setSubmittedAt(OffsetDateTime.now());
        instance.setDeleted(Boolean.FALSE);
        approvalInstanceMapper.insert(instance);
        return instance;
    }

    private ApprovalTaskEntity createPendingTask(ApprovalInstanceEntity instance, Long approverUserId,
                                                 Long nodeId, String nodeCode, Long transferredFromTaskId,
                                                 Boolean addSign) {
        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(IdWorker.getId());
        task.setInstanceId(instance.getId());
        task.setDocumentId(instance.getDocumentId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setNodeId(nodeId);
        task.setNodeCode(nodeCode);
        task.setApproverUserId(approverUserId);
        task.setStatus(STATUS_PENDING);
        task.setTransferredFromTaskId(transferredFromTaskId);
        task.setAddSign(Boolean.TRUE.equals(addSign));
        task.setDeleted(Boolean.FALSE);
        approvalTaskMapper.insert(task);
        if (nodeId != null || StringUtils.hasText(nodeCode)) {
            instance.setCurrentNodeId(nodeId);
            instance.setCurrentNodeCode(nodeCode);
        }
        return task;
    }

    private ApprovalTaskEntity createTransferredTask(ApprovalInstanceEntity instance, ApprovalTaskEntity source,
                                                    Long targetUserId) {
        ApprovalTaskEntity task = createPendingTask(instance, targetUserId, source.getNodeId(), source.getNodeCode(),
                source.getId(), Boolean.TRUE.equals(source.getAddSign()));
        createCandidateSnapshot(instance, task, "TRANSFER", String.valueOf(source.getApproverUserId()), targetUserId);
        return task;
    }

    private ApprovalTaskEntity createAddSignTask(ApprovalInstanceEntity instance, ApprovalTaskEntity source,
                                                 Long assigneeUserId) {
        ApprovalTaskEntity task = createPendingTask(instance, assigneeUserId, source.getNodeId(), source.getNodeCode(),
                source.getId(), Boolean.TRUE);
        createCandidateSnapshot(instance, task, "ADD_SIGN", String.valueOf(source.getApproverUserId()), assigneeUserId);
        return task;
    }

    private void createCandidateSnapshot(ApprovalInstanceEntity instance, ApprovalTaskEntity task,
                                         String candidateType, String candidateValue, Long candidateUserId) {
        ApprovalTaskCandidateEntity candidate = new ApprovalTaskCandidateEntity();
        candidate.setId(IdWorker.getId());
        candidate.setTaskId(task.getId());
        candidate.setInstanceId(instance.getId());
        candidate.setDefinitionId(instance.getDefinitionId());
        candidate.setNodeId(task.getNodeId());
        candidate.setNodeCode(task.getNodeCode());
        candidate.setCandidateType(candidateType);
        candidate.setCandidateValue(candidateValue);
        candidate.setCandidateUserId(candidateUserId);
        candidate.setResolved(Boolean.TRUE);
        candidate.setDeleted(Boolean.FALSE);
        approvalTaskCandidateMapper.insert(candidate);
    }

    private ApprovalTaskEntity requirePendingTask(Long taskId) {
        ApprovalTaskEntity task = approvalTaskMapper.selectById(taskId);
        if (task == null || Boolean.TRUE.equals(task.getDeleted())) {
            throw new BusinessException(ErrorCode.APPROVAL_TASK_NOT_FOUND);
        }
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_TASK_NOT_PENDING);
        }
        return task;
    }

    private ApprovalInstanceEntity requireInstance(Long instanceId) {
        ApprovalInstanceEntity instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null || Boolean.TRUE.equals(instance.getDeleted())) {
            throw new BusinessException(ErrorCode.APPROVAL_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    private ApprovalInstanceEntity requirePendingInstance(Long instanceId) {
        ApprovalInstanceEntity instance = requireInstance(instanceId);
        if (!STATUS_PENDING.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_TASK_NOT_PENDING, "审批实例不是待处理状态");
        }
        return instance;
    }

    private void validateTargetUser(Long targetUserId, Long currentApproverUserId, String fieldName) {
        if (Objects.equals(targetUserId, currentApproverUserId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "不能是当前处理人");
        }
        SysUser targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null || Boolean.TRUE.equals(targetUser.getDeleted())
                || targetUser.getStatus() == null || targetUser.getStatus() != USER_STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, fieldName + "不存在或不可用");
        }
    }

    private List<ApprovalTaskEntity> pendingTasks(Long instanceId) {
        return approvalTaskMapper.selectList(Wrappers.<ApprovalTaskEntity>lambdaQuery()
                .eq(ApprovalTaskEntity::getInstanceId, instanceId)
                .eq(ApprovalTaskEntity::getStatus, STATUS_PENDING)
                .eq(ApprovalTaskEntity::getDeleted, false));
    }

    private boolean hasPendingSiblingTask(ApprovalTaskEntity task) {
        // 加签会产生同实例、同节点的额外待办；全部处理完之前不能推进到下一节点。
        return approvalTaskMapper.selectCount(Wrappers.<ApprovalTaskEntity>lambdaQuery()
                .eq(ApprovalTaskEntity::getInstanceId, task.getInstanceId())
                .eq(task.getNodeId() != null, ApprovalTaskEntity::getNodeId, task.getNodeId())
                .eq(task.getNodeId() == null, ApprovalTaskEntity::getNodeCode, task.getNodeCode())
                .eq(ApprovalTaskEntity::getStatus, STATUS_PENDING)
                .eq(ApprovalTaskEntity::getDeleted, false)) > 0;
    }

    private void closeOtherPendingTasks(Long instanceId, Long handledTaskId, String status, String comment) {
        for (ApprovalTaskEntity pendingTask : pendingTasks(instanceId)) {
            if (!Objects.equals(pendingTask.getId(), handledTaskId)) {
                closeTask(pendingTask, status, comment);
            }
        }
    }

    private void closeTask(ApprovalTaskEntity task, String status, String comment) {
        task.setStatus(status);
        task.setComment(comment);
        task.setHandledAt(OffsetDateTime.now());
        approvalTaskMapper.updateById(task);
    }

    private void finishInstance(ApprovalInstanceEntity instance, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        instance.setStatus(status);
        instance.setCompletedAt(now);
        instance.setFinishedAt(now);
        approvalInstanceMapper.updateById(instance);
    }

    private void applyBusinessApprovalResult(ApprovalInstanceEntity instance, String comment, boolean approved) {
        // 审批最终结果只在实例终态时回写业务对象，避免多节点审批中间态污染资料或清单状态。
        if (BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType())) {
            projectChecklistService.recalculateItemStatus(instance.getBizId());
        } else if (BIZ_TYPE_DOCUMENT_VERSION.equals(instance.getBizType())) {
            documentService.markVersionApprovalResult(instance.getDocumentId(), instance.getVersionNo(), approved, comment);
        } else {
            documentService.markApprovalResult(instance.getDocumentId(), approved, comment);
        }
    }

    private void markChecklistItemStatus(Long itemId, String status) {
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setId(itemId);
        update.setStatus(status);
        update.setUpdatedAt(OffsetDateTime.now());
        projectChecklistItemMapper.updateById(update);
    }

    private void rollbackBusiness(ApprovalInstanceEntity instance, String reason, boolean terminate) {
        // 撤回/终止不代表业务被驳回；资料回到可重新提交状态，清单重新计算当前资料绑定口径。
        if (BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType())) {
            projectChecklistService.recalculateItemStatus(instance.getBizId());
        } else if (BIZ_TYPE_DOCUMENT_VERSION.equals(instance.getBizType())) {
            documentService.markVersionApprovalWithdrawn(instance.getDocumentId(), instance.getVersionNo(), reason);
        } else {
            documentService.markApprovalWithdrawn(instance.getDocumentId(), reason);
        }
    }

    private FolderEntity resolveFolderForInstance(ApprovalInstanceEntity instance) {
        if (instance.getDocumentId() == null) {
            return null;
        }
        DocumentEntity document = documentMapper.selectById(instance.getDocumentId());
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private Long resolveProjectIdForInstance(ApprovalInstanceEntity instance) {
        if (!BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType()) || instance.getBizId() == null) {
            return null;
        }
        ProjectChecklistItemEntity item = projectChecklistItemMapper.selectById(instance.getBizId());
        return item != null ? item.getProjectId() : null;
    }

    private void notifyTask(ApprovalTaskEntity task, ApprovalInstanceEntity instance, String title, String content) {
        notificationService.send(task.getApproverUserId(), "WORKFLOW_TASK", title, content,
                instance.getBizType() != null ? instance.getBizType() : BIZ_TYPE_DOCUMENT,
                instance.getBizId() != null ? instance.getBizId() : instance.getDocumentId());
    }

    private void notifySubmitter(ApprovalInstanceEntity instance, String title, String content) {
        notificationService.send(instance.getSubmitterUserId(), "WORKFLOW_RESULT", title, content,
                instance.getBizType() != null ? instance.getBizType() : BIZ_TYPE_DOCUMENT,
                instance.getBizId() != null ? instance.getBizId() : instance.getDocumentId());
    }

    private void recordAction(ApprovalInstanceEntity instance, ApprovalTaskEntity task, String actionType,
                              Long actionUserId, String comment, String beforeStatus, String afterStatus) {
        recordActionLog(instance, task != null ? task.getId() : null, task, actionType,
                actionUserId, comment, beforeStatus, afterStatus);
    }

    private void recordActionLog(ApprovalInstanceEntity instance, Long taskId, ApprovalTaskEntity task,
                                 String actionType, Long actionUserId, String comment,
                                 String beforeStatus, String afterStatus) {
        ApprovalActionLogEntity log = new ApprovalActionLogEntity();
        log.setId(IdWorker.getId());
        log.setInstanceId(instance.getId());
        log.setTaskId(taskId);
        log.setDefinitionId(instance.getDefinitionId());
        log.setNodeId(task != null ? task.getNodeId() : instance.getCurrentNodeId());
        log.setNodeCode(task != null ? task.getNodeCode() : instance.getCurrentNodeCode());
        log.setActionType(actionType);
        log.setActionUserId(actionUserId);
        log.setActionComment(comment);
        log.setActionAt(OffsetDateTime.now());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setDeleted(Boolean.FALSE);
        approvalActionLogMapper.insert(log);
    }

    private void recordApprovalAudit(ApprovalInstanceEntity instance, String operationType,
                                     Map<String, Object> afterData) {
        boolean checklistApproval = BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType());
        String objectName = resolveApprovalObjectName(instance);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(checklistApproval ? AuditModuleCodeEnum.PROJECT.getCode() : AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType(instance.getBizType() != null ? instance.getBizType() : BIZ_TYPE_DOCUMENT)
                .bizId(instance.getDocumentId() != null ? instance.getDocumentId() : instance.getBizId())
                .operationType(operationType)
                .objectName(objectName)
                .actionSummary(buildApprovalSummary(operationType, objectName))
                .relatedBizType("APPROVAL_INSTANCE")
                .relatedBizId(instance.getId())
                .afterData(afterData)
                .build());
    }

    private String resolveApprovalObjectName(ApprovalInstanceEntity instance) {
        if (BIZ_TYPE_CHECKLIST_ITEM.equals(instance.getBizType()) && instance.getBizId() != null) {
            ProjectChecklistItemEntity item = projectChecklistItemMapper.selectById(instance.getBizId());
            if (item != null && StringUtils.hasText(item.getItemName())) {
                return item.getItemName();
            }
        }
        if (instance.getDocumentId() != null) {
            DocumentEntity document = documentMapper.selectById(instance.getDocumentId());
            if (document != null && StringUtils.hasText(document.getName())) {
                return document.getName();
            }
        }
        return null;
    }

    private String buildApprovalSummary(String operationType, String objectName) {
        String actor = currentActorName();
        String target = StringUtils.hasText(objectName) ? "《" + objectName + "》" : "";
        if (AuditOperationTypeEnum.APPROVAL_SUBMIT.getCode().equals(operationType)) {
            return actor + " 提交了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_APPROVE.getCode().equals(operationType)) {
            return actor + " 通过了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_REJECT.getCode().equals(operationType)) {
            return actor + " 驳回了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_WITHDRAW.getCode().equals(operationType)) {
            return actor + " 撤回了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_TRANSFER.getCode().equals(operationType)) {
            return actor + " 转交了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_ADD_SIGN.getCode().equals(operationType)) {
            return actor + " 加签了" + target + "审批";
        }
        if (AuditOperationTypeEnum.APPROVAL_TERMINATE.getCode().equals(operationType)) {
            return actor + " 终止了" + target + "审批";
        }
        return actor + " 更新了" + target + "审批";
    }

    private String currentActorName() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            return "系统";
        }
        return StringUtils.hasText(user.getUsername()) ? user.getUsername() : String.valueOf(user.getUserId());
    }

    private Stream<ApprovalHistoryRespDTO> enrichHistory(ApprovalInstanceEntity instance) {
        List<ApprovalActionLogEntity> logs = approvalActionLogMapper.selectList(
                Wrappers.<ApprovalActionLogEntity>lambdaQuery()
                        .eq(ApprovalActionLogEntity::getInstanceId, instance.getId())
                        .eq(ApprovalActionLogEntity::getDeleted, false)
                        .orderByAsc(ApprovalActionLogEntity::getActionAt)
                        .orderByAsc(ApprovalActionLogEntity::getCreatedAt));
        if (logs != null && !logs.isEmpty()) {
            return logs.stream().map(log -> toHistoryRespDTO(instance, log));
        }
        return approvalTaskMapper.selectList(Wrappers.<ApprovalTaskEntity>lambdaQuery()
                .eq(ApprovalTaskEntity::getInstanceId, instance.getId())
                .eq(ApprovalTaskEntity::getDeleted, false)
        ).stream().map(task -> toHistoryRespDTO(instance, task));
    }

    private ApprovalHistoryRespDTO toHistoryRespDTO(ApprovalInstanceEntity instance, ApprovalActionLogEntity log) {
        ApprovalHistoryRespDTO dto = baseHistoryRespDTO(instance);
        dto.setTaskId(log.getTaskId());
        dto.setDefinitionName(resolveDefinitionName(log.getDefinitionId()));
        dto.setNodeName(resolveNodeName(log.getNodeId()));
        dto.setActionType(log.getActionType());
        dto.setActionComment(log.getActionComment());
        dto.setActionTime(log.getActionAt());
        dto.setHandlerUserId(log.getActionUserId());
        dto.setTaskStatus(log.getAfterStatus());
        dto.setTaskComment(log.getActionComment());
        dto.setHandledAt(log.getActionAt());
        if (log.getTaskId() != null) {
            ApprovalTaskEntity task = approvalTaskMapper.selectById(log.getTaskId());
            if (task != null) {
                dto.setApproverUserId(task.getApproverUserId());
            }
        }
        return dto;
    }

    private ApprovalHistoryRespDTO baseHistoryRespDTO(ApprovalInstanceEntity instance) {
        ApprovalHistoryRespDTO dto = new ApprovalHistoryRespDTO();
        dto.setInstanceId(instance.getId());
        dto.setDocumentId(instance.getDocumentId());
        dto.setVersionNo(instance.getVersionNo());
        dto.setDefinitionName(resolveDefinitionName(instance.getDefinitionId()));
        dto.setBizModule(instance.getBizModule());
        dto.setBizId(instance.getBizId());
        dto.setBizType(instance.getBizType());
        dto.setSubmitterUserId(instance.getSubmitterUserId());
        dto.setInstanceStatus(instance.getStatus());
        dto.setSubmitComment(instance.getSubmitComment());
        dto.setSubmittedAt(instance.getSubmittedAt());
        dto.setCompletedAt(instance.getCompletedAt());
        return dto;
    }

    private String resolveDefinitionName(Long definitionId) {
        if (definitionId == null) {
            return null;
        }
        ApprovalDefinitionEntity definition = approvalDefinitionMapper.selectById(definitionId);
        return definition != null ? definition.getName() : null;
    }

    private String resolveNodeName(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        ApprovalNodeEntity node = approvalNodeMapper.selectById(nodeId);
        return node != null ? node.getNodeName() : null;
    }
}
