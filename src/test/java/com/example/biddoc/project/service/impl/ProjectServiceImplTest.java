package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.audit.mapper.AuditOperationLogMapper;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.dto.req.ProjectMemberSaveReqDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveDetailRespDTO;
import com.example.biddoc.project.dto.resp.ProjectWorkbenchRespDTO;
import com.example.biddoc.project.entity.ProjectArchiveChecklistSnapshotEntity;
import com.example.biddoc.project.entity.ProjectArchiveDocumentSnapshotEntity;
import com.example.biddoc.project.entity.ProjectArchiveRecordEntity;
import com.example.biddoc.project.entity.ProjectChecklistDocumentEntity;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.entity.ProjectNoSequenceEntity;
import com.example.biddoc.project.mapper.ProjectArchiveChecklistSnapshotMapper;
import com.example.biddoc.project.mapper.ProjectArchiveDocumentSnapshotMapper;
import com.example.biddoc.project.mapper.ProjectArchiveRecordMapper;
import com.example.biddoc.project.mapper.ProjectChecklistDocumentMapper;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectNoSequenceMapper;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceImplTest {

    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final ProjectMemberMapper projectMemberMapper = mock(ProjectMemberMapper.class);
    private final ProjectNoSequenceMapper sequenceMapper = mock(ProjectNoSequenceMapper.class);
    private final ProjectChecklistItemMapper checklistItemMapper = mock(ProjectChecklistItemMapper.class);
    private final ProjectChecklistDocumentMapper checklistDocumentMapper = mock(ProjectChecklistDocumentMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final DocumentVersionMapper documentVersionMapper = mock(DocumentVersionMapper.class);
    private final ApprovalInstanceMapper approvalInstanceMapper = mock(ApprovalInstanceMapper.class);
    private final ProjectArchiveRecordMapper archiveRecordMapper = mock(ProjectArchiveRecordMapper.class);
    private final ProjectArchiveChecklistSnapshotMapper archiveChecklistSnapshotMapper = mock(ProjectArchiveChecklistSnapshotMapper.class);
    private final ProjectArchiveDocumentSnapshotMapper archiveDocumentSnapshotMapper = mock(ProjectArchiveDocumentSnapshotMapper.class);
    private final SysDepartmentMapper departmentMapper = mock(SysDepartmentMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final AuditOperationLogMapper auditOperationLogMapper = mock(AuditOperationLogMapper.class);
    private final AuditService auditService = mock(AuditService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ProjectServiceImpl service = new ProjectServiceImpl(
            projectMapper,
            projectMemberMapper,
            sequenceMapper,
            checklistItemMapper,
            checklistDocumentMapper,
            documentMapper,
            documentVersionMapper,
            approvalInstanceMapper,
            archiveRecordMapper,
            archiveChecklistSnapshotMapper,
            archiveDocumentSnapshotMapper,
            departmentMapper,
            sysUserMapper,
            auditOperationLogMapper,
            auditService,
            notificationService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createGeneratesProjectNoAndNotifiesMembers() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        SysDepartment dept = new SysDepartment();
        dept.setId(10L);
        dept.setExtensionData(Map.of("abbr", "GB"));

        when(departmentMapper.selectById(10L)).thenReturn(dept);
        when(sequenceMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L, 1, false));
        when(sysUserMapper.selectById(3L)).thenReturn(user(3L, 1, false));

        ProjectCreateReqDTO req = new ProjectCreateReqDTO();
        req.setProjectName("国网投标项目");
        req.setTenderUnit("国网");
        req.setOwnerDeptId(10L);
        req.setProjectType("POWER");
        req.setBidDeadline(OffsetDateTime.parse("2026-07-01T10:00:00+08:00"));
        req.setRemark("重点项目，需提前锁定商务资料");
        req.setOwnerUserIds(List.of(2L));
        req.setMemberUserIds(List.of(3L));

        Long id = service.create(req);

        verify(projectMapper).insert(argThat(project ->
                project.getId().equals(id)
                        && project.getProjectNo().startsWith("GB-")
                        && project.getProjectNo().endsWith("-001")
                        && "NORMAL".equals(project.getProjectStatus())
                        && "重点项目，需提前锁定商务资料".equals(project.getRemark())
        ));
        verify(sequenceMapper).insert(any(ProjectNoSequenceEntity.class));
        verify(projectMemberMapper).insert(argThat(member ->
                Long.valueOf(2L).equals(member.getUserId())
                        && "OWNER".equals(member.getMemberRole())
        ));
        verify(notificationService).send(2L, "PROJECT_MEMBER", "你已加入投标项目", "项目：国网投标项目", "PROJECT", id);
    }

    @Test
    void getReturnsProjectRemark() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        project.setRemark("客户要求商务标和技术标同步推进");
        when(projectMapper.selectById(100L)).thenReturn(project);

        var detail = service.get(100L);

        assertEquals("客户要求商务标和技术标同步推进", detail.getRemark());
    }

    @Test
    void createRejectsProjectWithoutOwner() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));

        ProjectCreateReqDTO req = new ProjectCreateReqDTO();
        req.setProjectName("无负责人项目");
        req.setOwnerDeptId(10L);
        req.setOwnerUserIds(List.of());

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void createRejectsDisabledOwnerUser() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        SysDepartment dept = new SysDepartment();
        dept.setId(10L);
        dept.setExtensionData(Map.of("abbr", "GB"));
        when(departmentMapper.selectById(10L)).thenReturn(dept);
        when(sequenceMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L, 0, false));

        ProjectCreateReqDTO req = new ProjectCreateReqDTO();
        req.setProjectName("国网投标项目");
        req.setTenderUnit("国网");
        req.setOwnerDeptId(10L);
        req.setProjectType("POWER");
        req.setOwnerUserIds(List.of(2L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        verify(projectMapper, never()).insert(any(ProjectEntity.class));
    }

    @Test
    void listFiltersNonAdminToJoinedProjects() {
        UserContext.set(new UserContext.UserInfo(3L, "employee", List.of("EMPLOYEE"), 10L));

        Page<ProjectEntity> page = new Page<>(1, 20);
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectName("参与项目");
        project.setProjectNo("GB-20260612-001");
        page.setRecords(List.of(project));
        page.setTotal(1);
        when(projectMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.list(null, null, null, null, null, null, null, null, null, 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals("参与项目", result.getList().get(0).getProjectName());
    }

    @Test
    void listEnrichesFrontendSummaryFields() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));

        ProjectEntity project = normalProject();
        project.setOwnerDeptId(10L);
        Page<ProjectEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(project));
        page.setTotal(1);
        when(projectMapper.selectPage(any(Page.class), any())).thenReturn(page);

        SysDepartment department = new SysDepartment();
        department.setId(10L);
        department.setName("投标管理部");
        when(departmentMapper.selectBatchIds(any())).thenReturn(List.of(department));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(
                projectMember(2L, "OWNER"),
                projectMember(3L, "MEMBER")
        ));
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(
                namedUser(2L, "张晨"),
                namedUser(3L, "王一鸣")
        ));
        ProjectChecklistItemEntity completeItem = checklistItem(30L, "营业执照", "COMPLETE", 2L,
                OffsetDateTime.parse("2026-06-25T18:00:00+08:00"));
        ProjectChecklistItemEntity pendingItem = checklistItem(31L, "技术方案", "PENDING_COLLECT", 3L,
                OffsetDateTime.parse("2026-06-28T18:00:00+08:00"));
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(completeItem, pendingItem));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding(30L, 200L, 1)));

        var result = service.list(null, null, null, null, null, null, null, null, null, 1, 20);

        var item = result.getList().get(0);
        assertEquals("张晨", item.getOwnerUserName());
        assertEquals("投标管理部", item.getOwnerDeptName());
        assertEquals(2L, item.getMemberCount());
        assertEquals(1L, item.getDocumentCount());
        assertEquals(2L, item.getChecklistProgress().getTotal());
        assertEquals(1L, item.getChecklistProgress().getCompleted());
        assertEquals(50, item.getChecklistProgress().getPercentage());
    }

    @Test
    void archiveRejectsPendingApprovalDocument() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity item = completeRequiredItem();
        ProjectChecklistDocumentEntity binding = binding(30L, 200L, 1);
        DocumentEntity document = approvedDocument(200L);
        DocumentVersionEntity version = approvedVersion(200L, 1);

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(item));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        when(documentMapper.selectById(200L)).thenReturn(document);
        when(documentVersionMapper.selectOne(any())).thenReturn(version);
        when(approvalInstanceMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "ARCHIVED"));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(archiveRecordMapper, never()).insert(any(ProjectArchiveRecordEntity.class));
    }

    @Test
    void archiveRejectsIncompleteRequiredChecklist() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "ARCHIVED"));

        assertEquals(ErrorCode.PROJECT_ARCHIVE_CHECKLIST_INCOMPLETE, ex.getErrorCode());
        verify(archiveRecordMapper, never()).insert(any(ProjectArchiveRecordEntity.class));
    }

    @Test
    void archiveRejectsInvalidBoundDocument() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity item = completeRequiredItem();
        ProjectChecklistDocumentEntity binding = binding(30L, 200L, 1);
        DocumentEntity document = approvedDocument(200L);
        document.setDocumentStatus("VOIDED");

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(item));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        when(documentMapper.selectById(200L)).thenReturn(document);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "ARCHIVED"));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(archiveRecordMapper, never()).insert(any(ProjectArchiveRecordEntity.class));
    }

    @Test
    void archiveRejectsDeletedBoundDocument() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity item = completeRequiredItem();
        ProjectChecklistDocumentEntity binding = binding(30L, 200L, 1);
        DocumentEntity document = approvedDocument(200L);
        document.setDeleted(true);

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(item));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        when(documentMapper.selectById(200L)).thenReturn(document);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "ARCHIVED"));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, ex.getErrorCode());
        verify(archiveRecordMapper, never()).insert(any(ProjectArchiveRecordEntity.class));
    }

    @Test
    void archiveRejectsExpiredBoundDocument() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity item = completeRequiredItem();
        ProjectChecklistDocumentEntity binding = binding(30L, 200L, 1);
        DocumentEntity document = approvedDocument(200L);
        document.setHasExpireDate(true);
        document.setExpireDate(OffsetDateTime.now().minusDays(1));

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(item));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        when(documentMapper.selectById(200L)).thenReturn(document);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "ARCHIVED"));

        assertEquals(ErrorCode.DOCUMENT_EXPIRED, ex.getErrorCode());
        verify(archiveRecordMapper, never()).insert(any(ProjectArchiveRecordEntity.class));
    }

    @Test
    void archiveCreatesRecordAndSnapshotsWithBoundVersion() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity item = completeRequiredItem();
        ProjectChecklistDocumentEntity binding = binding(30L, 200L, 2);
        DocumentEntity document = approvedDocument(200L);
        document.setCurrentVersionNo(3);
        DocumentVersionEntity version = approvedVersion(200L, 2);
        version.setSize(4096L);
        version.setMimeType("application/pdf");

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(checklistItemMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(item));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(binding));
        when(documentMapper.selectById(200L)).thenReturn(document);
        when(documentVersionMapper.selectOne(any())).thenReturn(version);
        when(approvalInstanceMapper.selectCount(any())).thenReturn(0L);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(projectMember(2L)));

        service.changeStatus(100L, "ARCHIVED");

        ArgumentCaptor<ProjectArchiveRecordEntity> recordCaptor = ArgumentCaptor.forClass(ProjectArchiveRecordEntity.class);
        verify(archiveRecordMapper).insert(recordCaptor.capture());
        assertEquals(100L, recordCaptor.getValue().getProjectId());
        assertEquals("ARCHIVED", recordCaptor.getValue().getArchiveStatus());
        assertEquals(1, recordCaptor.getValue().getChecklistTotal());
        assertEquals(1, recordCaptor.getValue().getChecklistComplete());
        assertEquals(1, recordCaptor.getValue().getDocumentTotal());
        assertNotNull(recordCaptor.getValue().getSnapshotHash());

        verify(archiveChecklistSnapshotMapper).insert(argThat(snapshot ->
                Long.valueOf(100L).equals(snapshot.getProjectId())
                        && Long.valueOf(30L).equals(snapshot.getChecklistItemId())
                        && "ARCHIVED".equals(snapshot.getItemStatus())
                        && Integer.valueOf(1).equals(snapshot.getBoundDocumentCount())
                        && snapshot.getSnapshotJson().contains("\"originalStatus\":\"COMPLETE\"")));
        verify(archiveDocumentSnapshotMapper).insert(argThat(snapshot ->
                Long.valueOf(200L).equals(snapshot.getDocumentId())
                        && Integer.valueOf(2).equals(snapshot.getVersionNo())
                        && "APPROVED".equals(snapshot.getDocumentStatus())
                        && "APPROVED".equals(snapshot.getVersionStatus())
                        && Long.valueOf(4096L).equals(snapshot.getFileSize())
                        && snapshot.getSnapshotJson().contains("\"currentVersionNo\":3")
                        && !snapshot.getSnapshotJson().contains("storageKey")));
        verify(checklistItemMapper).update(any(), any());
        verify(projectMapper).updateById(argThat(updated ->
                "ARCHIVED".equals(updated.getProjectStatus()) && "ARCHIVED".equals(updated.getProjectStage())));
        verify(notificationService).send(2L, "PROJECT_ARCHIVED", "投标项目已归档", "项目：国网投标项目", "PROJECT", 100L);
    }

    @Test
    void archivedProjectRejectsMemberMutation() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        project.setProjectStatus("ARCHIVED");
        when(projectMapper.selectById(100L)).thenReturn(project);

        ProjectMemberSaveReqDTO req = new ProjectMemberSaveReqDTO();
        req.setUserIds(List.of(2L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addMembers(100L, req));

        assertEquals(ErrorCode.PROJECT_ARCHIVED_READONLY, ex.getErrorCode());
        verify(projectMemberMapper, never()).insert(any(ProjectMemberEntity.class));
    }

    @Test
    void changeStageRejectsUnknownStage() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        when(projectMapper.selectById(100L)).thenReturn(project);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStage(100L, "FREE_TEXT"));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(projectMapper, never()).updateById(any(ProjectEntity.class));
    }

    @Test
    void changeStatusRejectsUnknownStatus() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        when(projectMapper.selectById(100L)).thenReturn(project);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(100L, "FREE_TEXT"));

        assertEquals(ErrorCode.BUSINESS_ILLEGAL, ex.getErrorCode());
        verify(projectMapper, never()).updateById(any(ProjectEntity.class));
    }

    @Test
    void getArchiveDetailRequiresViewPermissionAndReturnsSnapshots() {
        UserContext.set(new UserContext.UserInfo(2L, "member", List.of("EMPLOYEE"), 10L));
        ProjectEntity project = normalProject();
        ProjectArchiveRecordEntity record = new ProjectArchiveRecordEntity();
        record.setId(500L);
        record.setProjectId(100L);
        record.setArchiveNo("ARCH-GB-20260619-001");
        record.setArchiveStatus("ARCHIVED");
        record.setArchivedAt(OffsetDateTime.parse("2026-06-19T10:00:00+08:00"));
        record.setArchivedBy(1L);
        record.setChecklistTotal(1);
        record.setChecklistComplete(1);
        record.setDocumentTotal(1);

        ProjectArchiveChecklistSnapshotEntity checklistSnapshot = new ProjectArchiveChecklistSnapshotEntity();
        checklistSnapshot.setId(501L);
        checklistSnapshot.setArchiveRecordId(500L);
        checklistSnapshot.setProjectId(100L);
        checklistSnapshot.setChecklistItemId(30L);
        checklistSnapshot.setItemName("营业执照");
        checklistSnapshot.setItemStatus("ARCHIVED");
        checklistSnapshot.setBoundDocumentCount(1);

        ProjectArchiveDocumentSnapshotEntity documentSnapshot = new ProjectArchiveDocumentSnapshotEntity();
        documentSnapshot.setId(502L);
        documentSnapshot.setArchiveRecordId(500L);
        documentSnapshot.setProjectId(100L);
        documentSnapshot.setChecklistItemId(30L);
        documentSnapshot.setDocumentId(200L);
        documentSnapshot.setVersionNo(2);
        documentSnapshot.setDocumentName("营业执照.pdf");

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
        when(archiveRecordMapper.selectOne(any())).thenReturn(record);
        when(archiveChecklistSnapshotMapper.selectList(any())).thenReturn(List.of(checklistSnapshot));
        when(archiveDocumentSnapshotMapper.selectList(any())).thenReturn(List.of(documentSnapshot));

        ProjectArchiveDetailRespDTO detail = service.getArchiveDetail(100L);

        assertEquals(500L, detail.getRecord().getId());
        assertEquals("ARCH-GB-20260619-001", detail.getRecord().getArchiveNo());
        assertEquals(1, detail.getChecklistSnapshots().size());
        assertEquals("营业执照", detail.getChecklistSnapshots().get(0).getItemName());
        assertEquals(1, detail.getDocumentSnapshots().size());
        assertEquals(2, detail.getDocumentSnapshots().get(0).getVersionNo());
    }

    @Test
    void getArchiveDetailRejectsUserWithoutProjectPermission() {
        UserContext.set(new UserContext.UserInfo(9L, "outsider", List.of("EMPLOYEE"), 10L));
        ProjectEntity project = normalProject();

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getArchiveDetail(100L));

        assertEquals(ErrorCode.PROJECT_PERMISSION_DENIED, ex.getErrorCode());
        verify(archiveRecordMapper, never()).selectOne(any());
    }

    @Test
    void getWorkbenchAggregatesProjectContainerSections() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        ProjectEntity project = normalProject();
        project.setOwnerDeptId(10L);
        project.setFolderId(300L);
        project.setBidDeadline(OffsetDateTime.parse("2026-07-01T10:00:00+08:00"));
        project.setRemark("工作台演示项目");
        project.setCreatedAt(OffsetDateTime.parse("2026-06-01T09:00:00+08:00"));
        project.setUpdatedAt(OffsetDateTime.parse("2026-06-18T18:00:00+08:00"));

        SysDepartment dept = new SysDepartment();
        dept.setId(10L);
        dept.setName("投标管理部");
        dept.setExtensionData(Map.of("abbr", "TB"));

        ProjectChecklistItemEntity completeItem = checklistItem(30L, "营业执照", "COMPLETE", 2L,
                OffsetDateTime.parse("2026-06-25T18:00:00+08:00"));
        ProjectChecklistItemEntity supplementItem = checklistItem(31L, "技术方案", "NEED_SUPPLEMENT", 3L,
                OffsetDateTime.now().minusDays(1));
        ProjectChecklistItemEntity pendingItem = checklistItem(32L, "授权委托书", "PENDING_COLLECT", 4L,
                OffsetDateTime.now().plusDays(10));
        List<ProjectChecklistItemEntity> items = List.of(completeItem, supplementItem, pendingItem);

        ProjectChecklistDocumentEntity firstBinding = binding(30L, 200L, 1);
        ProjectChecklistDocumentEntity secondBinding = binding(31L, 201L, 1);
        DocumentEntity firstDocument = approvedDocument(200L);
        DocumentEntity secondDocument = approvedDocument(201L);
        secondDocument.setName("技术方案.docx");
        secondDocument.setDocumentNo("DOC-TECH-001");

        ApprovalInstanceEntity approval = new ApprovalInstanceEntity();
        approval.setId(700L);
        approval.setBizType("CHECKLIST_ITEM");
        approval.setBizId(31L);
        approval.setStatus("PENDING");
        approval.setSubmitterUserId(3L);
        approval.setSubmittedAt(OffsetDateTime.parse("2026-06-19T10:00:00+08:00"));
        approval.setDeleted(false);

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(departmentMapper.selectById(10L)).thenReturn(dept);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(
                projectMember(2L, "OWNER"),
                projectMember(3L, "MATERIAL_OWNER"),
                projectMember(4L, "MEMBER")
        ));
        when(checklistItemMapper.selectList(any())).thenReturn(items);
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of(firstBinding, secondBinding));
        when(documentMapper.selectBatchIds(any())).thenReturn(List.of(firstDocument, secondDocument));
        when(approvalInstanceMapper.selectList(any())).thenReturn(List.of(approval));
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(
                namedUser(2L, "张晨"),
                namedUser(3L, "李若溪"),
                namedUser(4L, "王一鸣")
        ));
        when(archiveRecordMapper.selectList(any())).thenReturn(List.of());

        ProjectWorkbenchRespDTO workbench = service.getWorkbench(100L);

        assertEquals("国网投标项目", workbench.getSummary().getProjectName());
        assertEquals("工作台演示项目", workbench.getBasicInfo().getRemark());
        assertEquals("投标管理部", workbench.getOrganization().getOwnerDeptName());
        assertEquals(33, workbench.getSummary().getChecklistCompletionRate());
        assertEquals(3, workbench.getSummary().getRiskCount());
        assertEquals(3, workbench.getMemberResponsibilities().size());
        assertEquals(3, workbench.getChecklist().size());
        assertEquals(2, workbench.getProjectFiles().size());
        assertEquals(1, workbench.getApprovalSummaries().size());
        assertTrue(workbench.getTabs().stream().anyMatch(tab -> "PROJECT_BASIC_INFO".equals(tab.getCode())));
        assertTrue(workbench.getTabs().stream().anyMatch(tab -> "RELATED_APPROVALS".equals(tab.getCode())));
    }

    @Test
    void getWorkbenchAllowsChecklistOwnerWithoutProjectMembership() {
        UserContext.set(new UserContext.UserInfo(6L, "checklist_owner", List.of("EMPLOYEE"), 10L));
        ProjectEntity project = normalProject();
        ProjectChecklistItemEntity ownedItem = checklistItem(30L, "财务证明", "PENDING_COLLECT", 6L,
                OffsetDateTime.parse("2026-06-28T18:00:00+08:00"));

        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);
        when(checklistItemMapper.selectCount(any())).thenReturn(1L);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of());
        when(checklistItemMapper.selectList(any())).thenReturn(List.of(ownedItem));
        when(checklistDocumentMapper.selectList(any())).thenReturn(List.of());
        when(documentMapper.selectBatchIds(any())).thenReturn(List.of());
        when(approvalInstanceMapper.selectList(any())).thenReturn(List.of());
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(namedUser(6L, "赵明远")));
        when(archiveRecordMapper.selectList(any())).thenReturn(List.of());

        ProjectWorkbenchRespDTO workbench = service.getWorkbench(100L);

        assertEquals("国网投标项目", workbench.getSummary().getProjectName());
        assertEquals(1, workbench.getMemberResponsibilities().size());
        assertTrue(workbench.getMemberResponsibilities().get(0).getChecklistOwner());
    }

    private ProjectEntity normalProject() {
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectNo("GB-" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-001");
        project.setProjectName("国网投标项目");
        project.setProjectStatus("NORMAL");
        project.setProjectStage("COLLECTING");
        project.setDeleted(false);
        return project;
    }

    private ProjectChecklistItemEntity checklistItem(Long id, String itemName, String status, Long ownerUserId,
                                                     OffsetDateTime deadline) {
        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(id);
        item.setProjectId(100L);
        item.setItemName(itemName);
        item.setRequired(true);
        item.setStatus(status);
        item.setOwnerUserId(ownerUserId);
        item.setDeadline(deadline);
        item.setMinCount(1);
        item.setMaxCount(2);
        item.setDeleted(false);
        return item;
    }

    private ProjectChecklistItemEntity completeRequiredItem() {
        ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
        item.setId(30L);
        item.setProjectId(100L);
        item.setTemplateItemId(20L);
        item.setItemName("营业执照");
        item.setRequired(true);
        item.setStatus("COMPLETE");
        item.setBusinessCategory("QUALIFICATION");
        item.setTenderStructureCategory("QUALIFICATION_REVIEW");
        item.setOwnerUserId(2L);
        item.setDeleted(false);
        return item;
    }

    private ProjectChecklistDocumentEntity binding(Long itemId, Long documentId, Integer versionNo) {
        ProjectChecklistDocumentEntity binding = new ProjectChecklistDocumentEntity();
        binding.setId(40L);
        binding.setChecklistItemId(itemId);
        binding.setDocumentId(documentId);
        binding.setVersionNo(versionNo);
        binding.setBindType("DOCUMENT");
        binding.setDeleted(false);
        return binding;
    }

    private DocumentEntity approvedDocument(Long documentId) {
        DocumentEntity document = new DocumentEntity();
        document.setId(documentId);
        document.setName("营业执照.pdf");
        document.setDocumentNo("DOC-2026-001");
        document.setDocumentStatus("APPROVED");
        document.setCurrentVersionNo(1);
        document.setHasExpireDate(false);
        document.setDeleted(false);
        return document;
    }

    private DocumentVersionEntity approvedVersion(Long documentId, Integer versionNo) {
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setId(300L);
        version.setDocumentId(documentId);
        version.setVersionNo(versionNo);
        version.setSize(1024L);
        version.setMimeType("application/pdf");
        version.setOriginalFilename("营业执照.pdf");
        version.setApprovalStatus("APPROVED");
        version.setDeleted(false);
        return version;
    }

    private ProjectMemberEntity projectMember(Long userId) {
        return projectMember(userId, "MEMBER");
    }

    private ProjectMemberEntity projectMember(Long userId, String memberRole) {
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(100L);
        member.setUserId(userId);
        member.setMemberRole(memberRole);
        member.setDeleted(false);
        return member;
    }

    private SysUser namedUser(Long id, String realName) {
        SysUser user = user(id, 1, false);
        user.setUsername("user" + id);
        user.setRealName(realName);
        return user;
    }

    private SysUser user(Long id, Integer status, Boolean deleted) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }
}
