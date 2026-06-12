package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.project.dto.req.ChecklistTemplateItemSaveReqDTO;
import com.example.biddoc.project.dto.req.ChecklistTemplateSaveReqDTO;
import com.example.biddoc.project.entity.ChecklistTemplateEntity;
import com.example.biddoc.project.entity.ChecklistTemplateItemEntity;
import com.example.biddoc.project.mapper.ChecklistTemplateItemMapper;
import com.example.biddoc.project.mapper.ChecklistTemplateMapper;
import com.example.biddoc.project.service.ChecklistTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChecklistTemplateServiceImpl implements ChecklistTemplateService {

    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistTemplateItemMapper templateItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ChecklistTemplateSaveReqDTO req) {
        requireSuperAdmin();
        ChecklistTemplateEntity entity = new ChecklistTemplateEntity();
        entity.setId(IdWorker.getId());
        entity.setTemplateName(req.getTemplateName());
        entity.setProjectType(req.getProjectType());
        entity.setEnabled(req.getEnabled() == null || req.getEnabled());
        entity.setDeleted(Boolean.FALSE);
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, ChecklistTemplateSaveReqDTO req) {
        requireSuperAdmin();
        ChecklistTemplateEntity entity = get(id);
        entity.setTemplateName(StringUtils.hasText(req.getTemplateName()) ? req.getTemplateName() : entity.getTemplateName());
        entity.setProjectType(req.getProjectType());
        entity.setEnabled(req.getEnabled());
        templateMapper.updateById(entity);
    }

    @Override
    public ChecklistTemplateEntity get(Long id) {
        ChecklistTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清单模板不存在");
        }
        return entity;
    }

    @Override
    public PageResponse<ChecklistTemplateEntity> list(String projectType, Integer page, Integer size) {
        requireSuperAdmin();
        Page<ChecklistTemplateEntity> result = templateMapper.selectPage(
                new Page<>(page == null || page < 1 ? 1 : page, size == null || size < 1 ? 20 : size),
                new LambdaQueryWrapper<ChecklistTemplateEntity>()
                        .eq(StringUtils.hasText(projectType), ChecklistTemplateEntity::getProjectType, projectType)
                        .orderByDesc(ChecklistTemplateEntity::getUpdatedAt));
        return PageResponse.of(result);
    }

    @Override
    public void delete(Long id) {
        requireSuperAdmin();
        ChecklistTemplateEntity entity = get(id);
        entity.setDeleted(Boolean.TRUE);
        templateMapper.updateById(entity);
    }

    @Override
    public Long addItem(Long templateId, ChecklistTemplateItemSaveReqDTO req) {
        requireSuperAdmin();
        get(templateId);
        ChecklistTemplateItemEntity item = new ChecklistTemplateItemEntity();
        item.setId(IdWorker.getId());
        item.setTemplateId(templateId);
        item.setItemName(req.getItemName());
        item.setDescription(req.getDescription());
        item.setRequired(req.getRequired() == null || req.getRequired());
        item.setBusinessCategory(req.getBusinessCategory());
        item.setTenderStructureCategory(req.getTenderStructureCategory());
        item.setSuggestedSensitiveLevel(req.getSuggestedSensitiveLevel());
        item.setAllowedSource(req.getAllowedSource());
        item.setAllowedFileTypes(req.getAllowedFileTypes());
        item.setMinCount(req.getMinCount() == null ? 1 : req.getMinCount());
        item.setMaxCount(req.getMaxCount());
        item.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        item.setDeleted(Boolean.FALSE);
        templateItemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateItem(Long templateId, Long itemId, ChecklistTemplateItemSaveReqDTO req) {
        requireSuperAdmin();
        ChecklistTemplateItemEntity item = requireItem(templateId, itemId);
        item.setItemName(StringUtils.hasText(req.getItemName()) ? req.getItemName() : item.getItemName());
        item.setDescription(req.getDescription());
        item.setRequired(req.getRequired());
        item.setBusinessCategory(req.getBusinessCategory());
        item.setTenderStructureCategory(req.getTenderStructureCategory());
        item.setSuggestedSensitiveLevel(req.getSuggestedSensitiveLevel());
        item.setAllowedSource(req.getAllowedSource());
        item.setAllowedFileTypes(req.getAllowedFileTypes());
        item.setMinCount(req.getMinCount());
        item.setMaxCount(req.getMaxCount());
        item.setSortOrder(req.getSortOrder());
        templateItemMapper.updateById(item);
    }

    @Override
    public void deleteItem(Long templateId, Long itemId) {
        requireSuperAdmin();
        ChecklistTemplateItemEntity item = requireItem(templateId, itemId);
        item.setDeleted(Boolean.TRUE);
        templateItemMapper.updateById(item);
    }

    private void requireSuperAdmin() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_MATCH);
        }
    }

    private ChecklistTemplateItemEntity requireItem(Long templateId, Long itemId) {
        ChecklistTemplateItemEntity item = templateItemMapper.selectById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted()) || !templateId.equals(item.getTemplateId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "模板项不存在");
        }
        return item;
    }
}
