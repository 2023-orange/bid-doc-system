package com.example.biddoc.workflow.service.impl;

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
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.workflow.entity.ApprovalConditionEntity;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskCandidateEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalConditionMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskCandidateMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalFlowEngineServiceImplTest {

    private final ApprovalDefinitionServiceImpl approvalDefinitionService = mock(ApprovalDefinitionServiceImpl.class);
    private final ApprovalNodeMapper approvalNodeMapper = mock(ApprovalNodeMapper.class);
    private final ApprovalConditionMapper approvalConditionMapper = mock(ApprovalConditionMapper.class);
    private final ApprovalTaskMapper approvalTaskMapper = mock(ApprovalTaskMapper.class);
    private final ApprovalTaskCandidateMapper approvalTaskCandidateMapper = mock(ApprovalTaskCandidateMapper.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysDepartmentMapper sysDepartmentMapper = mock(SysDepartmentMapper.class);
    private final ProjectMemberMapper projectMemberMapper = mock(ProjectMemberMapper.class);
    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final ApprovalFlowEngineServiceImpl service = new ApprovalFlowEngineServiceImpl(
            approvalDefinitionService,
            approvalNodeMapper,
            approvalConditionMapper,
            approvalTaskMapper,
            approvalTaskCandidateMapper,
            userRoleService,
            sysUserMapper,
            projectMemberMapper,
            sysDepartmentMapper,
            projectMapper,
            documentMapper
    );

    @Test
    void resolveRoleApproverSkipsUnavailableUsers() {
        ApprovalNodeEntity node = approvalNode("ROLE", "DEPT_MANAGER");
        when(userRoleService.listUserIdsByRoleCode("DEPT_MANAGER")).thenReturn(List.of(11L, 12L, 13L));
        when(sysUserMapper.selectById(11L)).thenReturn(user(11L, 0, false));
        when(sysUserMapper.selectById(12L)).thenReturn(user(12L, 1, true));
        when(sysUserMapper.selectById(13L)).thenReturn(user(13L, 1, false));

        Long approverUserId = service.resolveApprover(node, instance(), null, null, submitter());

        assertEquals(13L, approverUserId);
    }

    @Test
    void resolveApproverSupportsUserFolderOwnerAndProjectOwner() {
        assertEquals(21L, service.resolveApprover(approvalNode("USER", "21"),
                instance(), null, null, submitter()));

        FolderEntity folder = new FolderEntity();
        folder.setOwnerUserId(31L);
        assertEquals(31L, service.resolveApprover(approvalNode("FOLDER_OWNER", null),
                instance(), folder, null, submitter()));

        ProjectMemberEntity owner = new ProjectMemberEntity();
        owner.setUserId(41L);
        when(projectMemberMapper.selectOne(any())).thenReturn(owner);
        assertEquals(41L, service.resolveApprover(approvalNode("PROJECT_OWNER", null),
                instance(), null, 900L, submitter()));
    }

    @Test
    void resolveApproverSupportsDepartmentManager() {
        SysDepartment department = new SysDepartment();
        department.setId(10L);
        department.setManagerUserId(81L);
        department.setStatus(1);
        department.setDeleted(false);
        when(sysDepartmentMapper.selectById(10L)).thenReturn(department);
        when(sysUserMapper.selectById(81L)).thenReturn(user(81L, 1, false));

        Long approverUserId = service.resolveApprover(approvalNode("DEPT_MANAGER", null),
                instance(), null, null, submitter());

        assertEquals(81L, approverUserId);
    }

    @Test
    void createFirstTaskWritesTaskAndCandidateSnapshot() {
        ApprovalInstanceEntity instance = instance();
        ApprovalNodeEntity node = approvalNode("USER", "21");
        node.setId(301L);
        node.setDefinitionId(instance.getDefinitionId());
        node.setNodeCode("DEPT_REVIEW");
        when(approvalNodeMapper.selectOne(any())).thenReturn(node);

        ApprovalTaskEntity task = service.createFirstTask(instance, null, null, submitter());

        assertEquals(21L, task.getApproverUserId());
        assertEquals("DEPT_REVIEW", instance.getCurrentNodeCode());
        ArgumentCaptor<ApprovalTaskEntity> taskCaptor = ArgumentCaptor.forClass(ApprovalTaskEntity.class);
        verify(approvalTaskMapper).insert(taskCaptor.capture());
        assertEquals(instance.getDefinitionId(), taskCaptor.getValue().getDefinitionId());
        assertEquals("DEPT_REVIEW", taskCaptor.getValue().getNodeCode());

        ArgumentCaptor<ApprovalTaskCandidateEntity> candidateCaptor =
                ArgumentCaptor.forClass(ApprovalTaskCandidateEntity.class);
        verify(approvalTaskCandidateMapper).insert(candidateCaptor.capture());
        ApprovalTaskCandidateEntity candidate = candidateCaptor.getValue();
        assertEquals("USER", candidate.getCandidateType());
        assertEquals("21", candidate.getCandidateValue());
        assertEquals(21L, candidate.getCandidateUserId());
        assertTrue(candidate.getResolved());
    }

    @Test
    void createFirstTaskFollowsMatchedConditionTarget() {
        ApprovalInstanceEntity instance = instance();
        instance.setScenario("DOCUMENT_APPROVAL");
        ApprovalNodeEntity conditionNode = conditionNode("GATE", "DEFAULT_REVIEW");
        ApprovalConditionEntity condition = condition("scenario", "EQ", "DOCUMENT_APPROVAL", "TECH_REVIEW");
        ApprovalNodeEntity target = approvalNode("USER", "21");
        target.setId(401L);
        target.setDefinitionId(instance.getDefinitionId());
        target.setNodeCode("TECH_REVIEW");
        when(approvalNodeMapper.selectOne(any())).thenReturn(conditionNode);
        when(approvalConditionMapper.selectList(any())).thenReturn(List.of(condition));
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "TECH_REVIEW")).thenReturn(target);

        ApprovalTaskEntity task = service.createFirstTask(instance, null, null, submitter());

        assertEquals(21L, task.getApproverUserId());
        assertEquals("TECH_REVIEW", instance.getCurrentNodeCode());
    }

    @Test
    void createFirstTaskFollowsProjectTypeConditionTarget() {
        ApprovalInstanceEntity instance = instance();
        ProjectEntity project = new ProjectEntity();
        project.setId(900L);
        project.setProjectType("PUBLIC_TENDER");

        ApprovalNodeEntity conditionNode = conditionNode("GATE", "DEFAULT_REVIEW");
        ApprovalConditionEntity condition = condition("projectType", "EQ", "PUBLIC_TENDER", "TECH_REVIEW");
        ApprovalNodeEntity matchedTarget = approvalNode("USER", "21");
        matchedTarget.setId(401L);
        matchedTarget.setDefinitionId(instance.getDefinitionId());
        matchedTarget.setNodeCode("TECH_REVIEW");
        ApprovalNodeEntity defaultTarget = approvalNode("USER", "31");
        defaultTarget.setId(402L);
        defaultTarget.setDefinitionId(instance.getDefinitionId());
        defaultTarget.setNodeCode("DEFAULT_REVIEW");
        when(projectMapper.selectById(900L)).thenReturn(project);
        when(approvalNodeMapper.selectOne(any())).thenReturn(conditionNode);
        when(approvalConditionMapper.selectList(any())).thenReturn(List.of(condition));
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "TECH_REVIEW")).thenReturn(matchedTarget);
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "DEFAULT_REVIEW")).thenReturn(defaultTarget);

        ApprovalTaskEntity task = service.createFirstTask(instance, null, 900L, submitter());

        assertEquals(21L, task.getApproverUserId());
        assertEquals("TECH_REVIEW", instance.getCurrentNodeCode());
    }

    @Test
    void createFirstTaskFollowsSensitiveLevelConditionTarget() {
        ApprovalInstanceEntity instance = instance();
        DocumentEntity document = new DocumentEntity();
        document.setId(300L);
        document.setSensitiveLevel("SECRET");

        ApprovalNodeEntity conditionNode = conditionNode("GATE", "DEFAULT_REVIEW");
        ApprovalConditionEntity condition = condition("sensitiveLevel", "EQ", "SECRET", "SECURITY_REVIEW");
        ApprovalNodeEntity matchedTarget = approvalNode("USER", "21");
        matchedTarget.setId(501L);
        matchedTarget.setDefinitionId(instance.getDefinitionId());
        matchedTarget.setNodeCode("SECURITY_REVIEW");
        ApprovalNodeEntity defaultTarget = approvalNode("USER", "31");
        defaultTarget.setId(502L);
        defaultTarget.setDefinitionId(instance.getDefinitionId());
        defaultTarget.setNodeCode("DEFAULT_REVIEW");
        when(documentMapper.selectById(300L)).thenReturn(document);
        when(approvalNodeMapper.selectOne(any())).thenReturn(conditionNode);
        when(approvalConditionMapper.selectList(any())).thenReturn(List.of(condition));
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "SECURITY_REVIEW")).thenReturn(matchedTarget);
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "DEFAULT_REVIEW")).thenReturn(defaultTarget);

        ApprovalTaskEntity task = service.createFirstTask(instance, null, null, submitter());

        assertEquals(21L, task.getApproverUserId());
        assertEquals("SECURITY_REVIEW", instance.getCurrentNodeCode());
    }

    @Test
    void conditionLoopGuardRejectsOverDeepConditionChain() {
        ApprovalInstanceEntity instance = instance();
        ApprovalNodeEntity conditionNode = conditionNode("LOOP", null);
        ApprovalConditionEntity condition = condition("scenario", "ANY", null, "LOOP");
        when(approvalNodeMapper.selectOne(any())).thenReturn(conditionNode);
        when(approvalConditionMapper.selectList(any())).thenReturn(List.of(condition));
        when(approvalDefinitionService.findNodeByCode(instance.getDefinitionId(), "LOOP")).thenReturn(conditionNode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createFirstTask(instance, null, null, submitter()));

        assertEquals(ErrorCode.APPROVAL_FLOW_INVALID, ex.getErrorCode());
    }

    private ApprovalNodeEntity approvalNode(String assigneeType, String assigneeValue) {
        ApprovalNodeEntity node = new ApprovalNodeEntity();
        node.setNodeType("APPROVAL");
        node.setAssigneeType(assigneeType);
        node.setAssigneeValue(assigneeValue);
        return node;
    }

    private ApprovalNodeEntity conditionNode(String nodeCode, String nextNodeCode) {
        ApprovalNodeEntity node = new ApprovalNodeEntity();
        node.setId(300L);
        node.setDefinitionId(200L);
        node.setNodeType("CONDITION");
        node.setNodeCode(nodeCode);
        node.setNextNodeCode(nextNodeCode);
        return node;
    }

    private ApprovalConditionEntity condition(String fieldName, String operator, String compareValue, String targetNodeCode) {
        ApprovalConditionEntity condition = new ApprovalConditionEntity();
        condition.setFieldName(fieldName);
        condition.setOperator(operator);
        condition.setCompareValue(compareValue);
        condition.setTargetNodeCode(targetNodeCode);
        condition.setDeleted(false);
        return condition;
    }

    private SysUser user(Long id, Integer status, Boolean deleted) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }

    private ApprovalInstanceEntity instance() {
        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(100L);
        instance.setDefinitionId(200L);
        instance.setDocumentId(300L);
        return instance;
    }

    private UserContext.UserInfo submitter() {
        return new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 10L);
    }
}
