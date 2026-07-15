package com.example.biddoc.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.dto.req.DocumentUseGrantCreateReqDTO;
import com.example.biddoc.document.dto.resp.DocumentUseGrantRespDTO;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentUseGrantEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentUseGrantMapper;
import com.example.biddoc.document.service.DocumentUseGrantService;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocumentUseGrantServiceImpl implements DocumentUseGrantService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";

    private final DocumentMapper documentMapper;
    private final FolderMapper folderMapper;
    private final FolderPermissionService folderPermissionService;
    private final DocumentUseGrantMapper documentUseGrantMapper;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGrant(Long documentId, DocumentUseGrantCreateReqDTO req) {
        if (req == null || req.getGranteeId() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "被授权人不能为空");
        }
        DocumentEntity document = requireDocument(documentId);
        FolderEntity folder = requireFolder(document.getFolderId());
        folderPermissionService.checkView(folder);
        UserContext.UserInfo currentUser = requireCurrentUser();
        ensureGrantManager(document, folder, currentUser);

        Integer versionNo = req.getVersionNo() != null ? req.getVersionNo() : document.getCurrentVersionNo();
        if (versionNo == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "授权版本号不能为空");
        }
        OffsetDateTime validFrom = req.getValidFrom() != null ? req.getValidFrom() : OffsetDateTime.now();
        if (req.getValidUntil() != null && req.getValidUntil().isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "授权失效时间不能早于生效时间");
        }

        DocumentUseGrantEntity grant = new DocumentUseGrantEntity();
        grant.setId(IdWorker.getId());
        grant.setDocumentId(documentId);
        grant.setVersionNo(versionNo);
        grant.setProjectId(req.getProjectId());
        grant.setApplicantId(currentUser.getUserId());
        grant.setGranteeId(req.getGranteeId());
        grant.setApprovalInstanceId(req.getApprovalInstanceId());
        grant.setGrantType(StringUtils.hasText(req.getGrantType()) ? req.getGrantType() : "USE");
        grant.setScenario(req.getScenario());
        grant.setValidFrom(validFrom);
        grant.setValidUntil(req.getValidUntil());
        grant.setStatus(STATUS_ACTIVE);
        grant.setReason(req.getReason());
        grant.setDeleted(Boolean.FALSE);
        documentUseGrantMapper.insert(grant);

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT_USE_GRANT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.GRANT_ADD.getCode())
                .afterData(Map.of("grantId", grant.getId(), "versionNo", versionNo, "granteeId", req.getGranteeId()))
                .build());
        return grant.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeGrant(Long documentId, Long grantId) {
        DocumentEntity document = requireDocument(documentId);
        FolderEntity folder = requireFolder(document.getFolderId());
        folderPermissionService.checkView(folder);
        UserContext.UserInfo currentUser = requireCurrentUser();
        ensureGrantManager(document, folder, currentUser);

        DocumentUseGrantEntity grant = documentUseGrantMapper.selectById(grantId);
        if (grant == null || Boolean.TRUE.equals(grant.getDeleted()) || !Objects.equals(documentId, grant.getDocumentId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资料授权不存在");
        }
        DocumentUseGrantEntity update = new DocumentUseGrantEntity();
        update.setId(grantId);
        update.setStatus(STATUS_REVOKED);
        update.setDeleted(Boolean.TRUE);
        documentUseGrantMapper.updateById(update);

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT_USE_GRANT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.GRANT_REMOVE.getCode())
                .afterData(Map.of("grantId", grantId))
                .build());
    }

    @Override
    public List<DocumentUseGrantRespDTO> listGrants(Long documentId) {
        DocumentEntity document = requireDocument(documentId);
        FolderEntity folder = requireFolder(document.getFolderId());
        folderPermissionService.checkView(folder);
        return documentUseGrantMapper.selectList(new LambdaQueryWrapper<DocumentUseGrantEntity>()
                        .eq(DocumentUseGrantEntity::getDocumentId, documentId)
                        .eq(DocumentUseGrantEntity::getDeleted, false)
                        .orderByDesc(DocumentUseGrantEntity::getCreatedAt))
                .stream()
                .map(this::toRespDTO)
                .toList();
    }

    private DocumentEntity requireDocument(Long documentId) {
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private FolderEntity requireFolder(Long folderId) {
        FolderEntity folder = folderMapper.selectById(folderId);
        if (folder == null || Boolean.TRUE.equals(folder.getDeleted())) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private UserContext.UserInfo requireCurrentUser() {
        UserContext.UserInfo currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return currentUser;
    }

    private void ensureGrantManager(DocumentEntity document, FolderEntity folder, UserContext.UserInfo currentUser) {
        boolean canGrant = currentUser.isSuperAdmin()
                || Objects.equals(document.getOwnerUserId(), currentUser.getUserId())
                || folderPermissionService.isManagerOfFolder(folder, currentUser.getUserId());
        // 资料使用授权会扩大敏感资料访问面，只允许资料所有者、文件夹管理员或超级管理员维护。
        if (!canGrant) {
            throw new BusinessException(ErrorCode.DOCUMENT_OWNERSHIP_REQUIRED);
        }
    }

    private DocumentUseGrantRespDTO toRespDTO(DocumentUseGrantEntity entity) {
        DocumentUseGrantRespDTO dto = new DocumentUseGrantRespDTO();
        dto.setId(entity.getId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setVersionNo(entity.getVersionNo());
        dto.setProjectId(entity.getProjectId());
        dto.setApplicantId(entity.getApplicantId());
        dto.setGranteeId(entity.getGranteeId());
        dto.setApprovalInstanceId(entity.getApprovalInstanceId());
        dto.setGrantType(entity.getGrantType());
        dto.setScenario(entity.getScenario());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidUntil(entity.getValidUntil());
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
