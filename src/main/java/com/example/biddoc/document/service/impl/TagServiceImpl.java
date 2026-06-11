package com.example.biddoc.document.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.dto.resp.TagRespDTO;
import com.example.biddoc.document.entity.DocumentTagEntity;
import com.example.biddoc.document.entity.TagEntity;
import com.example.biddoc.document.mapper.DocumentTagMapper;
import com.example.biddoc.document.mapper.TagMapper;
import com.example.biddoc.document.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;

    @Override
    public List<TagRespDTO> list() {
        return tagMapper.selectList(
                Wrappers.<TagEntity>lambdaQuery()
                        .eq(TagEntity::getDeleted, false)
                        .orderByDesc(TagEntity::getCreatedAt)
        ).stream().map(this::toRespDTO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagRespDTO create(String name) {
        ensureTagManager();
        String normalized = normalizeName(name);
        Long count = tagMapper.selectCount(
                Wrappers.<TagEntity>lambdaQuery()
                        .eq(TagEntity::getDeleted, false)
                        .eq(TagEntity::getName, normalized)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.TAG_NAME_DUPLICATED);
        }
        TagEntity entity = new TagEntity();
        entity.setId(IdWorker.getId());
        entity.setName(normalized);
        entity.setDeleted(Boolean.FALSE);
        tagMapper.insert(entity);
        return toRespDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ensureTagManager();
        TagEntity tag = tagMapper.selectOne(
                Wrappers.<TagEntity>lambdaQuery()
                        .eq(TagEntity::getId, id)
                        .eq(TagEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (tag == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        tagMapper.deleteById(id);
        documentTagMapper.delete(
                Wrappers.<DocumentTagEntity>lambdaQuery()
                        .eq(DocumentTagEntity::getTagId, id)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDocumentTags(Long documentId, List<Long> tagIds) {
        ensureTagManager();
        List<Long> distinctTagIds = tagIds == null ? Collections.emptyList() : tagIds.stream().distinct().toList();
        if (!distinctTagIds.isEmpty()) {
            List<TagEntity> tags = tagMapper.selectBatchIds(distinctTagIds);
            long activeCount = tags.stream().filter(tag -> Boolean.FALSE.equals(tag.getDeleted())).count();
            if (activeCount != distinctTagIds.size()) {
                throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
            }
        }

        // 绑定采用全量替换语义，避免前端需要计算增删差异。
        documentTagMapper.delete(
                Wrappers.<DocumentTagEntity>lambdaQuery()
                        .eq(DocumentTagEntity::getDocumentId, documentId)
        );

        for (Long tagId : distinctTagIds) {
            DocumentTagEntity relation = new DocumentTagEntity();
            relation.setDocumentId(documentId);
            relation.setTagId(tagId);
            relation.setDeleted(Boolean.FALSE);
            documentTagMapper.insert(relation);
        }
    }

    @Override
    public List<TagRespDTO> listDocumentTags(Long documentId) {
        List<DocumentTagEntity> relations = documentTagMapper.selectList(
                Wrappers.<DocumentTagEntity>lambdaQuery()
                        .eq(DocumentTagEntity::getDocumentId, documentId)
                        .eq(DocumentTagEntity::getDeleted, false)
        );
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        List<Long> tagIds = relations.stream().map(DocumentTagEntity::getTagId).distinct().toList();
        return tagMapper.selectBatchIds(tagIds).stream()
                .filter(tag -> Boolean.FALSE.equals(tag.getDeleted()))
                .map(this::toRespDTO)
                .toList();
    }

    private void ensureTagManager() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || !(user.isSuperAdmin() || user.isFolderAdmin())) {
            throw new BusinessException(ErrorCode.ROLE_NOT_MATCH, "仅管理员可维护标签");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? null : name.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "name不能为空");
        }
        if (normalized.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "name长度不能超过64");
        }
        return normalized;
    }

    private TagRespDTO toRespDTO(TagEntity entity) {
        TagRespDTO dto = new TagRespDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
