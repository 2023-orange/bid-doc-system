package com.example.biddoc.project.service.impl;

import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.mapper.DocumentUseGrantMapper;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.entity.ChecklistTemplateEntity;
import com.example.biddoc.project.entity.ChecklistTemplateItemEntity;
import com.example.biddoc.project.entity.ProjectChecklistDocumentEntity;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ChecklistTemplateItemMapper;
import com.example.biddoc.project.mapper.ChecklistTemplateMapper;
import com.example.biddoc.project.mapper.ProjectChecklistDocumentMapper;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.service.ProjectPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectChecklistServiceImplTest {

    private final ChecklistTemplateMapper templateMapper = mock(ChecklistTemplateMapper.class);
    private final ChecklistTemplateItemMapper templateItemMapper = mock(ChecklistTemplateItemMapper.class);
    private final ProjectChecklistItemMapper checklistItemMapper = mock(ProjectChecklistItemMapper.class);
    private final ProjectChecklistDocumentMapper checklistDocumentMapper = mock(ProjectChecklistDocumentMapper.class);
    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final DocumentUseGrantMapper documentUseGrantMapper = mock(DocumentUseGrantMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ProjectChecklistServiceImpl service = new ProjectChecklistServiceImpl(
            templateMapper,
            templateItemMapper,
            checklistItemMapper,
            checklistDocumentMapper,
            projectMapper,
            documentMapper,
            documentUseGrantMapper,
            sysUserMapper,
            projectPermissionService,
            auditService,
            notificationService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void generateCopiesTemplateItemsIntoProjectChecklist() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ChecklistTemplateEntity template = new ChecklistTemplateEntity();
        template.setId(10L);
        template.setEnabled(true);

        ChecklistTemplateItemEntity templateItem = new ChecklistTemplateItemEntity();
        templateItem.setId(20L);
        templateItem.setTemplateId(10L);
        templateItem.setItemName("营业执照");
        templateItem.setRequired(true);
        templateItem.setMinCount(1);
        templateItem.setSortOrder(1);

        when(templateMapper.selectById(10L)).thenReturn(template);
        when(templateItemMapper.selectList(any())).thenReturn(List.of(templateItem));
        doNothing().when(projectPermissionService).checkManage(100L);

        service.generateFromTemplate(100L, 10L);

        verify(checklistItemMapper).insert(argThat(item ->
                Long.valueOf(100L).equals(item.getProjectId())
                        && Long.valueOf(20L).equals(item.getTemplateItemId())
                        && "营业执照".equals(item.getItemName())
                        && "PENDING_COLLECT".equals(item.getStatus())
        ));
    }

    @Test
    void bindApprovedDocumentMarksItemComplete() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);
        item.setMinCount(1);
        item.setStatus("PENDING_COLLECT");

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        DocumentEntity document = new DocumentEntity();
        document.setId(200L);
        document.setDocumentStatus("APPROVED");
        document.setCurrentVersionNo(1);
        document.setDeleted(false);

        ProjectChecklistDocumentEntity binding = new ProjectChecklistDocumentEntity();
        binding.setChecklistItemId(30L);
        binding.setDocumentId(200L);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(documentMapper.selectById(200L)).thenReturn(document);
        when(checklistDocumentMapper.selectCount(any())).thenReturn(1L);
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        service.bindDocument(100L, 30L, 200L, 1);

        verify(checklistDocumentMapper).insert(any(ProjectChecklistDocumentEntity.class));
        verify(checklistItemMapper).updateById(argThat(updated ->
                Long.valueOf(30L).equals(updated.getId())
                        && "COMPLETE".equals(updated.getStatus())
        ));
    }

    @Test
    void bindDocumentRejectsWhenAllowedSourceDoesNotMatch() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);
        item.setAllowedSource("COMMON_LIBRARY");

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        DocumentEntity document = new DocumentEntity();
        document.setId(200L);
        document.setDocumentStatus("APPROVED");
        document.setSourceType("PROJECT_UPLOAD");
        document.setDeleted(false);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(documentMapper.selectById(200L)).thenReturn(document);
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindDocument(100L, 30L, 200L, 1));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(checklistDocumentMapper, never()).insert(any(ProjectChecklistDocumentEntity.class));
    }

    @Test
    void bindDocumentRejectsWhenFileTypeNotAllowed() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);
        item.setAllowedFileTypes("application/pdf");

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        DocumentEntity document = new DocumentEntity();
        document.setId(200L);
        document.setDocumentStatus("APPROVED");
        document.setLatestMime("image/png");
        document.setDeleted(false);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(documentMapper.selectById(200L)).thenReturn(document);
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindDocument(100L, 30L, 200L, 1));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(checklistDocumentMapper, never()).insert(any(ProjectChecklistDocumentEntity.class));
    }

    @Test
    void bindDocumentRejectsWhenMaxCountExceeded() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);
        item.setMaxCount(1);

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        DocumentEntity document = new DocumentEntity();
        document.setId(200L);
        document.setDocumentStatus("APPROVED");
        document.setDeleted(false);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(documentMapper.selectById(200L)).thenReturn(document);
        when(checklistDocumentMapper.selectCount(any())).thenReturn(1L);
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindDocument(100L, 30L, 200L, 1));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(checklistDocumentMapper, never()).insert(any(ProjectChecklistDocumentEntity.class));
    }

    @Test
    void bindSensitiveDocumentRejectsWithoutUseGrant() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        DocumentEntity document = new DocumentEntity();
        document.setId(200L);
        document.setDocumentStatus("APPROVED");
        document.setCurrentVersionNo(1);
        document.setOwnerUserId(2L);
        document.setSensitiveLevel("SENSITIVE");
        document.setDeleted(false);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(documentMapper.selectById(200L)).thenReturn(document);
        when(documentUseGrantMapper.selectCount(any())).thenReturn(0L);
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindDocument(100L, 30L, 200L, 1));

        assertEquals(ErrorCode.DOCUMENT_SENSITIVE_ACCESS_DENIED, ex.getErrorCode());
        verify(checklistDocumentMapper, never()).insert(any(ProjectChecklistDocumentEntity.class));
    }

    @Test
    void archivedProjectRejectsDocumentBinding() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("ARCHIVED");

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        doNothing().when(projectPermissionService).checkChecklistMaintain(100L, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bindDocument(100L, 30L, 200L, 1));

        assertEquals(ErrorCode.PROJECT_ARCHIVED_READONLY, ex.getErrorCode());
        verify(checklistDocumentMapper, never()).insert(any(ProjectChecklistDocumentEntity.class));
    }

    @Test
    void archivedProjectRejectsChecklistGeneration() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("ARCHIVED");
        ChecklistTemplateEntity template = new ChecklistTemplateEntity();
        template.setId(10L);
        template.setEnabled(true);

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(templateMapper.selectById(10L)).thenReturn(template);
        doNothing().when(projectPermissionService).checkManage(100L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generateFromTemplate(100L, 10L));

        assertEquals(ErrorCode.PROJECT_ARCHIVED_READONLY, ex.getErrorCode());
        verify(checklistItemMapper, never()).insert(any(ProjectChecklistItemEntity.class));
    }

    @Test
    void archivedProjectRejectsChecklistOwnerUpdate() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("ARCHIVED");

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        doNothing().when(projectPermissionService).checkManage(100L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateOwner(100L, 30L, 2L, null));

        assertEquals(ErrorCode.PROJECT_ARCHIVED_READONLY, ex.getErrorCode());
        verify(checklistItemMapper, never()).updateById(any(ProjectChecklistItemEntity.class));
    }

    @Test
    void updateOwnerRejectsDisabledUser() {
        UserContext.set(new UserContext.UserInfo(1L, "owner", List.of("EMPLOYEE"), 10L));

        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);

        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectStatus("NORMAL");

        SysUser disabled = new SysUser();
        disabled.setId(2L);
        disabled.setStatus(0);
        disabled.setDeleted(false);

        when(checklistItemMapper.selectById(30L)).thenReturn(item);
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(sysUserMapper.selectById(2L)).thenReturn(disabled);
        doNothing().when(projectPermissionService).checkManage(100L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateOwner(100L, 30L, 2L, null));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        verify(checklistItemMapper, never()).updateById(any(ProjectChecklistItemEntity.class));
    }
}
