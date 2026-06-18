package com.example.biddoc.workflow.service.impl;

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
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.service.ProjectPermissionService;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalActionLogMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskCandidateMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import com.example.biddoc.workflow.service.ApprovalFlowEngineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceImplTest {

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

        service.approve(20L, "同意");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("APPROVED", instance.getStatus());
        verify(approvalTaskMapper).updateById(task);
        verify(approvalInstanceMapper).updateById(instance);
        verify(documentService).markApprovalResult(100L, true, "同意");
    }
}
