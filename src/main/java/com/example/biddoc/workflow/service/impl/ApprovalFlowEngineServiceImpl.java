package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
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

    private final ApprovalDefinitionServiceImpl approvalDefinitionService;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalConditionMapper approvalConditionMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ApprovalTaskCandidateMapper approvalTaskCandidateMapper;
    private final UserRoleService userRoleService;
    private final ProjectMemberMapper projectMemberMapper;

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
        node = resolveExecutableNode(instance, node);
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
        nextNode = resolveExecutableNode(instance, nextNode);
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
            return userIds.get(0);
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

    private ApprovalNodeEntity resolveExecutableNode(ApprovalInstanceEntity instance, ApprovalNodeEntity node) {
        ApprovalNodeEntity current = node;
        for (int guard = 0; guard < 20 && current != null && NODE_TYPE_CONDITION.equals(current.getNodeType()); guard++) {
            current = resolveConditionTarget(instance, current);
        }
        if (current != null && NODE_TYPE_CONDITION.equals(current.getNodeType())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "条件节点链路过深或存在循环");
        }
        return current;
    }

    private ApprovalNodeEntity resolveConditionTarget(ApprovalInstanceEntity instance, ApprovalNodeEntity conditionNode) {
        List<ApprovalConditionEntity> conditions = approvalConditionMapper.selectList(
                Wrappers.<ApprovalConditionEntity>lambdaQuery()
                        .eq(ApprovalConditionEntity::getDefinitionId, instance.getDefinitionId())
                        .eq(ApprovalConditionEntity::getNodeId, conditionNode.getId())
                        .eq(ApprovalConditionEntity::getDeleted, false)
                        .orderByAsc(ApprovalConditionEntity::getSortOrder)
                        .orderByAsc(ApprovalConditionEntity::getCreatedAt));
        for (ApprovalConditionEntity condition : conditions) {
            if (matchCondition(instance, condition)) {
                return approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), condition.getTargetNodeCode());
            }
        }
        if (StringUtils.hasText(conditionNode.getNextNodeCode())) {
            return approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), conditionNode.getNextNodeCode());
        }
        return null;
    }

    private boolean matchCondition(ApprovalInstanceEntity instance, ApprovalConditionEntity condition) {
        String actual = conditionFieldValue(instance, condition.getFieldName());
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

    private String conditionFieldValue(ApprovalInstanceEntity instance, String fieldName) {
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
