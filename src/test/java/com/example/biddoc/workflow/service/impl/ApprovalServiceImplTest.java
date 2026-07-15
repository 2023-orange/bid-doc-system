package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.service.ProjectChecklistService;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.service.ProjectPermissionService;
import com.example.biddoc.workflow.dto.resp.ApprovalHandleResultRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalActionLogMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskCandidateMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import com.example.biddoc.workflow.service.ApprovalFlowEngineService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceImplTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalTaskEntity.class);
    }

    private final ApprovalInstanceMapper approvalInstanceMapper = mock(ApprovalInstanceMapper.class);
    private final ApprovalTaskMapper approvalTaskMapper = mock(ApprovalTaskMapper.class);
    private final ApprovalActionLogMapper approvalActionLogMapper = mock(ApprovalActionLogMapper.class);
    private final ApprovalDefinitionMapper approvalDefinitionMapper = mock(ApprovalDefinitionMapper.class);
    private final ApprovalNodeMapper approvalNodeMapper = mock(ApprovalNodeMapper.class);
    private final ApprovalTaskCandidateMapper approvalTaskCandidateMapper = mock(ApprovalTaskCandidateMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final DocumentVersionMapper documentVersionMapper = mock(DocumentVersionMapper.class);
    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final ProjectChecklistService projectChecklistService = mock(ProjectChecklistService.class);
    private final ProjectChecklistItemMapper projectChecklistItemMapper = mock(ProjectChecklistItemMapper.class);
    private final ProjectMemberMapper projectMemberMapper = mock(ProjectMemberMapper.class);
    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final ApprovalFlowEngineService approvalFlowEngineService = mock(ApprovalFlowEngineService.class);
    private final ApprovalServiceImpl service = new ApprovalServiceImpl(
            approvalInstanceMapper,
            approvalTaskMapper,
            approvalActionLogMapper,
            approvalDefinitionMapper,
            approvalNodeMapper,
            approvalTaskCandidateMapper,
            sysUserMapper,
            documentMapper,
            documentVersionMapper,
            folderMapper,
            folderPermissionService,
            auditService,
            notificationService,
            documentService,
            projectChecklistService,
            projectChecklistItemMapper,
            projectMemberMapper,
            projectMapper,
            projectPermissionService,
            approvalFlowEngineService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void submitCreatesInstanceAndTaskForFolderOwner() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(100L);
        document.setName("方案.pdf");
        document.setFolderId(10L);

        FolderEntity folder = new FolderEntity();
        folder.setId(10L);
        folder.setOwnerUserId(1L);

        when(documentMapper.selectById(100L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        when(approvalFlowEngineService.matchDefinition(any(), any(), any(), any(), any())).thenReturn(null);
        doNothing().when(folderPermissionService).checkView(folder);

        service.submit(100L, "请审批");

        verify(approvalInstanceMapper).insert(argThat(instance ->
                Long.valueOf(100L).equals(instance.getDocumentId())
                        && Long.valueOf(7L).equals(instance.getSubmitterUserId())
                        && "PENDING".equals(instance.getStatus())
        ));
        verify(approvalTaskMapper).insert(any(ApprovalTaskEntity.class));
        verify(notificationService).send(1L, "WORKFLOW_TASK", "新的文档审批任务", "请审批文档：方案.pdf", "DOCUMENT", 100L);
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getBizType())
                        && "APPROVAL_SUBMIT".equals(command.getOperationType())
                        && "方案.pdf".equals(command.getObjectName())
                        && "APPROVAL_INSTANCE".equals(command.getRelatedBizType())
                        && command.getRelatedBizId() != null
                        && command.getActionSummary() != null
                        && command.getActionSummary().contains("提交了《方案.pdf》审批")
        ));
    }

    @Test
    void submitWithDefinitionStoresDefinitionSnapshotAndCreatesFirstTask() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(100L);
        document.setName("配置化方案.pdf");
        document.setFolderId(10L);
        document.setOwnerDeptId(200L);
        document.setBusinessCategory("TECHNICAL");

        FolderEntity folder = new FolderEntity();
        folder.setId(10L);
        folder.setOwnerUserId(1L);

        ApprovalDefinitionEntity definition = new ApprovalDefinitionEntity();
        definition.setId(500L);
        definition.setVersion(3);

        ApprovalTaskEntity firstTask = new ApprovalTaskEntity();
        firstTask.setId(600L);
        firstTask.setApproverUserId(1L);

        when(documentMapper.selectById(100L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        when(approvalFlowEngineService.matchDefinition("DOCUMENT_APPROVAL", "DOCUMENT", "DOCUMENT", 200L, "TECHNICAL"))
                .thenReturn(definition);
        when(approvalFlowEngineService.createFirstTask(any(), any(), any(), any())).thenAnswer(invocation -> {
            ApprovalInstanceEntity instance = invocation.getArgument(0);
            instance.setCurrentNodeId(700L);
            instance.setCurrentNodeCode("DEPT_REVIEW");
            firstTask.setInstanceId(instance.getId());
            firstTask.setDocumentId(instance.getDocumentId());
            firstTask.setDefinitionId(instance.getDefinitionId());
            firstTask.setNodeId(instance.getCurrentNodeId());
            firstTask.setNodeCode(instance.getCurrentNodeCode());
            return firstTask;
        });
        doNothing().when(folderPermissionService).checkView(folder);

        service.submit(100L, "请走配置化审批");

        ArgumentCaptor<ApprovalInstanceEntity> captor = ArgumentCaptor.forClass(ApprovalInstanceEntity.class);
        verify(approvalInstanceMapper).insert(captor.capture());
        ApprovalInstanceEntity instance = captor.getValue();
        assertEquals(500L, instance.getDefinitionId());
        assertEquals(3, instance.getDefinitionVersion());
        assertEquals("DOCUMENT_APPROVAL", instance.getScenario());
        assertEquals("DOCUMENT", instance.getBizModule());
        assertEquals("DOCUMENT", instance.getBizType());
        assertEquals(100L, instance.getBizId());
        verify(approvalFlowEngineService).createFirstTask(argThat(created ->
                        Long.valueOf(500L).equals(created.getDefinitionId())
                                && Integer.valueOf(3).equals(created.getDefinitionVersion())),
                argThat(createdFolder -> Long.valueOf(10L).equals(createdFolder.getId())),
                any(),
                any());
        verify(approvalInstanceMapper).updateById(argThat(updated ->
                Long.valueOf(700L).equals(updated.getCurrentNodeId())
                        && "DEPT_REVIEW".equals(updated.getCurrentNodeCode())));
        verify(documentService).markApproving(100L);
    }

    @Test
    void approveCompletesTaskAndInstance() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));

        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(20L);
        task.setInstanceId(30L);
        task.setApproverUserId(1L);
        task.setStatus("PENDING");

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(30L);
        instance.setDocumentId(100L);
        instance.setSubmitterUserId(7L);
        instance.setStatus("PENDING");
        instance.setDefinitionId(null);

        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalFlowEngineService.createNextTask(any(), any(), any(), any(), any())).thenReturn(null);

        ApprovalHandleResultRespDTO result = service.approve(20L, "同意");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("APPROVED", instance.getStatus());
        assertEquals("APPROVED", result.getTaskStatus());
        assertEquals("APPROVED", result.getInstanceStatus());
        assertTrue(result.getCompleted());
        assertNull(result.getNextTaskId());
        assertNull(result.getNextNodeName());
        verify(approvalTaskMapper).updateById(task);
        verify(approvalInstanceMapper).updateById(instance);
        verify(documentService).markApprovalResult(100L, true, "同意");
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getBizType())
                        && "APPROVAL_APPROVE".equals(command.getOperationType())
                        && Long.valueOf(30L).equals(command.getRelatedBizId())
                        && "APPROVAL_INSTANCE".equals(command.getRelatedBizType())
                        && command.getActionSummary() != null
                        && command.getActionSummary().contains("通过了审批")
        ));
    }

    @Test
    void listMyTasksForSuperAdminDoesNotFilterByApprover() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));
        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 2L);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of(task));

        service.listMyTasks("PENDING");

        ArgumentCaptor<LambdaQueryWrapper<ApprovalTaskEntity>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(approvalTaskMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        org.junit.jupiter.api.Assertions.assertFalse(sqlSegment.contains("approver_user_id"));
        org.junit.jupiter.api.Assertions.assertTrue(sqlSegment.contains("deleted"));
        org.junit.jupiter.api.Assertions.assertTrue(sqlSegment.contains("status"));
    }

    @Test
    void listMyTasksForNormalUserFiltersByApprover() {
        UserContext.set(new UserContext.UserInfo(2L, "reviewer", List.of("EMPLOYEE"), 1L));
        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 2L);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of(task));

        service.listMyTasks(null);

        ArgumentCaptor<LambdaQueryWrapper<ApprovalTaskEntity>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(approvalTaskMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        org.junit.jupiter.api.Assertions.assertTrue(sqlSegment.contains("approver_user_id"));
        org.junit.jupiter.api.Assertions.assertTrue(sqlSegment.contains("deleted"));
    }

    @Test
    void listMyTasksIncludesBusinessTitlesNamesAndActionFlags() {
        UserContext.set(new UserContext.UserInfo(2L, "reviewer", List.of("EMPLOYEE"), 1L));
        ApprovalTaskEntity task = pendingTask(20L, 30L, null, 500L, 700L, "DEPT_REVIEW", 2L);
        ApprovalInstanceEntity instance = pendingInstance(30L, null, 500L);
        instance.setBizModule("PROJECT");
        instance.setBizType("CHECKLIST_ITEM");
        instance.setBizId(900L);

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(900L);
        item.setProjectId(800L);
        item.setItemName("技术方案");

        ProjectEntity project = new ProjectEntity();
        project.setId(800L);
        project.setProjectName("医院智能化改造项目");

        ApprovalDefinitionEntity definition = new ApprovalDefinitionEntity();
        definition.setId(500L);
        definition.setName("技术标审批流程");

        ApprovalNodeEntity node = new ApprovalNodeEntity();
        node.setId(700L);
        node.setNodeName("部门经理审批");

        com.example.biddoc.auth.entity.SysUser submitter = user(7L, "张经理", "zhang_manager");
        com.example.biddoc.auth.entity.SysUser approver = user(2L, "王工程师", "wang_engineer");

        when(approvalTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(projectChecklistItemMapper.selectById(900L)).thenReturn(item);
        when(projectMapper.selectById(800L)).thenReturn(project);
        when(approvalDefinitionMapper.selectById(500L)).thenReturn(definition);
        when(approvalNodeMapper.selectById(700L)).thenReturn(node);
        when(sysUserMapper.selectById(7L)).thenReturn(submitter);
        when(sysUserMapper.selectById(2L)).thenReturn(approver);

        List<ApprovalTaskRespDTO> tasks = service.listMyTasks("PENDING");

        ApprovalTaskRespDTO dto = tasks.get(0);
        assertEquals("医院智能化改造项目 / 技术方案", dto.getBusinessTitle());
        assertEquals("医院智能化改造项目", dto.getProjectName());
        assertEquals("技术方案", dto.getChecklistItemName());
        assertEquals("张经理", dto.getSubmitterName());
        assertEquals("王工程师", dto.getApproverName());
        assertNull(dto.getHandlerName());
        assertEquals("PENDING", dto.getInstanceStatus());
        assertEquals("待审批", dto.getInstanceStatusName());
        assertTrue(dto.getCanApprove());
        assertTrue(dto.getCanReject());
        assertTrue(dto.getCanTransfer());
        assertTrue(dto.getCanAddSign());
        assertFalse(dto.getCanWithdraw());
        assertFalse(dto.getCanTerminate());
    }

    @Test
    void approveWithNextNodeKeepsInstancePendingAndDoesNotWriteBusinessResult() {
        UserContext.set(new UserContext.UserInfo(1L, "manager", List.of("DEPT_MANAGER"), 1L));

        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 1L);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        ApprovalTaskEntity nextTask = pendingTask(21L, 30L, 100L, 500L, 701L, "FINAL_REVIEW", 2L);
        ApprovalNodeEntity nextNode = new ApprovalNodeEntity();
        nextNode.setId(701L);
        nextNode.setNodeName("终审");

        DocumentEntity document = new DocumentEntity();
        document.setId(100L);
        document.setFolderId(10L);
        FolderEntity folder = new FolderEntity();
        folder.setId(10L);

        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectCount(any())).thenReturn(0L);
        when(documentMapper.selectById(100L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        when(approvalFlowEngineService.createNextTask(any(), any(), any(), any(), any())).thenReturn(nextTask);
        when(approvalNodeMapper.selectById(701L)).thenReturn(nextNode);

        ApprovalHandleResultRespDTO result = service.approve(20L, "同意，进入下一节点");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("PENDING", instance.getStatus());
        assertEquals("APPROVED", result.getTaskStatus());
        assertEquals("PENDING", result.getInstanceStatus());
        assertFalse(result.getCompleted());
        assertEquals(21L, result.getNextTaskId());
        assertEquals("终审", result.getNextNodeName());
        verify(approvalFlowEngineService).createNextTask(any(), any(), any(), any(), any());
        verify(documentService, never()).markApprovalResult(any(), any(Boolean.class), any());
    }

    @Test
    void approveLastConfiguredNodeCompletesInstanceAndWritesBusinessResult() {
        UserContext.set(new UserContext.UserInfo(1L, "manager", List.of("DEPT_MANAGER"), 1L));

        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 1L);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        DocumentEntity document = new DocumentEntity();
        document.setId(100L);
        document.setFolderId(10L);
        FolderEntity folder = new FolderEntity();
        folder.setId(10L);

        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectCount(any())).thenReturn(0L);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of());
        when(documentMapper.selectById(100L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        when(approvalFlowEngineService.createNextTask(any(), any(), any(), any(), any())).thenReturn(null);

        service.approve(20L, "最终同意");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("APPROVED", instance.getStatus());
        verify(documentService).markApprovalResult(100L, true, "最终同意");
    }

    @Test
    void approveAddSignTaskWaitsForSiblingBeforeAdvancing() {
        UserContext.set(new UserContext.UserInfo(2L, "reviewer", List.of("EMPLOYEE"), 1L));

        ApprovalTaskEntity task = pendingTask(22L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 2L);
        task.setAddSign(true);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);

        when(approvalTaskMapper.selectById(22L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectCount(any())).thenReturn(1L);

        service.approve(22L, "加签同意");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("PENDING", instance.getStatus());
        verify(approvalFlowEngineService, never()).createNextTask(any(), any(), any(), any(), any());
        verify(documentService, never()).markApprovalResult(any(), any(Boolean.class), any());
    }

    @Test
    void rejectCompletesInstanceAndWritesRejectedBusinessResult() {
        UserContext.set(new UserContext.UserInfo(1L, "manager", List.of("DEPT_MANAGER"), 1L));

        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, null, null, null, 1L);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, null);
        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of());

        ApprovalHandleResultRespDTO result = service.reject(20L, "不同意");

        assertEquals("REJECTED", task.getStatus());
        assertEquals("REJECTED", instance.getStatus());
        assertEquals("REJECTED", result.getTaskStatus());
        assertEquals("REJECTED", result.getInstanceStatus());
        assertTrue(result.getCompleted());
        assertNull(result.getNextTaskId());
        assertNull(result.getNextNodeName());
        verify(documentService).markApprovalResult(100L, false, "不同意");
    }

    @Test
    void transferCreatesNewTaskAndCandidateSnapshot() {
        UserContext.set(new UserContext.UserInfo(1L, "manager", List.of("DEPT_MANAGER"), 1L));

        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 1L);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        com.example.biddoc.auth.entity.SysUser target = new com.example.biddoc.auth.entity.SysUser();
        target.setId(2L);
        target.setStatus(1);
        target.setDeleted(false);

        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(sysUserMapper.selectById(2L)).thenReturn(target);

        service.transfer(20L, 2L, "请你处理");

        assertEquals("TRANSFERRED", task.getStatus());
        verify(approvalTaskMapper).insert(argThat(newTask ->
                Long.valueOf(2L).equals(newTask.getApproverUserId())
                        && Long.valueOf(20L).equals(newTask.getTransferredFromTaskId())
                        && Boolean.FALSE.equals(newTask.getAddSign())));
        verify(approvalTaskCandidateMapper).insert(argThat(candidate ->
                "TRANSFER".equals(candidate.getCandidateType())
                        && Long.valueOf(2L).equals(candidate.getCandidateUserId())));
    }

    @Test
    void addSignCreatesAdditionalTaskWithoutClosingSourceTask() {
        UserContext.set(new UserContext.UserInfo(1L, "manager", List.of("DEPT_MANAGER"), 1L));

        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 1L);
        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        com.example.biddoc.auth.entity.SysUser target = new com.example.biddoc.auth.entity.SysUser();
        target.setId(3L);
        target.setStatus(1);
        target.setDeleted(false);

        when(approvalTaskMapper.selectById(20L)).thenReturn(task);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(sysUserMapper.selectById(3L)).thenReturn(target);

        service.addSign(20L, 3L, "请加签");

        assertEquals("PENDING", task.getStatus());
        verify(approvalTaskMapper).insert(argThat(newTask ->
                Long.valueOf(3L).equals(newTask.getApproverUserId())
                        && Long.valueOf(20L).equals(newTask.getTransferredFromTaskId())
                        && Boolean.TRUE.equals(newTask.getAddSign())));
        verify(approvalTaskCandidateMapper).insert(argThat(candidate ->
                "ADD_SIGN".equals(candidate.getCandidateType())
                        && Long.valueOf(3L).equals(candidate.getCandidateUserId())));
    }

    @Test
    void withdrawClosesPendingTasksAndRollsBusinessBack() {
        UserContext.set(new UserContext.UserInfo(7L, "submitter", List.of("EMPLOYEE"), 1L));

        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 1L);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of(task));

        service.withdraw(30L, "撤回重提");

        assertEquals("WITHDRAWN", instance.getStatus());
        assertEquals("WITHDRAWN", task.getStatus());
        verify(documentService).markApprovalWithdrawn(100L, "撤回重提");
    }

    @Test
    void superAdminTerminateClosesPendingTasksAndRollsBusinessBack() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));

        ApprovalInstanceEntity instance = pendingInstance(30L, 100L, 500L);
        ApprovalTaskEntity task = pendingTask(20L, 30L, 100L, 500L, 700L, "DEPT_REVIEW", 2L);
        when(approvalInstanceMapper.selectById(30L)).thenReturn(instance);
        when(approvalTaskMapper.selectList(any())).thenReturn(List.of(task));

        service.terminate(30L, "smoke terminate");

        assertEquals("TERMINATED", instance.getStatus());
        assertEquals("CANCELLED", task.getStatus());
        verify(documentService).markApprovalWithdrawn(100L, "smoke terminate");
    }

    private ApprovalTaskEntity pendingTask(Long taskId, Long instanceId, Long documentId, Long definitionId,
                                           Long nodeId, String nodeCode, Long approverUserId) {
        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setId(taskId);
        task.setInstanceId(instanceId);
        task.setDocumentId(documentId);
        task.setDefinitionId(definitionId);
        task.setNodeId(nodeId);
        task.setNodeCode(nodeCode);
        task.setApproverUserId(approverUserId);
        task.setStatus("PENDING");
        task.setDeleted(false);
        task.setAddSign(false);
        return task;
    }

    private ApprovalInstanceEntity pendingInstance(Long instanceId, Long documentId, Long definitionId) {
        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(instanceId);
        instance.setDocumentId(documentId);
        instance.setBizModule("DOCUMENT");
        instance.setBizType("DOCUMENT");
        instance.setBizId(documentId);
        instance.setScenario("DOCUMENT_APPROVAL");
        instance.setDefinitionId(definitionId);
        instance.setSubmitterUserId(7L);
        instance.setStatus("PENDING");
        instance.setDeleted(false);
        return instance;
    }

    private com.example.biddoc.auth.entity.SysUser user(Long id, String realName, String username) {
        com.example.biddoc.auth.entity.SysUser user = new com.example.biddoc.auth.entity.SysUser();
        user.setId(id);
        user.setRealName(realName);
        user.setUsername(username);
        user.setStatus(1);
        user.setDeleted(false);
        return user;
    }
}
