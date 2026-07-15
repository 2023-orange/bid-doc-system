package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentUseGrantEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentUseGrantMapper;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.constant.ChecklistItemStatusEnum;
import com.example.biddoc.project.constant.ProjectStatusEnum;
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
import com.example.biddoc.project.service.ProjectChecklistService;
import com.example.biddoc.project.service.ProjectPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectChecklistServiceImpl implements ProjectChecklistService {

    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistTemplateItemMapper templateItemMapper;
    private final ProjectChecklistItemMapper checklistItemMapper;
    private final ProjectChecklistDocumentMapper checklistDocumentMapper;
    private final ProjectMapper projectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentUseGrantMapper documentUseGrantMapper;
    private final SysUserMapper sysUserMapper;
    private final ProjectPermissionService projectPermissionService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFromTemplate(Long projectId, Long templateId) {
        projectPermissionService.checkManage(projectId);
        ensureProjectEditable(projectId);
        ChecklistTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单模板不存在或未启用");
        }
        List<ChecklistTemplateItemEntity> templateItems = templateItemMapper.selectList(
                new LambdaQueryWrapper<ChecklistTemplateItemEntity>()
                        .eq(ChecklistTemplateItemEntity::getTemplateId, templateId)
                        .orderByAsc(ChecklistTemplateItemEntity::getSortOrder));
        for (ChecklistTemplateItemEntity source : templateItems) {
            ProjectChecklistItemEntity item = new ProjectChecklistItemEntity();
            item.setId(IdWorker.getId());
            item.setProjectId(projectId);
            item.setTemplateItemId(source.getId());
            item.setItemName(source.getItemName());
            item.setDescription(source.getDescription());
            item.setRequired(source.getRequired());
            item.setBusinessCategory(source.getBusinessCategory());
            item.setTenderStructureCategory(source.getTenderStructureCategory());
            item.setSensitiveLevel(source.getSuggestedSensitiveLevel());
            item.setAllowedSource(source.getAllowedSource());
            item.setAllowedFileTypes(source.getAllowedFileTypes());
            item.setMinCount(source.getMinCount());
            item.setMaxCount(source.getMaxCount());
            item.setStatus(ChecklistItemStatusEnum.PENDING_COLLECT.getCode());
            item.setSortOrder(source.getSortOrder());
            item.setDeleted(Boolean.FALSE);
            checklistItemMapper.insert(item);
        }
    }

    @Override
    public List<ProjectChecklistItemEntity> listItems(Long projectId) {
        projectPermissionService.checkView(projectId);
        return checklistItemMapper.selectList(new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                .eq(ProjectChecklistItemEntity::getDeleted, false)
                .orderByAsc(ProjectChecklistItemEntity::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOwner(Long projectId, Long itemId, Long ownerUserId, OffsetDateTime deadline) {
        ProjectChecklistItemEntity item = requireItem(itemId);
        if (!projectId.equals(item.getProjectId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不属于当前项目");
        }
        projectPermissionService.checkManage(projectId);
        ensureProjectEditable(projectId);
        validateAssignableUser(ownerUserId, "清单负责人");
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setId(itemId);
        update.setOwnerUserId(ownerUserId);
        update.setDeadline(deadline);
        update.setUpdatedAt(OffsetDateTime.now());
        checklistItemMapper.updateById(update);
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("ownerUserId", ownerUserId);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("CHECKLIST_ITEM")
                .bizId(itemId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .objectName(checklistObjectName(projectId, item))
                .actionSummary(currentActorName() + " 调整了清单项《" + item.getItemName() + "》负责人")
                .relatedBizType("PROJECT")
                .relatedBizId(projectId)
                .afterData(afterData)
                .build());
        if (ownerUserId != null) {
            notificationService.send(ownerUserId, "CHECKLIST_OWNER", "你被指定为清单项责任人",
                    "清单项：" + item.getItemName(), "CHECKLIST_ITEM", itemId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDocument(Long projectId, Long itemId, Long documentId, Integer versionNo) {
        ProjectChecklistItemEntity item = requireItem(itemId);
        if (!projectId.equals(item.getProjectId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不属于当前项目");
        }
        projectPermissionService.checkChecklistMaintain(projectId, item.getOwnerUserId());
        ensureProjectEditable(projectId);
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        if ("VOIDED".equals(document.getDocumentStatus()) || "DELETED".equals(document.getDocumentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "作废或删除资料不能绑定清单");
        }
        if (isExpired(document)) {
            throw new BusinessException(ErrorCode.DOCUMENT_EXPIRED);
        }
        validateChecklistBindingRules(item, document);
        validateSensitiveBindingGrant(document, versionNo != null ? versionNo : document.getCurrentVersionNo());

        ProjectChecklistDocumentEntity binding = new ProjectChecklistDocumentEntity();
        binding.setId(IdWorker.getId());
        binding.setChecklistItemId(itemId);
        binding.setDocumentId(documentId);
        binding.setVersionNo(versionNo != null ? versionNo : document.getCurrentVersionNo());
        binding.setBindType("DOCUMENT");
        binding.setDeleted(Boolean.FALSE);
        checklistDocumentMapper.insert(binding);

        recalculateItemStatus(itemId);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("CHECKLIST_ITEM")
                .bizId(itemId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .objectName(checklistObjectName(projectId, item))
                .actionSummary(currentActorName() + " 绑定资料《" + document.getName() + "》到清单项《" + item.getItemName() + "》")
                .relatedBizType("PROJECT")
                .relatedBizId(projectId)
                .afterData(Map.of("bindDocumentId", documentId, "versionNo", binding.getVersionNo()))
                .build());
        notifyChecklistOwner(item, "CHECKLIST_DOCUMENT_BIND", "清单项已绑定资料");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindDocument(Long projectId, Long itemId, Long documentId) {
        ProjectChecklistItemEntity item = requireItem(itemId);
        if (!projectId.equals(item.getProjectId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不属于当前项目");
        }
        projectPermissionService.checkChecklistMaintain(projectId, item.getOwnerUserId());
        ensureProjectEditable(projectId);
        ProjectChecklistDocumentEntity update = new ProjectChecklistDocumentEntity();
        update.setDeleted(Boolean.TRUE);
        checklistDocumentMapper.update(update, new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                .eq(ProjectChecklistDocumentEntity::getChecklistItemId, itemId)
                .eq(ProjectChecklistDocumentEntity::getDocumentId, documentId)
                .eq(ProjectChecklistDocumentEntity::getDeleted, false));
        DocumentEntity document = documentMapper.selectById(documentId);
        recalculateItemStatus(itemId);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("CHECKLIST_ITEM")
                .bizId(itemId)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .objectName(checklistObjectName(projectId, item))
                .actionSummary(currentActorName() + " 从清单项《" + item.getItemName() + "》解绑资料《"
                        + (document != null ? document.getName() : String.valueOf(documentId)) + "》")
                .relatedBizType("PROJECT")
                .relatedBizId(projectId)
                .afterData(Map.of("unbindDocumentId", documentId))
                .build());
        notifyChecklistOwner(item, "CHECKLIST_DOCUMENT_UNBIND", "清单项已解绑资料");
    }

    @Override
    public void recalculateItemStatus(Long itemId) {
        ProjectChecklistItemEntity item = requireItem(itemId);
        long boundCount = checklistDocumentMapper.selectCount(new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                .eq(ProjectChecklistDocumentEntity::getChecklistItemId, itemId)
                .eq(ProjectChecklistDocumentEntity::getDeleted, false));
        String status;
        if (boundCount <= 0) {
            status = ChecklistItemStatusEnum.PENDING_COLLECT.getCode();
        } else if (hasRejectedDocument(itemId)) {
            status = ChecklistItemStatusEnum.NEED_SUPPLEMENT.getCode();
        } else if (hasOnlyApprovedDocuments(itemId) && boundCount >= minCount(item)) {
            status = ChecklistItemStatusEnum.COMPLETE.getCode();
        } else {
            status = ChecklistItemStatusEnum.PENDING_REVIEW.getCode();
        }
        // 状态计算集中在服务层，避免控制器或审批模块各自散落判断导致清单口径不一致。
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setId(itemId);
        update.setStatus(status);
        update.setUpdatedAt(OffsetDateTime.now());
        checklistItemMapper.updateById(update);
    }

    private boolean hasOnlyApprovedDocuments(Long itemId) {
        List<ProjectChecklistDocumentEntity> bindings = checklistDocumentMapper.selectList(
                new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                        .eq(ProjectChecklistDocumentEntity::getChecklistItemId, itemId)
                        .eq(ProjectChecklistDocumentEntity::getDeleted, false));
        if (bindings == null || bindings.isEmpty()) {
            return false;
        }
        for (ProjectChecklistDocumentEntity binding : bindings) {
            DocumentEntity document = documentMapper.selectById(binding.getDocumentId());
            if (document == null
                    || Boolean.TRUE.equals(document.getDeleted())
                    || !"APPROVED".equals(document.getDocumentStatus())
                    || "VOIDED".equals(document.getDocumentStatus())
                    || isExpired(document)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasRejectedDocument(Long itemId) {
        List<ProjectChecklistDocumentEntity> bindings = checklistDocumentMapper.selectList(
                new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                        .eq(ProjectChecklistDocumentEntity::getChecklistItemId, itemId)
                        .eq(ProjectChecklistDocumentEntity::getDeleted, false));
        if (bindings == null) {
            return false;
        }
        for (ProjectChecklistDocumentEntity binding : bindings) {
            DocumentEntity document = documentMapper.selectById(binding.getDocumentId());
            if (document != null && ("REJECTED".equals(document.getDocumentStatus()) || isExpired(document))) {
                return true;
            }
        }
        return false;
    }

    private boolean isExpired(DocumentEntity document) {
        return Boolean.TRUE.equals(document.getHasExpireDate())
                && document.getExpireDate() != null
                && document.getExpireDate().isBefore(OffsetDateTime.now());
    }

    private void validateChecklistBindingRules(ProjectChecklistItemEntity item, DocumentEntity document) {
        // 清单模板约束必须在绑定前统一执行，避免绕过来源、格式和数量规则造成项目归档口径失真。
        validateAllowedSource(item, document);
        validateAllowedFileTypes(item, document);
        validateMaxCount(item);
    }

    private void validateAllowedSource(ProjectChecklistItemEntity item, DocumentEntity document) {
        List<String> allowedSources = splitRuleValues(item.getAllowedSource()).stream()
                .map(this::normalizeSourceRule)
                .toList();
        if (allowedSources.isEmpty()
                || allowedSources.contains("BOTH")
                || allowedSources.contains("ALL")) {
            return;
        }
        String documentSource = normalizeSourceRule(document.getSourceType());
        if (!allowedSources.contains(documentSource)) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "资料来源不符合清单要求");
        }
    }

    private void validateAllowedFileTypes(ProjectChecklistItemEntity item, DocumentEntity document) {
        List<String> allowedTypes = splitRuleValues(item.getAllowedFileTypes()).stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .toList();
        if (allowedTypes.isEmpty()) {
            return;
        }
        String latestMime = document.getLatestMime() == null
                ? ""
                : document.getLatestMime().trim().toLowerCase(Locale.ROOT);
        if (latestMime.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "资料文件类型不符合清单要求");
        }
        boolean matched = allowedTypes.stream().anyMatch(allowed -> matchesFileType(allowed, latestMime));
        if (!matched) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "资料文件类型不符合清单要求");
        }
    }

    private void validateMaxCount(ProjectChecklistItemEntity item) {
        if (item.getMaxCount() == null || item.getMaxCount() <= 0) {
            return;
        }
        long boundCount = checklistDocumentMapper.selectCount(new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                .eq(ProjectChecklistDocumentEntity::getChecklistItemId, item.getId())
                .eq(ProjectChecklistDocumentEntity::getDeleted, false));
        if (boundCount >= item.getMaxCount()) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "清单项绑定资料数量已达上限");
        }
    }

    private void validateSensitiveBindingGrant(DocumentEntity document, Integer versionNo) {
        String level = document.getSensitiveLevel();
        if (!"SENSITIVE".equalsIgnoreCase(level) && !"SECRET".equalsIgnoreCase(level)) {
            return;
        }
        com.example.biddoc.common.constant.UserContext.UserInfo currentUser =
                com.example.biddoc.common.constant.UserContext.get();
        boolean privileged = currentUser != null
                && (currentUser.isSuperAdmin() || document.getOwnerUserId() != null
                && document.getOwnerUserId().equals(currentUser.getUserId()));
        if (privileged) {
            return;
        }
        if (currentUser == null || versionNo == null || !hasActiveUseGrant(document.getId(), versionNo, currentUser.getUserId())) {
            throw new BusinessException(ErrorCode.DOCUMENT_SENSITIVE_ACCESS_DENIED);
        }
    }

    private boolean hasActiveUseGrant(Long documentId, Integer versionNo, Long granteeId) {
        OffsetDateTime now = OffsetDateTime.now();
        Long count = documentUseGrantMapper.selectCount(new LambdaQueryWrapper<DocumentUseGrantEntity>()
                .eq(DocumentUseGrantEntity::getDocumentId, documentId)
                .eq(DocumentUseGrantEntity::getVersionNo, versionNo)
                .eq(DocumentUseGrantEntity::getGranteeId, granteeId)
                .in(DocumentUseGrantEntity::getStatus, List.of("ACTIVE", "APPROVED"))
                .eq(DocumentUseGrantEntity::getDeleted, false)
                .and(wrapper -> wrapper.isNull(DocumentUseGrantEntity::getValidFrom)
                        .or()
                        .le(DocumentUseGrantEntity::getValidFrom, now))
                .and(wrapper -> wrapper.isNull(DocumentUseGrantEntity::getValidUntil)
                        .or()
                        .ge(DocumentUseGrantEntity::getValidUntil, now)));
        return count != null && count > 0;
    }

    private boolean matchesFileType(String allowed, String latestMime) {
        if (allowed == null || allowed.isBlank()) {
            return false;
        }
        String normalized = allowed.startsWith(".") ? allowed.substring(1) : allowed;
        if (latestMime.equals(normalized)) {
            return true;
        }
        int slashIndex = latestMime.indexOf('/');
        return slashIndex >= 0 && latestMime.substring(slashIndex + 1).equals(normalized);
    }

    private String normalizeSourceRule(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "通用资料库", "COMMON", "COMMON_LIBRARY" -> "COMMON_LIBRARY";
            case "项目上传", "PROJECT", "PROJECT_UPLOAD" -> "PROJECT_UPLOAD";
            case "两者均可", "全部", "BOTH", "ALL" -> "BOTH";
            default -> normalized;
        };
    }

    private List<String> splitRuleValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private void notifyChecklistOwner(ProjectChecklistItemEntity item, String type, String title) {
        if (item.getOwnerUserId() != null) {
            notificationService.send(item.getOwnerUserId(), type, title,
                    "清单项：" + item.getItemName(), "CHECKLIST_ITEM", item.getId());
        }
    }

    private String checklistObjectName(Long projectId, ProjectChecklistItemEntity item) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project != null && project.getProjectName() != null && !project.getProjectName().isBlank()) {
            return project.getProjectName() + " / " + item.getItemName();
        }
        return item.getItemName();
    }

    private String currentActorName() {
        com.example.biddoc.common.constant.UserContext.UserInfo user =
                com.example.biddoc.common.constant.UserContext.get();
        if (user == null) {
            return "系统";
        }
        return user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : String.valueOf(user.getUserId());
    }

    private ProjectChecklistItemEntity requireItem(Long itemId) {
        ProjectChecklistItemEntity item = checklistItemMapper.selectById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不存在");
        }
        return item;
    }

    private void validateAssignableUser(Long userId, String roleName) {
        if (userId == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(userId);
        // 清单负责人后续可维护清单资料，必须拒绝停用或已删除账号，防止任务落到不可处理用户。
        if (user == null || Boolean.TRUE.equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, roleName + "账号不可用");
        }
    }

    private void ensureProjectEditable(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project != null && ProjectStatusEnum.ARCHIVED.getCode().equals(project.getProjectStatus())) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED_READONLY);
        }
    }

    private int minCount(ProjectChecklistItemEntity item) {
        return item.getMinCount() == null || item.getMinCount() < 1 ? 1 : item.getMinCount();
    }
}
