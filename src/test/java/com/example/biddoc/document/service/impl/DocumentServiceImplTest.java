package com.example.biddoc.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.dto.req.DocumentSearchReqDTO;
import com.example.biddoc.document.dto.req.DocumentMetadataUpdateReqDTO;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentUseGrantEntity;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import com.example.biddoc.document.entity.DownloadLogEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentTagMapper;
import com.example.biddoc.document.mapper.DocumentUseGrantMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.document.mapper.DownloadLogMapper;
import com.example.biddoc.document.mapper.SearchHistoryMapper;
import com.example.biddoc.document.storage.StorageAdapter;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.notify.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceImplTest {

    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final DocumentVersionMapper documentVersionMapper = mock(DocumentVersionMapper.class);
    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);
    private final SearchHistoryMapper searchHistoryMapper = mock(SearchHistoryMapper.class);
    private final DownloadLogMapper downloadLogMapper = mock(DownloadLogMapper.class);
    private final DocumentTagMapper documentTagMapper = mock(DocumentTagMapper.class);
    private final DocumentUseGrantMapper documentUseGrantMapper = mock(DocumentUseGrantMapper.class);
    private final FolderFavoriteMapper folderFavoriteMapper = mock(FolderFavoriteMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final StorageAdapter storageAdapter = mock(StorageAdapter.class);
    private final AuditService auditService = mock(AuditService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final DocumentServiceImpl service = new DocumentServiceImpl(
            documentMapper,
            documentVersionMapper,
            folderMapper,
            folderPermissionService,
            searchHistoryMapper,
            downloadLogMapper,
            documentTagMapper,
            documentUseGrantMapper,
            folderFavoriteMapper,
            sysUserMapper,
            storageAdapter,
            auditService,
            notificationService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void uploadDocumentCreatesPendingVersionAndIncompleteMetadata() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity folder = folder(10L, "10");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "方案.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );

        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkCreateChild(folder);
        when(documentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(storageAdapter.put(any(), org.mockito.ArgumentMatchers.eq(file.getSize()), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("2026/06/20/upload.pdf");
        when(storageAdapter.get("2026/06/20/upload.pdf"))
                .thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes()))
                .thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes()));

        var result = service.uploadDocument(10L, file, "技术方案", "首版", "初稿");

        assertEquals(1, result.getVersionNo());
        verify(documentVersionMapper).insert(argThat(version ->
                version instanceof DocumentVersionEntity
                        && Integer.valueOf(1).equals(((DocumentVersionEntity) version).getVersionNo())
                        && "PENDING".equals(((DocumentVersionEntity) version).getApprovalStatus())
                        && ((DocumentVersionEntity) version).getApprovedAt() == null
        ));
        verify(documentMapper).insert(argThat(document ->
                document instanceof DocumentEntity
                        && "INCOMPLETE".equals(((DocumentEntity) document).getDocumentStatus())
                        && Boolean.FALSE.equals(((DocumentEntity) document).getMetadataCompleted())
        ));
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getBizType())
                        && "UPLOAD".equals(command.getOperationType())
                        && "技术方案".equals(command.getObjectName())
                        && command.getActionSummary() != null
                        && command.getActionSummary().contains("上传了《技术方案》")
        ));
    }

    @Test
    void recursiveSearchUsesBoundedSubtreeAndFiltersEachChildFolderByViewPermission() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity parent = folder(10L, "10");
        FolderEntity allowedChild = folder(11L, "10,11");
        FolderEntity deniedChild = folder(12L, "10,12");

        when(folderMapper.selectById(10L)).thenReturn(parent);
        when(folderMapper.selectSubtree(10L)).thenReturn(List.of(parent, allowedChild, deniedChild));
        doNothing().when(folderPermissionService).checkView(parent);
        when(folderPermissionService.canView(parent)).thenReturn(true);
        when(folderPermissionService.canView(allowedChild)).thenReturn(true);
        when(folderPermissionService.canView(deniedChild)).thenReturn(false);

        Page<DocumentEntity> page = new Page<>(1, 20);
        page.setRecords(List.of());
        page.setTotal(0);
        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        DocumentSearchReqDTO req = new DocumentSearchReqDTO();
        req.setFolderId(10L);
        req.setRecursive(true);

        service.searchDocuments(req);

        verify(folderMapper).selectSubtree(10L);
        verify(folderPermissionService).canView(parent);
        verify(folderPermissionService).canView(allowedChild);
        verify(folderPermissionService).canView(deniedChild);
    }

    @Test
    void listDocumentsSupportsKeywordSortAliasesAndFrontendFieldAliases() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity folder = folder(10L, "10");
        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("投标文件模板.docx");
        document.setCurrentVersionNo(3);
        document.setLatestSize(1024000L);
        document.setLatestMime("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        document.setDocumentStatus("VALID");
        document.setOwnerUserId(8L);
        document.setCreatedAt(OffsetDateTime.now().minusDays(1));
        document.setUpdatedAt(OffsetDateTime.now());

        SysUser uploader = new SysUser();
        uploader.setId(8L);
        uploader.setUsername("zhangsan");
        uploader.setRealName("张三");

        Page<DocumentEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(document));
        page.setTotal(1);

        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sysUserMapper.selectBatchIds(List.of(8L))).thenReturn(List.of(uploader));

        var result = service.listDocuments(10L, 1, 20, "name", "asc", "投标");

        assertEquals(1, result.getTotal());
        assertEquals("投标文件模板.docx", result.getList().get(0).getName());
        assertEquals(1024000L, result.getList().get(0).getSize());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                result.getList().get(0).getMimeType());
        assertEquals("VALID", result.getList().get(0).getStatus());
        assertEquals("8", result.getList().get(0).getUploadedBy());
        assertEquals("张三", result.getList().get(0).getUploadedByName());
    }

    @Test
    void downloadDocumentWritesDownloadLogAfterStorageReadSucceeds() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("合同");
        document.setCurrentVersionNo(2);

        FolderEntity folder = folder(10L, "10");

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(2);
        version.setStorageKey("2026/06/10/1000.pdf");
        version.setOriginalFilename("合同.pdf");
        version.setMimeType("application/pdf");
        version.setSize(12L);

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(storageAdapter.get("2026/06/10/1000.pdf"))
                .thenReturn(new ByteArrayInputStream("file-content".getBytes()));

        assertNotNull(service.downloadDocument(1000L, "127.0.0.1", "JUnit"));

        verify(downloadLogMapper).insert(argThat(log ->
                log instanceof DownloadLogEntity
                        && Long.valueOf(1000L).equals(((DownloadLogEntity) log).getDocumentId())
                        && Integer.valueOf(2).equals(((DownloadLogEntity) log).getVersionNo())
                        && Long.valueOf(7L).equals(((DownloadLogEntity) log).getUserId())
                        && "127.0.0.1".equals(((DownloadLogEntity) log).getIpAddress())
                        && "JUnit".equals(((DownloadLogEntity) log).getUserAgent())
                        && ((DownloadLogEntity) log).getDownloadTime() != null
                        && Boolean.FALSE.equals(((DownloadLogEntity) log).getDeleted())
        ));
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getBizType())
                        && "DOWNLOAD".equals(command.getOperationType())
                        && Long.valueOf(1000L).equals(command.getBizId())
                        && "合同".equals(command.getObjectName())
                        && "127.0.0.1".equals(command.getClientIp())
                        && "JUnit".equals(command.getUserAgent())
                        && command.getActionSummary() != null
                        && command.getActionSummary().contains("下载了《合同》 v2")
        ));
    }

    @Test
    void downloadSensitiveDocumentAllowsActiveUseGrant() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("合同");
        document.setCurrentVersionNo(2);
        document.setOwnerUserId(8L);
        document.setSensitiveLevel("SENSITIVE");

        FolderEntity folder = folder(10L, "10");

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(2);
        version.setStorageKey("2026/06/10/sensitive.pdf");
        version.setOriginalFilename("敏感资料.pdf");
        version.setMimeType("application/pdf");
        version.setSize(12L);

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderPermissionService.isManagerOfFolder(folder, 7L)).thenReturn(false);
        when(documentUseGrantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(storageAdapter.get("2026/06/10/sensitive.pdf"))
                .thenReturn(new ByteArrayInputStream("file-content".getBytes()));

        assertNotNull(service.downloadDocument(1000L, "127.0.0.1", "JUnit"));

        verify(downloadLogMapper).insert(argThat(log ->
                log instanceof DownloadLogEntity
                        && Long.valueOf(1000L).equals(((DownloadLogEntity) log).getDocumentId())
                        && Integer.valueOf(2).equals(((DownloadLogEntity) log).getVersionNo())
                        && Long.valueOf(7L).equals(((DownloadLogEntity) log).getUserId())
        ));
    }

    @Test
    void downloadSensitiveDocumentRejectsWithoutPrivilegeOrUseGrant() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setCurrentVersionNo(2);
        document.setOwnerUserId(8L);
        document.setSensitiveLevel("SENSITIVE");

        FolderEntity folder = folder(10L, "10");
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(2);
        version.setStorageKey("2026/06/10/sensitive.pdf");
        version.setOriginalFilename("敏感资料.pdf");
        version.setMimeType("application/pdf");
        version.setSize(12L);

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderPermissionService.isManagerOfFolder(folder, 7L)).thenReturn(false);
        when(documentUseGrantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(storageAdapter.get("2026/06/10/sensitive.pdf"))
                .thenReturn(new ByteArrayInputStream("file-content".getBytes()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.downloadDocument(1000L, "127.0.0.1", "JUnit"));

        assertEquals(ErrorCode.DOCUMENT_SENSITIVE_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void previewDocumentAllowsBrowserPreviewableMimeTypes() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("预览");
        document.setCurrentVersionNo(1);

        FolderEntity folder = folder(10L, "10");

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(1);
        version.setStorageKey("2026/06/10/preview.pdf");
        version.setOriginalFilename("预览.pdf");
        version.setMimeType("application/pdf");
        version.setSize(12L);

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(storageAdapter.get("2026/06/10/preview.pdf"))
                .thenReturn(new ByteArrayInputStream("file-content".getBytes()));

        var result = service.previewDocument(1000L, "127.0.0.1", "JUnit");

        assertEquals("预览.pdf", result.getOriginalFilename());
        assertEquals("application/pdf", result.getMimeType());
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getModuleCode())
                        && "PREVIEW".equals(command.getOperationType())
                        && Long.valueOf(1000L).equals(command.getBizId())
                        && "预览".equals(command.getObjectName())
                        && "127.0.0.1".equals(command.getClientIp())
                        && "JUnit".equals(command.getUserAgent())
                        && command.getActionSummary() != null
                        && command.getActionSummary().contains("预览了《预览》 v1")
        ));
    }

    @Test
    void previewDocumentRejectsUnsupportedMimeTypes() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setCurrentVersionNo(1);

        FolderEntity folder = folder(10L, "10");

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(1);
        version.setStorageKey("2026/06/10/word.docx");
        version.setOriginalFilename("方案.docx");
        version.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        version.setSize(12L);

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.previewDocument(1000L, "127.0.0.1", "JUnit"));

        assertEquals(ErrorCode.DOCUMENT_PREVIEW_UNSUPPORTED, ex.getErrorCode());
    }

    @Test
    void updateMetadataMarksDocumentReadyToSubmit() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setDocumentStatus("INCOMPLETE");

        FolderEntity folder = folder(10L, "10");
        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);

        DocumentMetadataUpdateReqDTO req = new DocumentMetadataUpdateReqDTO();
        req.setDocumentName("营业执照");
        req.setBusinessCategory("LICENSE");
        req.setSensitiveLevel("INTERNAL");
        req.setOwnerDeptId(100L);
        req.setSourceType("DEPARTMENT");
        req.setHasExpireDate(false);

        service.updateMetadata(1000L, req);

        verify(documentMapper).updateById(argThat(updated ->
                Long.valueOf(1000L).equals(updated.getId())
                        && "营业执照".equals(updated.getName())
                        && "READY_SUBMIT".equals(updated.getDocumentStatus())
                        && Boolean.TRUE.equals(updated.getMetadataCompleted())
        ));
    }

    @Test
    void submitApprovalMarksDocumentApproving() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setDocumentStatus("READY_SUBMIT");

        FolderEntity folder = folder(10L, "10");
        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);

        service.markApproving(1000L);

        verify(documentMapper).updateById(argThat(updated ->
                Long.valueOf(1000L).equals(updated.getId())
                        && "APPROVING".equals(updated.getDocumentStatus())
        ));
    }

    @Test
    void getDocumentDetailReturnsLifecycleFieldsForFrontendDisplay() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        OffsetDateTime expireDate = OffsetDateTime.parse("2026-07-01T10:00:00+08:00");
        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("技术方案");
        document.setCurrentVersionNo(2);
        document.setOwnerUserId(7L);
        document.setOwnerDeptId(100L);
        document.setDocumentNo("DOC-1000");
        document.setDocumentStatus("APPROVED");
        document.setBusinessCategory("TECHNICAL");
        document.setSensitiveLevel("INTERNAL");
        document.setHasExpireDate(true);
        document.setExpireDate(expireDate);
        document.setMetadataCompleted(true);

        FolderEntity folder = folder(10L, "10");
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(1000L);
        version.setVersionNo(2);
        version.setApprovalStatus("APPROVED");

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(documentVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);

        var detail = service.getDocumentDetail(1000L);

        assertEquals("DOC-1000", detail.getDocumentNo());
        assertEquals("APPROVED", detail.getDocumentStatus());
        assertEquals("TECHNICAL", detail.getBusinessCategory());
        assertEquals("INTERNAL", detail.getSensitiveLevel());
        assertEquals(true, detail.getHasExpireDate());
        assertEquals(expireDate, detail.getExpireDate());
        assertEquals(true, detail.getMetadataCompleted());
        assertEquals("APPROVED", detail.getApprovalStatus());
        assertEquals("APPROVED", detail.getVersionApprovalStatus());
    }

    @Test
    void searchDocumentsReturnsLifecycleFieldsInListItems() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        OffsetDateTime expireDate = OffsetDateTime.parse("2026-07-01T10:00:00+08:00");
        FolderEntity folder = folder(10L, "10");
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderPermissionService.canView(folder)).thenReturn(true);

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setName("技术方案");
        document.setCurrentVersionNo(2);
        document.setLatestSize(128L);
        document.setLatestMime("application/pdf");
        document.setOwnerUserId(7L);
        document.setDocumentNo("DOC-1000");
        document.setDocumentStatus("APPROVED");
        document.setBusinessCategory("TECHNICAL");
        document.setSensitiveLevel("INTERNAL");
        document.setHasExpireDate(true);
        document.setExpireDate(expireDate);
        document.setMetadataCompleted(true);

        Page<DocumentEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(document));
        page.setTotal(1);
        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        DocumentSearchReqDTO req = new DocumentSearchReqDTO();
        req.setFolderId(10L);
        req.setRecursive(false);

        var item = service.searchDocuments(req).getList().get(0);

        assertEquals("DOC-1000", item.getDocumentNo());
        assertEquals("APPROVED", item.getDocumentStatus());
        assertEquals("TECHNICAL", item.getBusinessCategory());
        assertEquals("INTERNAL", item.getSensitiveLevel());
        assertEquals(true, item.getHasExpireDate());
        assertEquals(expireDate, item.getExpireDate());
        assertEquals(true, item.getMetadataCompleted());
        assertEquals("APPROVED", item.getApprovalStatus());
        assertEquals(2, item.getCurrentVersionNo());
    }

    private static FolderEntity folder(Long id, String ancestorIds) {
        FolderEntity folder = new FolderEntity();
        folder.setId(id);
        folder.setAncestorIds(ancestorIds);
        folder.setLevel(1);
        folder.setDeleted(false);
        return folder;
    }
}
