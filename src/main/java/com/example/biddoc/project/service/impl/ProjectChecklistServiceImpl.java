package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectChecklistServiceImpl implements ProjectChecklistService {

    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistTemplateItemMapper templateItemMapper;
    private final ProjectChecklistItemMapper checklistItemMapper;
    private final ProjectChecklistDocumentMapper checklistDocumentMapper;
    private final ProjectMapper projectMapper;
    private final DocumentMapper documentMapper;
    private final ProjectPermissionService projectPermissionService;

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
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setId(itemId);
        update.setOwnerUserId(ownerUserId);
        update.setDeadline(deadline);
        update.setUpdatedAt(OffsetDateTime.now());
        checklistItemMapper.updateById(update);
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

        ProjectChecklistDocumentEntity binding = new ProjectChecklistDocumentEntity();
        binding.setId(IdWorker.getId());
        binding.setChecklistItemId(itemId);
        binding.setDocumentId(documentId);
        binding.setVersionNo(versionNo != null ? versionNo : document.getCurrentVersionNo());
        binding.setBindType("DOCUMENT");
        binding.setDeleted(Boolean.FALSE);
        checklistDocumentMapper.insert(binding);

        recalculateItemStatus(itemId);
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
        recalculateItemStatus(itemId);
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
            if (document == null || !"APPROVED".equals(document.getDocumentStatus())) {
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
            if (document != null && "REJECTED".equals(document.getDocumentStatus())) {
                return true;
            }
        }
        return false;
    }

    private ProjectChecklistItemEntity requireItem(Long itemId) {
        ProjectChecklistItemEntity item = checklistItemMapper.selectById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单项不存在");
        }
        return item;
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
