package com.example.biddoc.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.common.util.AssertUtil;
import com.example.biddoc.document.dto.req.DocumentMetadataUpdateReqDTO;
import com.example.biddoc.document.dto.req.DocumentSearchReqDTO;
import com.example.biddoc.document.dto.resp.DocumentDetailRespDTO;
import com.example.biddoc.document.dto.resp.DocumentListItemRespDTO;
import com.example.biddoc.document.dto.resp.DocumentUploadRespDTO;
import com.example.biddoc.document.dto.resp.DocumentVersionRespDTO;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentTagEntity;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import com.example.biddoc.document.entity.DownloadLogEntity;
import com.example.biddoc.document.mapper.DocumentTagMapper;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.document.storage.StorageAdapter;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final FolderMapper folderMapper;
    private final FolderPermissionService folderPermissionService;
    private final com.example.biddoc.document.mapper.SearchHistoryMapper searchHistoryMapper;
    private final com.example.biddoc.document.mapper.DownloadLogMapper downloadLogMapper;
    private final DocumentTagMapper documentTagMapper;
    private final FolderFavoriteMapper folderFavoriteMapper;
    private final StorageAdapter storageAdapter;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private static final Tika TIKA = new Tika();
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String VERSION_PENDING = "PENDING";
    private static final String VERSION_APPROVED = "APPROVED";
    private static final String VERSION_REJECTED = "REJECTED";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadRespDTO uploadDocument(Long folderId, MultipartFile file, String name,
                                                String remark, String changeLog) {
        // 1. 校验文件
        AssertUtil.notNull(file, ErrorCode.DOCUMENT_FILE_REQUIRED);
        AssertUtil.isTrue(!file.isEmpty(), ErrorCode.DOCUMENT_FILE_REQUIRED);
        AssertUtil.isTrue(file.getSize() > 0 && file.getSize() <= MAX_FILE_SIZE,
            ErrorCode.DOCUMENT_SIZE_EXCEEDED);

        // 2. 校验 folder
        AssertUtil.notNull(folderId, ErrorCode.FOLDER_NOT_FOUND);
        AssertUtil.isTrue(folderId != 0, ErrorCode.DOCUMENT_ROOT_FOLDER_FORBIDDEN);

        FolderEntity folder = folderMapper.selectById(folderId);
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);

        // 3. 权限检查：createChild
        folderPermissionService.checkCreateChild(folder);

        // 4. 确定文档名称
        String documentName = determineName(name, file.getOriginalFilename());
        validateName(documentName);

        // 5. 检查同名
        checkNameUnique(folderId, documentName, null);

        // 6. 生成 ID
        Long documentId = IdWorker.getId();
        Long versionId = IdWorker.getId();

        UserContext.UserInfo currentUser = UserContext.get();

        try {
            // 7. 写入存储
            InputStream inputStream = file.getInputStream();
            String hintExt = extractExtension(file.getOriginalFilename());
            String storageKey = storageAdapter.put(inputStream, file.getSize(), null, hintExt);

            // 8. 探测 MIME
            String mimeType = detectMimeType(storageKey);

            // 9. 计算 hash
            String contentHash = calculateHash(storageKey);

            // 10. 插入版本记录
            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setId(versionId);
            version.setDocumentId(documentId);
            version.setVersionNo(1);
            version.setStorageKey(storageKey);
            version.setSize(file.getSize());
            version.setMimeType(mimeType);
            version.setOriginalFilename(file.getOriginalFilename());
            version.setContentHash(contentHash);
            version.setUploadedByUserId(currentUser.getUserId());
            version.setChangeLog(changeLog);
            version.setApprovalStatus(VERSION_APPROVED);
            version.setApprovedAt(OffsetDateTime.now());
            documentVersionMapper.insert(version);

            // 11. 插入文档记录
            DocumentEntity document = new DocumentEntity();
            document.setId(documentId);
            document.setFolderId(folderId);
            document.setName(documentName);
            document.setCurrentVersionNo(1);
            document.setLatestSize(file.getSize());
            document.setLatestMime(mimeType);
            document.setOwnerUserId(currentUser.getUserId());
            document.setOwnerDeptId(currentUser.getDeptId());
            document.setStatus(1);
            document.setDocumentNo("DOC-" + documentId);
            document.setDocumentStatus("INCOMPLETE");
            document.setMetadataCompleted(Boolean.FALSE);
            document.setHasExpireDate(Boolean.FALSE);
            document.setRemark(remark);
            documentMapper.insert(document);

            // 12. 审计
            Map<String, Object> afterData = new HashMap<>();
            afterData.put("name", documentName);
            afterData.put("folderId", folderId);
            afterData.put("versionNo", 1);
            afterData.put("size", file.getSize());
            afterData.put("mimeType", mimeType);
            afterData.put("storageKey", storageKey);
            afterData.put("originalFilename", file.getOriginalFilename());

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("contentHash", contentHash);

            auditService.record(
                AuditRecordCommand.builder()
                    .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                    .bizType("DOCUMENT")
                    .bizId(documentId)
                    .operationType(AuditOperationTypeEnum.UPLOAD.getCode())
                    .beforeData(null)
                    .afterData(afterData)
                    .extraData(extraData)
                    .build()
            );

            // 13. 返回响应
            return new DocumentUploadRespDTO(
                String.valueOf(documentId),
                1,
                documentName,
                file.getSize(),
                mimeType
            );

        } catch (Exception e) {
            log.error("文档上传失败: folderId={}, name={}", folderId, documentName, e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED, "文档上传失败: " + e.getMessage());
        }
    }

    @Override
    public DocumentDetailRespDTO getDocumentDetail(Long documentId) {
        // 1. 查询文档
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 2. 权限检查：folder.view
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        // 3. 查询当前版本
        DocumentVersionEntity currentVersion = documentVersionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, document.getCurrentVersionNo())
        );

        // 4. 组装响应
        DocumentDetailRespDTO resp = new DocumentDetailRespDTO();
        resp.setId(String.valueOf(document.getId()));
        resp.setFolderId(String.valueOf(document.getFolderId()));
        resp.setName(document.getName());
        resp.setCurrentVersionNo(document.getCurrentVersionNo());
        resp.setOwnerUserId(String.valueOf(document.getOwnerUserId()));
        resp.setOwnerDeptId(document.getOwnerDeptId() != null ? String.valueOf(document.getOwnerDeptId()) : null);
        resp.setRemark(document.getRemark());
        resp.setCreatedAt(document.getCreatedAt());
        resp.setUpdatedAt(document.getUpdatedAt());

        if (currentVersion != null) {
            resp.setCurrentVersion(toVersionRespDTO(currentVersion));
        }

        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetadata(Long documentId, DocumentMetadataUpdateReqDTO req) {
        DocumentEntity document = requireDocumentForLifecycle(documentId);
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);
        if (Boolean.TRUE.equals(req.getHasExpireDate()) && req.getExpireDate() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "有有效期资料必须填写失效日期");
        }

        DocumentEntity update = new DocumentEntity();
        update.setId(documentId);
        update.setName(StringUtils.hasText(req.getDocumentName()) ? req.getDocumentName() : document.getName());
        update.setBusinessCategory(req.getBusinessCategory());
        update.setTenderStructureCategory(req.getTenderStructureCategory());
        update.setSensitiveLevel(req.getSensitiveLevel());
        update.setOwnerDeptId(req.getOwnerDeptId());
        update.setSourceType(req.getSourceType());
        update.setHasExpireDate(Boolean.TRUE.equals(req.getHasExpireDate()));
        update.setEffectiveDate(req.getEffectiveDate());
        update.setExpireDate(req.getExpireDate());
        update.setRemark(req.getRemark());
        update.setMetadataCompleted(Boolean.TRUE);
        update.setDocumentStatus("READY_SUBMIT");
        if (!StringUtils.hasText(document.getDocumentNo())) {
            update.setDocumentNo("DOC-" + documentId);
        }
        // 元数据变更不生成文件版本，只更新主表并通过审计保留业务变更痕迹。
        documentMapper.updateById(update);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .afterData(Map.of("documentStatus", "READY_SUBMIT"))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApproving(Long documentId) {
        updateDocumentStatus(documentId, "APPROVING", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApprovalResult(Long documentId, boolean approved, String reason) {
        updateDocumentStatus(documentId, approved ? "APPROVED" : "REJECTED", reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markVersionApproving(Long documentId, Integer versionNo) {
        updateVersionApprovalStatus(documentId, versionNo, VERSION_PENDING, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markVersionApprovalResult(Long documentId, Integer versionNo, boolean approved, String reason) {
        updateVersionApprovalStatus(documentId, versionNo,
                approved ? VERSION_APPROVED : VERSION_REJECTED, reason, approved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidDocument(Long documentId, String reason) {
        updateDocumentStatus(documentId, "VOIDED", reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreDocument(Long documentId) {
        updateDocumentStatus(documentId, "READY_SUBMIT", null);
    }

    @Override
    public PageResponse<DocumentListItemRespDTO> listBindableDocuments(DocumentSearchReqDTO searchReq) {
        if (searchReq == null) {
            searchReq = new DocumentSearchReqDTO();
        }
        searchReq.setDocumentStatus("APPROVED");
        searchReq.setExcludeExpired(true);
        return searchDocuments(searchReq);
    }

    @Override
    public PageResponse<DocumentListItemRespDTO> listDocuments(Long folderId, Integer page, Integer size,
                                                               String sort, String order) {
        // 1. 校验参数
        AssertUtil.notNull(folderId, ErrorCode.FOLDER_NOT_FOUND);
        page = page == null || page < 1 ? 1 : page;
        size = size == null || size <= 0 || size > 100 ? 20 : size;
        sort = StringUtils.hasText(sort) ? sort : "createdAt";
        order = "asc".equalsIgnoreCase(order) ? "asc" : "desc";

        // 2. 权限检查：folder.view
        FolderEntity folder = folderMapper.selectById(folderId);
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        // 3. 查询分页
        Page<DocumentEntity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<DocumentEntity>()
            .eq(DocumentEntity::getFolderId, folderId);

        // 排序
        switch (sort) {
            case "name":
                wrapper.orderBy(true, "asc".equals(order), DocumentEntity::getName);
                break;
            case "size":
                wrapper.orderBy(true, "asc".equals(order), DocumentEntity::getLatestSize);
                break;
            case "createdAt":
            default:
                wrapper.orderBy(true, "asc".equals(order), DocumentEntity::getCreatedAt);
                break;
        }

        IPage<DocumentEntity> pageResult = documentMapper.selectPage(pageParam, wrapper);

        // 4. 转换 DTO
        List<DocumentListItemRespDTO> items = pageResult.getRecords().stream()
            .map(this::toListItemRespDTO)
            .collect(Collectors.toList());

        return new PageResponse<>(
            items,
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getPages(),
            pageResult.getCurrent() < pageResult.getPages()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        // 1. 查询文档
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 2. 权限检查：严格三元
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkEdit(folder);

        UserContext.UserInfo currentUser = UserContext.get();
        boolean isSuperAdmin = currentUser.getRoleCodes().contains("SUPER_ADMIN");
        boolean isOwner = Objects.equals(document.getOwnerUserId(), currentUser.getUserId());
        boolean isManager = folderPermissionService.isManagerOfFolder(folder, currentUser.getUserId());

        AssertUtil.isTrue(isSuperAdmin || isOwner || isManager, ErrorCode.DOCUMENT_OWNERSHIP_REQUIRED);

        // 3. 软删除文档（使用 MyBatis-Plus 的 deleteById 触发逻辑删除）
        documentMapper.deleteById(documentId);

        // 4. 软删除所有版本
        int affectedVersions = documentVersionMapper.batchSoftDeleteByDocumentIds(
            Collections.singletonList(documentId),
            currentUser.getUsername()
        );

        // 5. 审计
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("name", document.getName());
        beforeData.put("folderId", document.getFolderId());
        beforeData.put("currentVersionNo", document.getCurrentVersionNo());

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("deleted", true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("affectedVersions", affectedVersions);

        auditService.record(
            AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.DELETE.getCode())
                .beforeData(beforeData)
                .afterData(afterData)
                .extraData(extraData)
                .build()
        );

        log.info("文档删除成功: documentId={}, affectedVersions={}", documentId, affectedVersions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionRespDTO uploadNewVersion(Long documentId, MultipartFile file, String changeLog) {
        // 1. 校验文件
        AssertUtil.notNull(file, ErrorCode.DOCUMENT_FILE_REQUIRED);
        AssertUtil.isTrue(!file.isEmpty(), ErrorCode.DOCUMENT_FILE_REQUIRED);
        AssertUtil.isTrue(file.getSize() > 0 && file.getSize() <= MAX_FILE_SIZE,
            ErrorCode.DOCUMENT_SIZE_EXCEEDED);

        // 2. 锁定文档（SELECT FOR UPDATE）
        DocumentEntity document = documentMapper.selectOne(
            new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getId, documentId)
                .last("FOR UPDATE")
        );
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 3. 权限检查：严格三元
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkEdit(folder);

        UserContext.UserInfo currentUser = UserContext.get();
        boolean isSuperAdmin = currentUser.getRoleCodes().contains("SUPER_ADMIN");
        boolean isOwner = Objects.equals(document.getOwnerUserId(), currentUser.getUserId());
        boolean isManager = folderPermissionService.isManagerOfFolder(folder, currentUser.getUserId());

        AssertUtil.isTrue(isSuperAdmin || isOwner || isManager, ErrorCode.DOCUMENT_OWNERSHIP_REQUIRED);

        // 4. 版本号按历史最大值递增，避免当前版本停留在旧版本时重复生成待审批版本号。
        int nextVersionNo = nextVersionNo(documentId);

        try {
            // 5. 写入存储
            InputStream inputStream = file.getInputStream();
            String hintExt = extractExtension(file.getOriginalFilename());
            String storageKey = storageAdapter.put(inputStream, file.getSize(), null, hintExt);

            // 6. 探测 MIME
            String mimeType = detectMimeType(storageKey);

            // 7. 计算 hash
            String contentHash = calculateHash(storageKey);

            // 8. 插入版本记录
            Long versionId = IdWorker.getId();
            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setId(versionId);
            version.setDocumentId(documentId);
            version.setVersionNo(nextVersionNo);
            version.setStorageKey(storageKey);
            version.setSize(file.getSize());
            version.setMimeType(mimeType);
            version.setOriginalFilename(file.getOriginalFilename());
            version.setContentHash(contentHash);
            version.setUploadedByUserId(currentUser.getUserId());
            version.setChangeLog(changeLog);
            version.setApprovalStatus(VERSION_PENDING);
            documentVersionMapper.insert(version);

            // 9. 新版本需要审批通过后才切换 currentVersionNo，避免未通过版本污染当前可用资料。

            // 10. 审计
            Map<String, Object> beforeData = new HashMap<>();
            beforeData.put("previousVersionNo", nextVersionNo - 1);
            beforeData.put("previousSize", document.getLatestSize());

            Map<String, Object> afterData = new HashMap<>();
            afterData.put("newVersionNo", nextVersionNo);
            afterData.put("size", file.getSize());
            afterData.put("mimeType", mimeType);
            afterData.put("originalFilename", file.getOriginalFilename());
            afterData.put("approvalStatus", VERSION_PENDING);

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("contentHash", contentHash);
            extraData.put("changeLog", changeLog);

            auditService.record(
                AuditRecordCommand.builder()
                    .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                    .bizType("DOCUMENT")
                    .bizId(documentId)
                    .operationType(AuditOperationTypeEnum.NEW_VERSION.getCode())
                    .beforeData(beforeData)
                    .afterData(afterData)
                    .extraData(extraData)
                    .build()
            );

            if (folder.getOwnerUserId() != null && !Objects.equals(folder.getOwnerUserId(), currentUser.getUserId())) {
                notificationService.send(folder.getOwnerUserId(),
                        "DOCUMENT_NEW_VERSION",
                        "文档上传了新版本",
                        "文档已上传新版本：" + document.getName(),
                        "DOCUMENT",
                        documentId);
            }

            return toVersionRespDTO(version);

        } catch (Exception e) {
            log.error("上传新版本失败: documentId={}", documentId, e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED, "上传新版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<DocumentVersionRespDTO> listVersions(Long documentId) {
        // 1. 查询文档
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 2. 权限检查：folder.view
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        // 3. 查询版本列表（降序）
        List<DocumentVersionEntity> versions = documentVersionMapper.selectList(
            new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .orderByDesc(DocumentVersionEntity::getVersionNo)
        );

        return versions.stream()
            .map(this::toVersionRespDTO)
            .collect(Collectors.toList());
    }

    @Override
    public DownloadResult downloadDocument(Long documentId) {
        return downloadDocument(documentId, null, null);
    }

    @Override
    public DownloadResult downloadDocument(Long documentId, String ipAddress, String userAgent) {
        // 1. 查询文档
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 2. 权限检查：folder.view
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        // 3. 查询当前版本
        DocumentVersionEntity version = documentVersionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, document.getCurrentVersionNo())
        );
        AssertUtil.notNull(version, ErrorCode.DOCUMENT_VERSION_NOT_FOUND);

        // 4. 读取存储
        InputStream inputStream = storageAdapter.get(version.getStorageKey());

        // 5. 下载日志用于热门文档统计，失败不阻断文件流返回
        recordDownloadLog(documentId, version.getVersionNo(), ipAddress, userAgent);

        // 6. 异步审计（不阻塞响应）
        asyncAuditDownload(documentId, version.getVersionNo(), version.getSize());

        return new DownloadResult(
            inputStream,
            version.getOriginalFilename(),
            version.getMimeType(),
            version.getSize(),
            version.getVersionNo()
        );
    }

    @Override
    public DownloadResult downloadVersion(Long documentId, Integer versionNo) {
        return downloadVersion(documentId, versionNo, null, null);
    }

    @Override
    public DownloadResult downloadVersion(Long documentId, Integer versionNo, String ipAddress, String userAgent) {
        // 1. 查询文档
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        // 2. 权限检查：folder.view
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        // 3. 查询指定版本
        DocumentVersionEntity version = documentVersionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, versionNo)
        );
        AssertUtil.notNull(version, ErrorCode.DOCUMENT_VERSION_NOT_FOUND);

        // 4. 读取存储
        InputStream inputStream = storageAdapter.get(version.getStorageKey());

        // 5. 下载日志用于热门文档统计，失败不阻断文件流返回
        recordDownloadLog(documentId, versionNo, ipAddress, userAgent);

        // 6. 异步审计（不阻塞响应）
        asyncAuditDownload(documentId, versionNo, version.getSize());

        return new DownloadResult(
            inputStream,
            version.getOriginalFilename(),
            version.getMimeType(),
            version.getSize(),
            versionNo
        );
    }

    @Override
    public DownloadResult previewDocument(Long documentId, String ipAddress, String userAgent) {
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);
        return previewVersion(documentId, document.getCurrentVersionNo(), ipAddress, userAgent);
    }

    @Override
    public DownloadResult previewVersion(Long documentId, Integer versionNo, String ipAddress, String userAgent) {
        // 预览复用下载的查看权限，但只允许浏览器原生可预览类型，避免引入转换服务。
        DocumentEntity document = documentMapper.selectById(documentId);
        AssertUtil.notNull(document, ErrorCode.DOCUMENT_NOT_FOUND);

        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        DocumentVersionEntity version = documentVersionMapper.selectOne(
            new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, versionNo)
        );
        AssertUtil.notNull(version, ErrorCode.DOCUMENT_VERSION_NOT_FOUND);

        if (!isPreviewableMimeType(version.getMimeType())) {
            throw new BusinessException(ErrorCode.DOCUMENT_PREVIEW_UNSUPPORTED);
        }

        InputStream inputStream = storageAdapter.get(version.getStorageKey());
        asyncAuditPreview(documentId, versionNo, version.getSize(), ipAddress, userAgent);

        return new DownloadResult(
            inputStream,
            version.getOriginalFilename(),
            version.getMimeType(),
            version.getSize(),
            versionNo
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cascadeSoftDeleteByFolderIds(List<Long> folderIds) {
        if (CollectionUtils.isEmpty(folderIds)) {
            return 0;
        }

        UserContext.UserInfo currentUser = UserContext.get();
        String updatedBy = currentUser != null ? currentUser.getUsername() : "system";

        // 1. 查询受影响的 document ID
        List<DocumentEntity> documents = documentMapper.selectList(
            new LambdaQueryWrapper<DocumentEntity>()
                .in(DocumentEntity::getFolderId, folderIds)
        );

        if (documents.isEmpty()) {
            return 0;
        }

        List<Long> documentIds = documents.stream()
            .map(DocumentEntity::getId)
            .collect(Collectors.toList());

        // 2. 软删除文档
        int affectedDocuments = documentMapper.batchSoftDeleteByFolderIds(folderIds, updatedBy);

        // 3. 软删除版本
        int affectedVersions = documentVersionMapper.batchSoftDeleteByDocumentIds(documentIds, updatedBy);

        log.info("级联软删除文档完成: folderIds={}, affectedDocuments={}, affectedVersions={}",
            folderIds, affectedDocuments, affectedVersions);

        return affectedDocuments;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 确定文档名称
     */
    private String determineName(String name, String originalFilename) {
        if (StringUtils.hasText(name)) {
            return name.trim();
        }

        if (StringUtils.hasText(originalFilename)) {
            // 去掉扩展名
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot > 0) {
                return originalFilename.substring(0, lastDot).trim();
            }
            return originalFilename.trim();
        }

        throw new BusinessException(ErrorCode.DOCUMENT_NAME_INVALID, "文档名称不能为空");
    }

    /**
     * 校验文档名称
     */
    private void validateName(String name) {
        AssertUtil.isTrue(StringUtils.hasText(name), ErrorCode.DOCUMENT_NAME_INVALID);
        AssertUtil.isTrue(name.length() <= 255, ErrorCode.DOCUMENT_NAME_INVALID);
        AssertUtil.isTrue(!name.contains("/") && !name.contains("\\") && !name.contains("\0"),
            ErrorCode.DOCUMENT_NAME_INVALID);
    }

    /**
     * 检查同名唯一性
     */
    private void checkNameUnique(Long folderId, String name, Long excludeDocumentId) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<DocumentEntity>()
            .eq(DocumentEntity::getFolderId, folderId)
            .eq(DocumentEntity::getName, name);

        if (excludeDocumentId != null) {
            wrapper.ne(DocumentEntity::getId, excludeDocumentId);
        }

        Long count = documentMapper.selectCount(wrapper);
        AssertUtil.isTrue(count == 0, ErrorCode.DOCUMENT_NAME_DUPLICATED);
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return null;
    }

    /**
     * 探测 MIME 类型
     */
    private String detectMimeType(String storageKey) {
        try {
            InputStream is = storageAdapter.get(storageKey);
            String mimeType = TIKA.detect(is);
            is.close();
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (Exception e) {
            log.warn("MIME 探测失败: storageKey={}", storageKey, e);
            return "application/octet-stream";
        }
    }

    /**
     * 计算文件 SHA-256 hash
     */
    private String calculateHash(String storageKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream is = storageAdapter.get(storageKey);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            is.close();

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("计算 hash 失败: storageKey={}", storageKey, e);
            return null;
        }
    }

    /**
     * 异步审计下载事件
     */
    private void asyncAuditDownload(Long documentId, Integer versionNo, Long size) {
        try {
            Map<String, Object> afterData = new HashMap<>();
            afterData.put("versionNo", versionNo);
            afterData.put("size", size);

            auditService.record(
                AuditRecordCommand.builder()
                    .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                    .bizType("DOCUMENT")
                    .bizId(documentId)
                    .operationType(AuditOperationTypeEnum.DOWNLOAD.getCode())
                    .beforeData(null)
                    .afterData(afterData)
                    .extraData(Collections.emptyMap())
                    .build()
            );
        } catch (Exception e) {
            log.warn("下载审计失败: documentId={}, versionNo={}", documentId, versionNo, e);
        }
    }

    /**
     * 记录下载统计日志。统计日志与审计日志职责不同：前者用于排行/报表，后者用于追溯。
     * 这里吞掉异常，避免统计表异常影响真实文件下载。
     */
    private void recordDownloadLog(Long documentId, Integer versionNo, String ipAddress, String userAgent) {
        try {
            UserContext.UserInfo currentUser = UserContext.get();
            if (currentUser == null) {
                return;
            }
            DownloadLogEntity logEntity = new DownloadLogEntity();
            logEntity.setDocumentId(documentId);
            logEntity.setVersionNo(versionNo);
            logEntity.setUserId(currentUser.getUserId());
            logEntity.setDownloadTime(java.time.OffsetDateTime.now());
            logEntity.setIpAddress(ipAddress);
            logEntity.setUserAgent(userAgent);
            logEntity.setDeleted(Boolean.FALSE);
            downloadLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("记录下载统计日志失败: documentId={}, versionNo={}", documentId, versionNo, e);
        }
    }

    private void asyncAuditPreview(Long documentId, Integer versionNo, Long size, String ipAddress, String userAgent) {
        try {
            Map<String, Object> afterData = new HashMap<>();
            afterData.put("versionNo", versionNo);
            afterData.put("size", size);

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("ipAddress", ipAddress);
            extraData.put("userAgent", userAgent);

            auditService.record(
                AuditRecordCommand.builder()
                    .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                    .bizType("DOCUMENT")
                    .bizId(documentId)
                    .operationType(AuditOperationTypeEnum.PREVIEW.getCode())
                    .afterData(afterData)
                    .extraData(extraData)
                    .build()
            );
        } catch (Exception e) {
            log.warn("预览审计失败: documentId={}, versionNo={}", documentId, versionNo, e);
        }
    }

    private boolean isPreviewableMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return false;
        }
        return "application/pdf".equals(mimeType)
                || mimeType.startsWith("image/")
                || mimeType.startsWith("text/");
    }

    /**
     * 转换为版本响应 DTO
     */
    private DocumentVersionRespDTO toVersionRespDTO(DocumentVersionEntity version) {
        DocumentVersionRespDTO dto = new DocumentVersionRespDTO();
        dto.setVersionNo(version.getVersionNo());
        dto.setSize(version.getSize());
        dto.setMimeType(version.getMimeType());
        dto.setOriginalFilename(version.getOriginalFilename());
        dto.setUploadedByUserId(String.valueOf(version.getUploadedByUserId()));
        dto.setUploadedAt(version.getCreatedAt());
        dto.setChangeLog(version.getChangeLog());
        dto.setApprovalStatus(version.getApprovalStatus());
        dto.setApprovedAt(version.getApprovedAt());
        dto.setRejectedReason(version.getRejectedReason());
        return dto;
    }

    /**
     * 转换为列表项响应 DTO
     */
    private DocumentListItemRespDTO toListItemRespDTO(DocumentEntity document) {
        DocumentListItemRespDTO dto = new DocumentListItemRespDTO();
        dto.setId(String.valueOf(document.getId()));
        dto.setName(document.getName());
        dto.setCurrentVersionNo(document.getCurrentVersionNo());
        dto.setLatestSize(document.getLatestSize());
        dto.setLatestMime(document.getLatestMime());
        dto.setOwnerUserId(String.valueOf(document.getOwnerUserId()));
        dto.setCreatedAt(document.getCreatedAt());
        return dto;
    }

    @Override
    public PageResponse<DocumentListItemRespDTO> searchDocuments(com.example.biddoc.document.dto.req.DocumentSearchReqDTO searchReq) {
        // 1. 参数校验和默认值处理
        Integer page = searchReq.getPage() != null && searchReq.getPage() > 0 ? searchReq.getPage() : 1;
        Integer size = searchReq.getSize() != null && searchReq.getSize() > 0 && searchReq.getSize() <= 100
                ? searchReq.getSize() : 20;
        String sortBy = StringUtils.hasText(searchReq.getSortBy()) ? searchReq.getSortBy() : "createdAt";
        String sortOrder = "asc".equalsIgnoreCase(searchReq.getSortOrder()) ? "asc" : "desc";
        Boolean recursive = searchReq.getRecursive() != null ? searchReq.getRecursive() : true;

        UserContext.UserInfo currentUser = UserContext.get();

        // 2. 构建查询条件
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();

        // 2.1 关键词搜索（文档名称模糊匹配）
        if (StringUtils.hasText(searchReq.getKeyword())) {
            wrapper.like(DocumentEntity::getName, searchReq.getKeyword());
        }

        // 2.2 文件夹范围过滤
        if (searchReq.getFolderId() != null) {
            FolderEntity folder = folderMapper.selectById(searchReq.getFolderId());
            AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);

            // 权限检查：必须对指定文件夹有 view 权限
            folderPermissionService.checkView(folder);

            if (recursive) {
                // 递归搜索：查询该文件夹及其所有子文件夹下的文档
                // 使用 ancestorIds 字段：子文件夹的 ancestorIds 包含父文件夹ID
                List<Long> folderIds = new ArrayList<>();
                folderIds.add(searchReq.getFolderId());

                // 递归范围必须先按边界安全的子树查询，再逐个过滤 view 权限，避免父级可见时泄露不可见子目录文档。
                List<FolderEntity> subFolders = folderMapper.selectSubtree(searchReq.getFolderId());
                folderIds = subFolders.stream()
                        .filter(folderPermissionService::canView)
                        .map(FolderEntity::getId)
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new));

                if (CollectionUtils.isEmpty(folderIds)) {
                    return emptyPage(page, size);
                }
                wrapper.in(DocumentEntity::getFolderId, folderIds);
            } else {
                // 非递归：仅搜索指定文件夹
                wrapper.eq(DocumentEntity::getFolderId, searchReq.getFolderId());
            }
        } else {
            // 未指定文件夹：搜索所有有权限的文件夹下的文档
            // 查询当前用户有 view 权限的所有文件夹
            List<Long> accessibleFolderIds = getAccessibleFolderIds(currentUser);

            if (CollectionUtils.isEmpty(accessibleFolderIds)) {
                // 没有任何可访问的文件夹，返回空结果
                return emptyPage(page, size);
            }

            wrapper.in(DocumentEntity::getFolderId, accessibleFolderIds);
        }

        // 2.3 排序
        applyAdvancedSearchFilters(searchReq, currentUser, wrapper, page, size);

        // 2.4 排序
        switch (sortBy) {
            case "name":
                wrapper.orderBy(true, "asc".equals(sortOrder), DocumentEntity::getName);
                break;
            case "size":
                wrapper.orderBy(true, "asc".equals(sortOrder), DocumentEntity::getLatestSize);
                break;
            case "updatedAt":
                wrapper.orderBy(true, "asc".equals(sortOrder), DocumentEntity::getUpdatedAt);
                break;
            case "createdAt":
            default:
                wrapper.orderBy(true, "asc".equals(sortOrder), DocumentEntity::getCreatedAt);
                break;
        }

        // 3. 分页查询
        Page<DocumentEntity> pageParam = new Page<>(page, size);
        IPage<DocumentEntity> pageResult = documentMapper.selectPage(pageParam, wrapper);

        // 4. 转换 DTO
        List<DocumentListItemRespDTO> items = pageResult.getRecords().stream()
            .map(this::toListItemRespDTO)
            .collect(Collectors.toList());

        // 5. 记录搜索历史（仅当有关键词时记录）
        if (StringUtils.hasText(searchReq.getKeyword())) {
            recordSearchHistory(currentUser.getUserId(), searchReq.getKeyword(),
                searchReq.getFolderId(), (int) pageResult.getTotal());
        }

        return new PageResponse<>(
            items,
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getPages(),
            pageResult.getCurrent() < pageResult.getPages()
        );
    }

    private void applyAdvancedSearchFilters(DocumentSearchReqDTO searchReq,
                                            UserContext.UserInfo currentUser,
                                            LambdaQueryWrapper<DocumentEntity> wrapper,
                                            Integer page,
                                            Integer size) {
        if (StringUtils.hasText(searchReq.getMimeType())) {
            wrapper.eq(DocumentEntity::getLatestMime, searchReq.getMimeType());
        }
        if (StringUtils.hasText(searchReq.getDocumentNo())) {
            wrapper.eq(DocumentEntity::getDocumentNo, searchReq.getDocumentNo());
        }
        if (StringUtils.hasText(searchReq.getDocumentStatus())) {
            wrapper.eq(DocumentEntity::getDocumentStatus, searchReq.getDocumentStatus());
        }
        if (StringUtils.hasText(searchReq.getBusinessCategory())) {
            wrapper.eq(DocumentEntity::getBusinessCategory, searchReq.getBusinessCategory());
        }
        if (StringUtils.hasText(searchReq.getSensitiveLevel())) {
            wrapper.eq(DocumentEntity::getSensitiveLevel, searchReq.getSensitiveLevel());
        }
        if (searchReq.getOwnerDeptId() != null) {
            wrapper.eq(DocumentEntity::getOwnerDeptId, searchReq.getOwnerDeptId());
        }
        if (Boolean.TRUE.equals(searchReq.getExpiredOnly())) {
            wrapper.eq(DocumentEntity::getHasExpireDate, true)
                    .lt(DocumentEntity::getExpireDate, java.time.OffsetDateTime.now());
        }
        if (Boolean.TRUE.equals(searchReq.getExcludeExpired())) {
            wrapper.and(q -> q.ne(DocumentEntity::getHasExpireDate, true)
                    .or()
                    .ge(DocumentEntity::getExpireDate, java.time.OffsetDateTime.now()));
        }
        if (searchReq.getOwnerUserId() != null) {
            wrapper.eq(DocumentEntity::getOwnerUserId, searchReq.getOwnerUserId());
        }
        if (searchReq.getCreatedFrom() != null) {
            wrapper.ge(DocumentEntity::getCreatedAt, searchReq.getCreatedFrom());
        }
        if (searchReq.getCreatedTo() != null) {
            wrapper.le(DocumentEntity::getCreatedAt, searchReq.getCreatedTo());
        }

        if (Boolean.TRUE.equals(searchReq.getFavoriteFolderOnly())) {
            List<Long> favoriteFolderIds = getFavoriteFolderIds(currentUser);
            if (favoriteFolderIds.isEmpty()) {
                wrapper.eq(DocumentEntity::getId, -1L);
            } else {
                wrapper.in(DocumentEntity::getFolderId, favoriteFolderIds);
            }
        }

        if (!CollectionUtils.isEmpty(searchReq.getTagIds())) {
            List<Long> documentIds = documentTagMapper.selectList(
                    new LambdaQueryWrapper<DocumentTagEntity>()
                            .eq(DocumentTagEntity::getDeleted, false)
                            .in(DocumentTagEntity::getTagId, searchReq.getTagIds())
            ).stream().map(DocumentTagEntity::getDocumentId).distinct().toList();
            if (documentIds.isEmpty()) {
                wrapper.eq(DocumentEntity::getId, -1L);
            } else {
                wrapper.in(DocumentEntity::getId, documentIds);
            }
        }
    }

    private List<Long> getFavoriteFolderIds(UserContext.UserInfo currentUser) {
        if (currentUser == null) {
            return Collections.emptyList();
        }
        return folderFavoriteMapper.selectList(
                new LambdaQueryWrapper<FolderFavoriteEntity>()
                        .eq(FolderFavoriteEntity::getUserId, currentUser.getUserId())
                        .eq(FolderFavoriteEntity::getDeleted, false)
        ).stream().map(FolderFavoriteEntity::getFolderId).distinct().toList();
    }

    /**
     * 记录搜索历史
     * 异步记录，不影响搜索性能
     */
    private void recordSearchHistory(Long userId, String keyword, Long folderId, int resultCount) {
        try {
            com.example.biddoc.document.entity.SearchHistoryEntity history =
                new com.example.biddoc.document.entity.SearchHistoryEntity();
            history.setUserId(userId);
            history.setKeyword(keyword);
            history.setFolderId(folderId);
            history.setResultCount(resultCount);
            history.setSearchTime(java.time.OffsetDateTime.now());
            searchHistoryMapper.insert(history);
        } catch (Exception e) {
            // 记录失败不影响搜索功能
            log.warn("记录搜索历史失败: userId={}, keyword={}", userId, keyword, e);
        }
    }

    @Override
    public List<com.example.biddoc.document.dto.resp.SearchHistoryRespDTO> getSearchHistory(Integer limit) {
        UserContext.UserInfo currentUser = UserContext.get();
        limit = limit != null && limit > 0 && limit <= 50 ? limit : 10;

        // 查询用户的搜索历史（按时间倒序）
        List<com.example.biddoc.document.entity.SearchHistoryEntity> histories =
            searchHistoryMapper.selectList(
                new LambdaQueryWrapper<com.example.biddoc.document.entity.SearchHistoryEntity>()
                    .eq(com.example.biddoc.document.entity.SearchHistoryEntity::getUserId, currentUser.getUserId())
                    .orderByDesc(com.example.biddoc.document.entity.SearchHistoryEntity::getSearchTime)
                    .last("LIMIT " + limit)
            );

        // 转换为 DTO
        return histories.stream()
            .map(this::toSearchHistoryRespDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSearchHistory() {
        UserContext.UserInfo currentUser = UserContext.get();

        // 软删除当前用户的所有搜索历史
        searchHistoryMapper.delete(
            new LambdaQueryWrapper<com.example.biddoc.document.entity.SearchHistoryEntity>()
                .eq(com.example.biddoc.document.entity.SearchHistoryEntity::getUserId, currentUser.getUserId())
        );

        log.info("用户搜索历史已清除: userId={}", currentUser.getUserId());
    }

    @Override
    public List<com.example.biddoc.document.dto.resp.HotSearchRespDTO> getHotSearchKeywords(Integer limit, Integer days) {
        limit = limit != null && limit > 0 && limit <= 50 ? limit : 10;
        days = days != null && days > 0 && days <= 30 ? days : 7;

        // 计算起始时间
        java.time.OffsetDateTime startTime = java.time.OffsetDateTime.now().minusDays(days);

        // 查询指定天数内的搜索记录，按关键词分组统计
        List<com.example.biddoc.document.entity.SearchHistoryEntity> histories =
            searchHistoryMapper.selectList(
                new LambdaQueryWrapper<com.example.biddoc.document.entity.SearchHistoryEntity>()
                    .ge(com.example.biddoc.document.entity.SearchHistoryEntity::getSearchTime, startTime)
                    .isNotNull(com.example.biddoc.document.entity.SearchHistoryEntity::getKeyword)
            );

        // 按关键词分组统计
        Map<String, Long> keywordCountMap = histories.stream()
            .collect(Collectors.groupingBy(
                com.example.biddoc.document.entity.SearchHistoryEntity::getKeyword,
                Collectors.counting()
            ));

        // 转换为 DTO 并排序
        return keywordCountMap.entrySet().stream()
            .map(entry -> {
                com.example.biddoc.document.dto.resp.HotSearchRespDTO dto =
                    new com.example.biddoc.document.dto.resp.HotSearchRespDTO();
                dto.setKeyword(entry.getKey());
                dto.setSearchCount(entry.getValue());
                return dto;
            })
            .sorted((a, b) -> Long.compare(b.getSearchCount(), a.getSearchCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 转换为搜索历史响应 DTO
     */
    private com.example.biddoc.document.dto.resp.SearchHistoryRespDTO toSearchHistoryRespDTO(
            com.example.biddoc.document.entity.SearchHistoryEntity history) {
        com.example.biddoc.document.dto.resp.SearchHistoryRespDTO dto =
            new com.example.biddoc.document.dto.resp.SearchHistoryRespDTO();
        dto.setId(String.valueOf(history.getId()));
        dto.setKeyword(history.getKeyword());
        dto.setFolderId(history.getFolderId() != null ? String.valueOf(history.getFolderId()) : null);
        dto.setResultCount(history.getResultCount());
        dto.setSearchTime(history.getSearchTime());
        return dto;
    }

    /**
     * 获取当前用户有 view 权限的所有文件夹 ID 列表
     * 用于全局搜索时的权限过滤
     */
    private List<Long> getAccessibleFolderIds(UserContext.UserInfo currentUser) {
        if (currentUser == null) {
            return Collections.emptyList();
        }
        // 查询所有未删除文件夹，后续统一走 FolderPermissionService 做后端权限过滤。
        List<FolderEntity> allFolders = folderMapper.selectList(
            new LambdaQueryWrapper<FolderEntity>()
                .eq(FolderEntity::getDeleted, false)
        );

        // 过滤出有 view 权限的文件夹
        return allFolders.stream()
            .filter(folderPermissionService::canView)
            .map(FolderEntity::getId)
            .collect(Collectors.toList());
    }

    @Override
    public com.example.biddoc.document.dto.resp.StorageStatsRespDTO getStorageStats() {
        UserContext.UserInfo currentUser = UserContext.get();

        List<Long> visibleDocumentIds = getVisibleDocumentIds(currentUser);
        Long totalDocuments = (long) visibleDocumentIds.size();

        Long totalVersions = 0L;
        List<DocumentVersionEntity> allVersions = Collections.emptyList();
        if (!CollectionUtils.isEmpty(visibleDocumentIds)) {
            allVersions = documentVersionMapper.selectList(
                new LambdaQueryWrapper<DocumentVersionEntity>()
                    .in(DocumentVersionEntity::getDocumentId, visibleDocumentIds)
            );
            totalVersions = (long) allVersions.size();
        }
        Long totalSize = allVersions.stream()
            .mapToLong(DocumentVersionEntity::getSize)
            .sum();

        // 统计当前用户的数据
        Long myDocuments = documentMapper.selectCount(
            new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getOwnerUserId, currentUser.getUserId())
        );

        // 查询当前用户的文档ID列表
        List<Long> myDocumentIds = documentMapper.selectList(
            new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getOwnerUserId, currentUser.getUserId())
                .select(DocumentEntity::getId)
        ).stream().map(DocumentEntity::getId).collect(Collectors.toList());

        // 统计当前用户的存储空间
        Long mySize = 0L;
        if (!CollectionUtils.isEmpty(myDocumentIds)) {
            List<DocumentVersionEntity> myVersions = documentVersionMapper.selectList(
                new LambdaQueryWrapper<DocumentVersionEntity>()
                    .in(DocumentVersionEntity::getDocumentId, myDocumentIds)
            );
            mySize = myVersions.stream()
                .mapToLong(DocumentVersionEntity::getSize)
                .sum();
        }

        // 构建响应
        com.example.biddoc.document.dto.resp.StorageStatsRespDTO dto =
            new com.example.biddoc.document.dto.resp.StorageStatsRespDTO();
        dto.setTotalDocuments(totalDocuments);
        dto.setTotalVersions(totalVersions);
        dto.setTotalSize(totalSize);
        dto.setTotalSizeReadable(formatSize(totalSize));
        dto.setMyDocuments(myDocuments);
        dto.setMySize(mySize);
        dto.setMySizeReadable(formatSize(mySize));

        return dto;
    }

    @Override
    public List<com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO> getDocumentTypeStats(Integer limit) {
        limit = limit != null && limit > 0 && limit <= 50 ? limit : 10;

        List<Long> accessibleFolderIds = getAccessibleFolderIds(UserContext.get());
        if (CollectionUtils.isEmpty(accessibleFolderIds)) {
            return Collections.emptyList();
        }

        // 只统计当前用户可见文件夹下的文档，避免泄露无权目录的类型和容量信息。
        List<DocumentEntity> documents = documentMapper.selectList(
            new LambdaQueryWrapper<DocumentEntity>()
                .in(DocumentEntity::getFolderId, accessibleFolderIds)
        );

        // 按 MIME 类型分组统计
        Map<String, List<DocumentEntity>> typeMap = documents.stream()
            .collect(Collectors.groupingBy(doc ->
                doc.getLatestMime() != null ? doc.getLatestMime() : "unknown"
            ));

        // 转换为 DTO 并排序
        return typeMap.entrySet().stream()
            .map(entry -> {
                String mimeType = entry.getKey();
                List<DocumentEntity> docs = entry.getValue();
                Long count = (long) docs.size();
                Long totalSize = docs.stream()
                    .mapToLong(doc -> doc.getLatestSize() != null ? doc.getLatestSize() : 0L)
                    .sum();

                com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO dto =
                    new com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO();
                dto.setMimeType(mimeType);
                dto.setCount(count);
                dto.setTotalSize(totalSize);
                dto.setTotalSizeReadable(formatSize(totalSize));
                return dto;
            })
            .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.example.biddoc.document.dto.resp.PopularDocumentRespDTO> getPopularDocuments(Integer limit, Integer days) {
        limit = limit != null && limit > 0 && limit <= 50 ? limit : 10;
        days = days != null && days > 0 && days <= 90 ? days : 30;

        // 计算起始时间
        java.time.OffsetDateTime startTime = java.time.OffsetDateTime.now().minusDays(days);

        // 查询指定天数内的下载记录
        List<com.example.biddoc.document.entity.DownloadLogEntity> downloadLogs =
            downloadLogMapper.selectList(
                new LambdaQueryWrapper<com.example.biddoc.document.entity.DownloadLogEntity>()
                    .ge(com.example.biddoc.document.entity.DownloadLogEntity::getDownloadTime, startTime)
            );

        // 按文档ID分组统计下载次数
        Map<Long, Long> downloadCountMap = downloadLogs.stream()
            .collect(Collectors.groupingBy(
                com.example.biddoc.document.entity.DownloadLogEntity::getDocumentId,
                Collectors.counting()
            ));

        // 获取下载次数最多的文档ID列表
        List<Long> topDocumentIds = downloadCountMap.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(topDocumentIds)) {
            return Collections.emptyList();
        }

        List<Long> accessibleFolderIds = getAccessibleFolderIds(UserContext.get());
        if (CollectionUtils.isEmpty(accessibleFolderIds)) {
            return Collections.emptyList();
        }

        // 查询文档详情
        List<DocumentEntity> documents = documentMapper.selectList(
            new LambdaQueryWrapper<DocumentEntity>()
                .in(DocumentEntity::getId, topDocumentIds)
                .in(DocumentEntity::getFolderId, accessibleFolderIds)
        );

        // 转换为 DTO（保持下载次数排序）
        return topDocumentIds.stream()
            .map(docId -> {
                DocumentEntity doc = documents.stream()
                    .filter(d -> d.getId().equals(docId))
                    .findFirst()
                    .orElse(null);

                if (doc == null) {
                    return null;
                }

                com.example.biddoc.document.dto.resp.PopularDocumentRespDTO dto =
                    new com.example.biddoc.document.dto.resp.PopularDocumentRespDTO();
                dto.setId(String.valueOf(doc.getId()));
                dto.setName(doc.getName());
                dto.setCurrentVersionNo(doc.getCurrentVersionNo());
                dto.setSize(String.valueOf(doc.getLatestSize()));
                dto.setMimeType(doc.getLatestMime());
                dto.setOwnerUserId(String.valueOf(doc.getOwnerUserId()));
                dto.setDownloadCount(downloadCountMap.get(docId));
                dto.setCreatedAt(doc.getCreatedAt());
                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    private DocumentEntity requireDocumentForLifecycle(Long documentId) {
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private int nextVersionNo(Long documentId) {
        DocumentVersionEntity latest = documentVersionMapper.selectOne(new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .orderByDesc(DocumentVersionEntity::getVersionNo)
                .last("limit 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    private DocumentVersionEntity requireVersion(Long documentId, Integer versionNo) {
        DocumentVersionEntity version = documentVersionMapper.selectOne(new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, versionNo));
        if (version == null || Boolean.TRUE.equals(version.getDeleted())) {
            throw new BusinessException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }
        return version;
    }

    private void updateVersionApprovalStatus(Long documentId, Integer versionNo, String status,
                                             String reason, boolean switchCurrentVersion) {
        DocumentEntity document = requireDocumentForLifecycle(documentId);
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        DocumentVersionEntity version = requireVersion(documentId, versionNo);
        DocumentVersionEntity update = new DocumentVersionEntity();
        update.setId(version.getId());
        update.setApprovalStatus(status);
        update.setRejectedReason(VERSION_REJECTED.equals(status) ? reason : null);
        update.setApprovedAt(VERSION_APPROVED.equals(status) ? OffsetDateTime.now() : null);
        documentVersionMapper.updateById(update);

        if (switchCurrentVersion) {
            DocumentEntity documentUpdate = new DocumentEntity();
            documentUpdate.setId(documentId);
            documentUpdate.setCurrentVersionNo(versionNo);
            documentUpdate.setLatestSize(version.getSize());
            documentUpdate.setLatestMime(version.getMimeType());
            documentMapper.updateById(documentUpdate);
        }

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT_VERSION")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .afterData(Map.of("versionNo", versionNo, "approvalStatus", status))
                .build());
    }

    private void updateDocumentStatus(Long documentId, String status, String reason) {
        DocumentEntity document = requireDocumentForLifecycle(documentId);
        FolderEntity folder = folderMapper.selectById(document.getFolderId());
        AssertUtil.notNull(folder, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(folder);

        DocumentEntity update = new DocumentEntity();
        update.setId(documentId);
        update.setDocumentStatus(status);
        update.setInvalidReason(reason);
        documentMapper.updateById(update);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.DOCUMENT.getCode())
                .bizType("DOCUMENT")
                .bizId(documentId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .afterData(Map.of("documentStatus", status))
                .build());
    }

    /**
     * 格式化文件大小为可读格式
     */
    private String formatSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private PageResponse<DocumentListItemRespDTO> emptyPage(Integer page, Integer size) {
        return new PageResponse<>(
            Collections.emptyList(),
            (long) page,
            (long) size,
            0L,
            0L,
            false
        );
    }

    /**
     * 返回当前用户可见范围内的文档 ID，统计类接口必须复用该范围，避免跨目录泄露数量和容量。
     */
    private List<Long> getVisibleDocumentIds(UserContext.UserInfo currentUser) {
        List<Long> accessibleFolderIds = getAccessibleFolderIds(currentUser);
        if (CollectionUtils.isEmpty(accessibleFolderIds)) {
            return Collections.emptyList();
        }
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>()
                        .select(DocumentEntity::getId)
                        .in(DocumentEntity::getFolderId, accessibleFolderIds)
        ).stream().map(DocumentEntity::getId).collect(Collectors.toList());
    }
}
