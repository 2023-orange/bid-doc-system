package com.example.biddoc.document.service.impl;

import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.document.dto.req.DocumentUseGrantCreateReqDTO;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentUseGrantEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentUseGrantMapper;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentUseGrantServiceImplTest {

    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);
    private final DocumentUseGrantMapper documentUseGrantMapper = mock(DocumentUseGrantMapper.class);
    private final AuditService auditService = mock(AuditService.class);
    private final DocumentUseGrantServiceImpl service = new DocumentUseGrantServiceImpl(
            documentMapper,
            folderMapper,
            folderPermissionService,
            documentUseGrantMapper,
            auditService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createGrantWritesVersionScopedActiveGrantAndAudit() {
        UserContext.set(new UserContext.UserInfo(7L, "owner", List.of("EMPLOYEE"), 100L));

        DocumentEntity document = new DocumentEntity();
        document.setId(1000L);
        document.setFolderId(10L);
        document.setCurrentVersionNo(2);
        document.setOwnerUserId(7L);
        document.setDeleted(false);

        FolderEntity folder = new FolderEntity();
        folder.setId(10L);
        folder.setDeleted(false);

        DocumentUseGrantCreateReqDTO req = new DocumentUseGrantCreateReqDTO();
        req.setGranteeId(8L);
        req.setProjectId(900L);
        req.setGrantType("DOWNLOAD");
        req.setScenario("SENSITIVE_DOWNLOAD");
        req.setValidUntil(OffsetDateTime.now().plusDays(7));
        req.setReason("审批通过");

        when(documentMapper.selectById(1000L)).thenReturn(document);
        when(folderMapper.selectById(10L)).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);

        Long grantId = service.createGrant(1000L, req);

        verify(documentUseGrantMapper).insert(argThat(grant ->
                grant instanceof DocumentUseGrantEntity
                        && grantId.equals(((DocumentUseGrantEntity) grant).getId())
                        && Long.valueOf(1000L).equals(((DocumentUseGrantEntity) grant).getDocumentId())
                        && Integer.valueOf(2).equals(((DocumentUseGrantEntity) grant).getVersionNo())
                        && Long.valueOf(7L).equals(((DocumentUseGrantEntity) grant).getApplicantId())
                        && Long.valueOf(8L).equals(((DocumentUseGrantEntity) grant).getGranteeId())
                        && "ACTIVE".equals(((DocumentUseGrantEntity) grant).getStatus())
                        && Boolean.FALSE.equals(((DocumentUseGrantEntity) grant).getDeleted())
        ));
        verify(auditService).record(argThat(command ->
                "DOCUMENT".equals(command.getModuleCode())
                        && Long.valueOf(1000L).equals(command.getBizId())
                        && AuditOperationTypeEnum.GRANT_ADD.getCode().equals(command.getOperationType())
        ));
    }
}
