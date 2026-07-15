package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalConditionEntity;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskCandidateEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalConditionMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskCandidateMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import com.example.biddoc.workflow.service.ApprovalFlowEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApprovalFlowEngineServiceImpl implements ApprovalFlowEngineService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CONDITION = "CONDITION";
    private static final String NODE_TYPE_END = "END";
    private static final int USER_STATUS_ENABLED = 1;

    private final ApprovalDefinitionServiceImpl approvalDefinitionService;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalConditionMapper approvalConditionMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ApprovalTaskCandidateMapper approvalTaskCandidateMapper;
    private final UserRoleService userRoleService;
    private final SysUserMapper sysUserMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final ProjectMapper projectMapper;
    private final DocumentMapper documentMapper;

    @Override
    public ApprovalDefinitionEntity matchDefinition(String scenario, String bizModule, String bizType,
                                                    Long deptId, String businessCategory) {
        return approvalDefinitionService.findEnabledDefinition(scenario, bizModule, bizType, deptId, businessCategory);
    }

    @Override
    public ApprovalTaskEntity createFirstTask(ApprovalInstanceEntity instance, FolderEntity folder,
                                              Long projectId, UserContext.UserInfo submitter) {
        ApprovalNodeEntity node = approvalNodeMapper.selectOne(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                .eq(ApprovalNodeEntity::getDefinitionId, instance.getDefinitionId())
                .eq(ApprovalNodeEntity::getDeleted, false)
                .orderByAsc(ApprovalNodeEntity::getSortOrder)
                .orderByAsc(ApprovalNodeEntity::getCreatedAt)
                .last("limit 1"));
        node = resolveExecutableNode(instance, node, projectId, submitter);
        if (node == null || NODE_TYPE_END.equals(node.getNodeType())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "流程未配置有效审批节点");
        }
        return createTaskForNode(instance, node, folder, projectId, submitter, null, false);
    }

    @Override
    public ApprovalTaskEntity createNextTask(ApprovalInstanceEntity instance, ApprovalTaskEntity currentTask,
                                             FolderEntity folder, Long projectId, UserContext.UserInfo submitter) {
        ApprovalNodeEntity currentNode = approvalNodeMapper.selectById(currentTask.getNodeId());
        if (currentNode == null || !StringUtils.hasText(currentNode.getNextNodeCode())) {
            return null;
        }
        ApprovalNodeEntity nextNode = approvalDefinitionService.findNodeByCode(instance.getDefinitionId(),
                currentNode.getNextNodeCode());
        nextNode = resolveExecutableNode(instance, nextNode, projectId, submitter);
        if (nextNode == null || NODE_TYPE_END.equals(nextNode.getNodeType())) {
            return null;
        }
        return createTaskForNode(instance, nextNode, folder, projectId, submitter, null, false);
    }

    @Override
    public Long resolveApprover(ApprovalNodeEntity node, ApprovalInstanceEntity instance, FolderEntity folder,
                                Long projectId, UserContext.UserInfo submitter) {
        if (!NODE_TYPE_APPROVAL.equals(node.getNodeType())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "当前节点不是审批节点");
        }
        String type = node.getAssigneeType();
        if ("USER".equals(type)) {
            return parseUserId(node.getAssigneeValue());
        }
        if ("ROLE".equals(type)) {
            List<Long> userIds = userRoleService.listUserIdsByRoleCode(node.getAssigneeValue());
            if (userIds == null || userIds.isEmpty()) {
                throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "角色下未找到审批人");
            }
            // 角色关系有效不代表用户账号可用；创建待办前过滤停用或软删除用户，避免任务落到不可处理账号。
            return userIds.stream()
                    .filter(this::isAvailableUser)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "角色下未找到可用审批人"));
        }
        if ("PROJECT_OWNER".equals(type)) {
            return resolveProjectOwner(projectId);
        }
        if ("FOLDER_OWNER".equals(type)) {
            if (folder == null || folder.getOwnerUserId() == null) {
                throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "未找到文件夹负责人");
            }
            return folder.getOwnerUserId();
        }
        if ("DEPT_MANAGER".equals(type)) {
            return resolveDepartmentManager(projectId, submitter);
        }
        throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "暂不支持的审批人规则");
    }

    private ApprovalTaskEntity createTaskForNode(ApprovalInstanceEntity instance, ApprovalNodeEntity node,
                                                FolderEntity folder, Long projectId, UserContext.UserInfo submitter,
                                                Long transferredFromTaskId, boolean addSign) {
        Long approverUserId = resolveApprover(node, instance, folder, projectId, submitter);
        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(IdWorker.getId());
        task.setInstanceId(instance.getId());
        task.setDocumentId(instance.getDocumentId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setNodeId(node.getId());
        task.setNodeCode(node.getNodeCode());
        task.setApproverUserId(approverUserId);
        task.setStatus(STATUS_PENDING);
        task.setTransferredFromTaskId(transferredFromTaskId);
        task.setAddSign(addSign);
        task.setDeleted(Boolean.FALSE);
        approvalTaskMapper.insert(task);

        ApprovalTaskCandidateEntity candidate = new ApprovalTaskCandidateEntity();
        candidate.setId(IdWorker.getId());
        candidate.setTaskId(task.getId());
        candidate.setInstanceId(instance.getId());
        candidate.setDefinitionId(instance.getDefinitionId());
        candidate.setNodeId(node.getId());
        candidate.setNodeCode(node.getNodeCode());
        candidate.setCandidateType(node.getAssigneeType());
        candidate.setCandidateValue(node.getAssigneeValue());
        candidate.setCandidateUserId(approverUserId);
        candidate.setResolved(Boolean.TRUE);
        candidate.setDeleted(Boolean.FALSE);
        approvalTaskCandidateMapper.insert(candidate);

        instance.setCurrentNodeId(node.getId());
        instance.setCurrentNodeCode(node.getNodeCode());
        return task;
    }

    private ApprovalNodeEntity resolveExecutableNode(ApprovalInstanceEntity instance, ApprovalNodeEntity node,
                                                    Long projectId, UserContext.UserInfo submitter) {
        ApprovalNodeEntity current = node;
        for (int guard = 0; guard < 20 && current != null && NODE_TYPE_CONDITION.equals(current.getNodeType()); guard++) {
            current = resolveConditionTarget(instance, current, projectId, submitter);
        }
        if (current != null && NODE_TYPE_CONDITION.equals(current.getNodeType())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "条件节点链路过深或存在循环");
        }
        return current;
    }

    private ApprovalNodeEntity resolveConditionTarget(ApprovalInstanceEntity instance, ApprovalNodeEntity conditionNode,
                                                     Long projectId, UserContext.UserInfo submitter) {
        List<ApprovalConditionEntity> conditions = approvalConditionMapper.selectList(
                Wrappers.<ApprovalConditionEntity>lambdaQuery()
                        .eq(ApprovalConditionEntity::getDefinitionId, instance.getDefinitionId())
                        .eq(ApprovalConditionEntity::getNodeId, conditionNode.getId())
                        .eq(ApprovalConditionEntity::getDeleted, false)
                        .orderByAsc(ApprovalConditionEntity::getSortOrder)
                        .orderByAsc(ApprovalConditionEntity::getCreatedAt));
        for (ApprovalConditionEntity condition : conditions) {
            if (matchCondition(instance, condition, projectId, submitter)) {
                return approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), condition.getTargetNodeCode());
            }
        }
        if (StringUtils.hasText(conditionNode.getNextNodeCode())) {
            return approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), conditionNode.getNextNodeCode());
        }
        return null;
    }

    private boolean matchCondition(ApprovalInstanceEntity instance, ApprovalConditionEntity condition,
                                   Long projectId, UserContext.UserInfo submitter) {
        String actual = conditionFieldValue(instance, condition.getFieldName(), projectId, submitter);
        String expected = condition.getCompareValue();
        String operator = StringUtils.hasText(condition.getOperator()) ? condition.getOperator() : "EQ";
        if ("ANY".equalsIgnoreCase(operator)) {
            return true;
        }
        if ("IS_NULL".equalsIgnoreCase(operator)) {
            return !StringUtils.hasText(actual);
        }
        if ("NOT_NULL".equalsIgnoreCase(operator)) {
            return StringUtils.hasText(actual);
        }
        if ("NE".equalsIgnoreCase(operator)) {
            return !Objects.equals(actual, expected);
        }
        if ("IN".equalsIgnoreCase(operator)) {
            return StringUtils.hasText(expected) && actual != null
                    && List.of(expected.split(",")).stream().map(String::trim).anyMatch(actual::equals);
        }
        return Objects.equals(actual, expected);
    }

    private String conditionFieldValue(ApprovalInstanceEntity instance, String fieldName,
                                      Long projectId, UserContext.UserInfo submitter) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }
        return switch (fieldName) {
            case "scenario" -> instance.getScenario();
            case "bizModule" -> instance.getBizModule();
            case "bizType" -> instance.getBizType();
            case "bizId" -> instance.getBizId() != null ? String.valueOf(instance.getBizId()) : null;
            case "documentId" -> instance.getDocumentId() != null ? String.valueOf(instance.getDocumentId()) : null;
            case "versionNo" -> instance.getVersionNo() != null ? String.valueOf(instance.getVersionNo()) : null;
            case "submitterUserId" -> instance.getSubmitterUserId() != null ? String.valueOf(instance.getSubmitterUserId()) : null;
            case "deptId", "submitterDeptId", "departmentId" -> submitter != null && submitter.getDeptId() != null
                    ? String.valueOf(submitter.getDeptId())
                    : null;
            case "projectType" -> {
                ProjectEntity project = loadProject(projectId);
                yield project != null ? project.getProjectType() : null;
            }
            case "projectStage" -> {
                ProjectEntity project = loadProject(projectId);
                yield project != null ? project.getProjectStage() : null;
            }
            case "projectStatus" -> {
                ProjectEntity project = loadProject(projectId);
                yield project != null ? project.getProjectStatus() : null;
            }
            case "businessCategory", "documentCategory" -> {
                DocumentEntity document = loadDocument(instance);
                yield document != null ? document.getBusinessCategory() : null;
            }
            case "sensitiveLevel" -> {
                DocumentEntity document = loadDocument(instance);
                yield document != null ? document.getSensitiveLevel() : null;
            }
            case "isExpired", "expired" -> String.valueOf(isDocumentExpired(loadDocument(instance)));
            case "ownerDeptId", "projectDeptId" -> {
                ProjectEntity project = loadProject(projectId);
                yield project != null && project.getOwnerDeptId() != null
                        ? String.valueOf(project.getOwnerDeptId())
                        : null;
            }
            case "isCrossDept", "crossDept" -> String.valueOf(isCrossDept(projectId, submitter));
            default -> null;
        };
    }

    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "未配置指定审批人");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "指定审批人配置必须是用户ID");
        }
    }

    private boolean isAvailableUser(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user != null
                && !Boolean.TRUE.equals(user.getDeleted())
                && Integer.valueOf(USER_STATUS_ENABLED).equals(user.getStatus());
    }

    private Long resolveDepartmentManager(Long projectId, UserContext.UserInfo submitter) {
        Long deptId = submitter != null ? submitter.getDeptId() : null;
        if (deptId == null) {
            ProjectEntity project = loadProject(projectId);
            deptId = project != null ? project.getOwnerDeptId() : null;
        }
        if (deptId == null) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "未找到审批部门");
        }
        SysDepartment department = sysDepartmentMapper.selectById(deptId);
        if (department == null
                || Boolean.TRUE.equals(department.getDeleted())
                || !Integer.valueOf(USER_STATUS_ENABLED).equals(department.getStatus())
                || department.getManagerUserId() == null) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "部门未配置负责人");
        }
        // 部门负责人规则也必须复用账号可用性校验，避免待办流向停用或已删除用户。
        if (!isAvailableUser(department.getManagerUserId())) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "部门负责人账号不可用");
        }
        return department.getManagerUserId();
    }

    private boolean isCrossDept(Long projectId, UserContext.UserInfo submitter) {
        if (submitter == null || submitter.getDeptId() == null) {
            return false;
        }
        ProjectEntity project = loadProject(projectId);
        return project != null
                && project.getOwnerDeptId() != null
                && !Objects.equals(project.getOwnerDeptId(), submitter.getDeptId());
    }

    private ProjectEntity loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectMapper.selectById(projectId);
    }

    private DocumentEntity loadDocument(ApprovalInstanceEntity instance) {
        Long documentId = instance != null ? instance.getDocumentId() : null;
        if (documentId == null) {
            return null;
        }
        // 审批条件读取资料元数据，统一从资料主表取当前事实，避免流程配置依赖前端传参。
        return documentMapper.selectById(documentId);
    }

    private boolean isDocumentExpired(DocumentEntity document) {
        return document != null
                && Boolean.TRUE.equals(document.getHasExpireDate())
                && document.getExpireDate() != null
                && document.getExpireDate().isBefore(java.time.OffsetDateTime.now());
    }

    private Long resolveProjectOwner(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "未找到项目负责人");
        }
        ProjectMemberEntity owner = projectMemberMapper.selectOne(Wrappers.<ProjectMemberEntity>lambdaQuery()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getMemberRole, ProjectMemberRoleEnum.OWNER.getCode())
                .eq(ProjectMemberEntity::getDeleted, false)
                .last("limit 1"));
        if (owner == null || Objects.isNull(owner.getUserId())) {
            throw new BusinessException(ErrorCode.APPROVAL_APPROVER_INVALID, "项目未配置负责人");
        }
        return owner.getUserId();
    }
}
