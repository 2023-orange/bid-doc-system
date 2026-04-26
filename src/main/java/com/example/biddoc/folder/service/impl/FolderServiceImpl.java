package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int ROOT_LEVEL = 0;
    private static final int MAX_LEVEL = 8;
    private static final int DEFAULT_STATUS = 1;
    private static final int INITIAL_SORT_NO = 1;

    private final FolderMapper folderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FolderCreateReqDTO req) {
        String name = normalizeName(req.getName());
        Long parentId = normalizeParentId(req.getParentId());

        FolderEntity parent = null;
        if (isRoot(parentId)) {
            ensureSuperAdmin();
        } else {
            parent = getExistingFolder(parentId, ErrorCode.FOLDER_PARENT_NOT_FOUND);
        }

        int level = parent == null ? ROOT_LEVEL : parent.getLevel() + 1;
        if (level > MAX_LEVEL) {
            throw new BusinessException(ErrorCode.FOLDER_LEVEL_EXCEEDED);
        }

        ensureNameUnique(parentId, name, null);

        FolderEntity entity = new FolderEntity();
        entity.setId(IdWorker.getId());
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setLevel(level);
        entity.setSortNo(nextSortNo(parentId));
        entity.setOwnerUserId(getCurrentUserId());
        entity.setOwnerDeptId(getCurrentDeptId(parent));
        entity.setInheritPermission(Boolean.TRUE);
        entity.setStatus(DEFAULT_STATUS);
        entity.setRemark(req.getRemark());
        entity.setDeleted(Boolean.FALSE);
        entity.setAncestorIds(buildAncestorIds(parent, entity.getId()));

        folderMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public FolderDetailRespDTO getById(Long id) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        return toDetailResp(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, FolderRenameReqDTO req) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        if (entity.getLevel() != null && entity.getLevel() == ROOT_LEVEL) {
            ensureSuperAdmin();
        }

        String name = normalizeName(req.getName());
        ensureNameUnique(entity.getParentId(), name, entity.getId());

        entity.setName(name);
        folderMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FolderUpdateReqDTO req) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        if (entity.getLevel() != null && entity.getLevel() == ROOT_LEVEL) {
            // 根级目录编辑继续沿用创建、重命名的超级管理员保护规则
            ensureSuperAdmin();
        }

        entity.setRemark(req.getRemark());
        entity.setStatus(req.getStatus());
        entity.setInheritPermission(req.getInheritPermission());
        entity.setSortNo(req.getSortNo());
        folderMapper.updateById(entity);
    }

    @Override
    public List<FolderTreeNodeRespDTO> listChildren(Long parentId) {
        Long normalizedParentId = normalizeParentId(parentId);
        List<FolderEntity> children = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .eq(FolderEntity::getParentId, normalizedParentId)
                        .orderByAsc(FolderEntity::getSortNo)
                        .orderByAsc(FolderEntity::getCreatedAt)
        );
        return buildTreeNodes(children);
    }

    @Override
    public List<FolderTreeNodeRespDTO> listRootTree() {
        return listChildren(ROOT_PARENT_ID);
    }

    private FolderEntity getExistingFolder(Long id, ErrorCode notFoundCode) {
        FolderEntity entity = folderMapper.selectOne(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getId, id)
                        .eq(FolderEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BusinessException(notFoundCode);
        }
        return entity;
    }

    private void ensureNameUnique(Long parentId, String name, Long excludeId) {
        Long count = folderMapper.selectCount(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .eq(FolderEntity::getParentId, parentId)
                        .eq(FolderEntity::getName, name)
                        .ne(excludeId != null, FolderEntity::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATED);
        }
    }

    private int nextSortNo(Long parentId) {
        List<FolderEntity> lastOne = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .select(FolderEntity::getSortNo)
                        .eq(FolderEntity::getDeleted, false)
                        .eq(FolderEntity::getParentId, parentId)
                        .orderByDesc(FolderEntity::getSortNo)
                        .last("limit 1")
        );
        if (lastOne.isEmpty() || lastOne.get(0).getSortNo() == null) {
            return INITIAL_SORT_NO;
        }
        return lastOne.get(0).getSortNo() + 1;
    }

    private String buildAncestorIds(FolderEntity parent, Long currentId) {
        if (currentId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件夹主键生成失败");
        }
        if (parent == null) {
            return String.valueOf(currentId);
        }
        return parent.getAncestorIds() + "," + currentId;
    }

    private List<FolderTreeNodeRespDTO> buildTreeNodes(List<FolderEntity> children) {
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> folderIds = children.stream()
                .map(FolderEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> parentIdsWithChildren = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .select(FolderEntity::getParentId)
                        .eq(FolderEntity::getDeleted, false)
                        .in(FolderEntity::getParentId, folderIds)
        ).stream()
                .map(FolderEntity::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return children.stream()
                .map(entity -> toTreeNodeResp(entity, parentIdsWithChildren.contains(entity.getId())))
                .toList();
    }

    private FolderDetailRespDTO toDetailResp(FolderEntity entity) {
        FolderDetailRespDTO resp = new FolderDetailRespDTO();
        resp.setId(entity.getId());
        resp.setParentId(entity.getParentId());
        resp.setName(entity.getName());
        resp.setAncestorIds(entity.getAncestorIds());
        resp.setLevel(entity.getLevel());
        resp.setSortNo(entity.getSortNo());
        resp.setOwnerDeptId(entity.getOwnerDeptId());
        resp.setOwnerUserId(entity.getOwnerUserId());
        resp.setInheritPermission(entity.getInheritPermission());
        resp.setStatus(entity.getStatus());
        resp.setRemark(entity.getRemark());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setCreatedBy(entity.getCreatedBy());
        resp.setUpdatedAt(entity.getUpdatedAt());
        resp.setUpdatedBy(entity.getUpdatedBy());
        return resp;
    }

    private FolderTreeNodeRespDTO toTreeNodeResp(FolderEntity entity, boolean hasChildren) {
        FolderTreeNodeRespDTO resp = new FolderTreeNodeRespDTO();
        resp.setId(entity.getId());
        resp.setParentId(entity.getParentId());
        resp.setName(entity.getName());
        resp.setLevel(entity.getLevel());
        resp.setSortNo(entity.getSortNo());
        resp.setHasChildren(hasChildren);
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }

    private String normalizeName(String name) {
        String normalized = name == null ? null : name.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "name不能为空");
        }
        return normalized;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private boolean isRoot(Long parentId) {
        return ROOT_PARENT_ID == parentId;
    }

    private void ensureSuperAdmin() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    private Long getCurrentUserId() {
        UserContext.UserInfo user = UserContext.get();
        return user != null ? user.getUserId() : null;
    }

    private Long getCurrentDeptId(FolderEntity parent) {
        UserContext.UserInfo user = UserContext.get();
        if (user != null && user.getDeptId() != null) {
            return user.getDeptId();
        }
        return parent != null ? parent.getOwnerDeptId() : null;
    }
}
