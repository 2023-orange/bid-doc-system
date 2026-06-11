package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.folder.dto.req.FolderBatchDeleteReqDTO;
import com.example.biddoc.folder.dto.req.FolderCopyReqDTO;
import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderMoveReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderPermissionRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final FolderGrantMapper folderGrantMapper;
    private final FolderManagerMapper folderManagerMapper;
    private final FolderFavoriteMapper folderFavoriteMapper;
    private final FolderPermissionService folderPermissionService;
    private final AuditService auditService;
    private final DocumentService documentService;

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
            // 非根级创建必须先通过父节点可创建子目录权限校验，避免绕过第一版权限模型。
            folderPermissionService.checkCreateChild(parent);
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

        // 审计：记录目录创建事件，after 包含关键定位字段
        Map<String, Object> createAfter = new HashMap<>();
        createAfter.put("parentId", String.valueOf(entity.getParentId()));
        createAfter.put("name", entity.getName());
        createAfter.put("level", String.valueOf(entity.getLevel()));
        createAfter.put("ownerDeptId", entity.getOwnerDeptId() != null ? String.valueOf(entity.getOwnerDeptId()) : null);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(entity.getId())
                .operationType(AuditOperationTypeEnum.CREATE.getCode())
                .afterData(createAfter)
                .build());

        return entity.getId();
    }

    @Override
    public FolderDetailRespDTO getById(Long id) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkView(entity);
        return toDetailResp(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, FolderRenameReqDTO req) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkRename(entity);

        String oldName = entity.getName();
        String name = normalizeName(req.getName());
        ensureNameUnique(entity.getParentId(), name, entity.getId());

        entity.setName(name);
        folderMapper.updateById(entity);

        // 审计：记录重命名前后的 name，便于追溯
        Map<String, Object> renameBefore = new HashMap<>();
        renameBefore.put("name", oldName);
        Map<String, Object> renameAfter = new HashMap<>();
        renameAfter.put("name", name);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(id)
                .operationType(AuditOperationTypeEnum.RENAME.getCode())
                .beforeData(renameBefore)
                .afterData(renameAfter)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FolderUpdateReqDTO req) {
        FolderEntity entity = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        folderPermissionService.checkEdit(entity);

        // 记录变更前快照，用于审计 before
        Map<String, Object> updateBefore = new HashMap<>();
        updateBefore.put("remark", entity.getRemark());
        updateBefore.put("status", entity.getStatus() != null ? String.valueOf(entity.getStatus()) : null);
        updateBefore.put("inheritPermission", entity.getInheritPermission() != null ? String.valueOf(entity.getInheritPermission()) : null);
        updateBefore.put("sortNo", entity.getSortNo() != null ? String.valueOf(entity.getSortNo()) : null);

        entity.setRemark(req.getRemark());
        entity.setStatus(req.getStatus());
        entity.setInheritPermission(req.getInheritPermission());
        entity.setSortNo(req.getSortNo());
        folderMapper.updateById(entity);

        Map<String, Object> updateAfter = new HashMap<>();
        updateAfter.put("remark", req.getRemark());
        updateAfter.put("status", req.getStatus() != null ? String.valueOf(req.getStatus()) : null);
        updateAfter.put("inheritPermission", req.getInheritPermission() != null ? String.valueOf(req.getInheritPermission()) : null);
        updateAfter.put("sortNo", req.getSortNo() != null ? String.valueOf(req.getSortNo()) : null);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(id)
                .operationType(AuditOperationTypeEnum.UPDATE.getCode())
                .beforeData(updateBefore)
                .afterData(updateAfter)
                .build());
    }

    @Override
    public List<FolderTreeNodeRespDTO> listChildren(Long parentId) {
        Long normalizedParentId = normalizeParentId(parentId);
        if (!isRoot(normalizedParentId)) {
            FolderEntity parent = getExistingFolder(normalizedParentId, ErrorCode.FOLDER_PARENT_NOT_FOUND);
            folderPermissionService.checkView(parent);
        }

        List<FolderEntity> children = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .eq(FolderEntity::getParentId, normalizedParentId)
                        .orderByAsc(FolderEntity::getSortNo)
                        .orderByAsc(FolderEntity::getCreatedAt)
        );
        List<FolderEntity> viewableChildren = folderPermissionService.filterViewableFolders(children);
        return buildTreeNodes(viewableChildren);
    }

    @Override
    public List<FolderTreeNodeRespDTO> listRootTree() {
        return listChildren(ROOT_PARENT_ID);
    }

    @Override
    public FolderPermissionRespDTO getMyPermissions(Long id) {
        FolderEntity folder = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);
        UserContext.UserInfo user = UserContext.get();

        FolderPermissionRespDTO dto = new FolderPermissionRespDTO();
        boolean isSuperAdmin = user != null && user.isSuperAdmin();
        boolean isOwner = user != null && Objects.equals(folder.getOwnerUserId(), user.getUserId());
        boolean isManager = user != null && folderPermissionService.isManagerOfFolder(folder, user.getUserId());

        dto.setCanView(folderPermissionService.canView(folder));
        dto.setCanCreateChild(folderPermissionService.canCreateChild(folder));
        dto.setCanRename(folderPermissionService.canRename(folder));
        dto.setCanEdit(folderPermissionService.canEdit(folder));
        dto.setCanDelete(folderPermissionService.canDelete(folder));
        dto.setCanMove(folderPermissionService.canMove(folder));
        dto.setCanCopy(folderPermissionService.canCopy(folder));
        dto.setCanFavorite(dto.getCanView());
        dto.setIsOwner(isOwner);
        dto.setIsManager(isManager);
        dto.setIsSuperAdmin(isSuperAdmin);
        return dto;
    }

    @Override
    public PageResponse<FolderTreeNodeRespDTO> search(String keyword, Long parentId, Boolean recursive,
                                                      Boolean favoriteOnly, Integer page, Integer size) {
        int pageNo = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 && size <= 100 ? size : 20;
        boolean recursiveSearch = recursive == null || recursive;

        List<FolderEntity> candidates;
        Long normalizedParentId = normalizeParentId(parentId);
        if (parentId != null && !isRoot(normalizedParentId)) {
            FolderEntity parent = getExistingFolder(normalizedParentId, ErrorCode.FOLDER_PARENT_NOT_FOUND);
            folderPermissionService.checkView(parent);
            candidates = recursiveSearch
                    ? folderMapper.selectSubtree(normalizedParentId).stream()
                            .filter(folder -> !Objects.equals(folder.getId(), normalizedParentId))
                            .toList()
                    : folderMapper.selectList(Wrappers.<FolderEntity>lambdaQuery()
                            .eq(FolderEntity::getDeleted, false)
                            .eq(FolderEntity::getParentId, normalizedParentId)
                            .orderByAsc(FolderEntity::getSortNo));
        } else {
            candidates = folderMapper.selectList(Wrappers.<FolderEntity>lambdaQuery()
                    .eq(FolderEntity::getDeleted, false)
                    .orderByDesc(FolderEntity::getCreatedAt));
        }

        List<Long> favoriteFolderIds = Collections.emptyList();
        if (Boolean.TRUE.equals(favoriteOnly)) {
            UserContext.UserInfo user = UserContext.get();
            if (user == null) {
                return new PageResponse<>(Collections.emptyList(), pageNo, pageSize, 0, 0, false);
            }
            favoriteFolderIds = folderFavoriteMapper.selectList(Wrappers.<FolderFavoriteEntity>lambdaQuery()
                            .eq(FolderFavoriteEntity::getUserId, user.getUserId())
                            .eq(FolderFavoriteEntity::getDeleted, false))
                    .stream().map(FolderFavoriteEntity::getFolderId).toList();
            if (favoriteFolderIds.isEmpty()) {
                return new PageResponse<>(Collections.emptyList(), pageNo, pageSize, 0, 0, false);
            }
        }

        List<Long> finalFavoriteFolderIds = favoriteFolderIds;
        List<FolderEntity> filtered = candidates.stream()
                .filter(folderPermissionService::canView)
                .filter(folder -> keyword == null || keyword.isBlank() || folder.getName().contains(keyword.trim()))
                .filter(folder -> !Boolean.TRUE.equals(favoriteOnly) || finalFavoriteFolderIds.contains(folder.getId()))
                .toList();

        long total = filtered.size();
        int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<FolderTreeNodeRespDTO> items = buildTreeNodes(filtered.subList(fromIndex, toIndex));
        long totalPages = total == 0 ? 0 : (long) Math.ceil(total * 1.0 / pageSize);
        return new PageResponse<>(items, pageNo, pageSize, total, totalPages, pageNo < totalPages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 单删走 batchDelete 内部逻辑，但审计 op = DELETE（API 入口决定事件类型）
        doBatchDelete(Collections.singletonList(id), AuditOperationTypeEnum.DELETE.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(FolderBatchDeleteReqDTO req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "request不能为空");
        }
        doBatchDelete(req.getFolderIds(), AuditOperationTypeEnum.BATCH_DELETE.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, FolderMoveReqDTO req) {
        FolderEntity source = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);

        // MVP 不允许 move 到根级（targetParentId 必须 > 0）
        Long targetParentId = req.getTargetParentId();
        if (targetParentId == null || targetParentId == ROOT_PARENT_ID) {
            throw new BusinessException(ErrorCode.FOLDER_MOVE_TARGET_INVALID,
                    "targetParentId 必须为非根级目录");
        }

        FolderEntity target = getExistingFolder(targetParentId, ErrorCode.FOLDER_PARENT_NOT_FOUND);

        // 防自环：目标父不能是源自身
        if (Objects.equals(source.getId(), target.getId())) {
            throw new BusinessException(ErrorCode.FOLDER_CYCLE_NOT_ALLOWED);
        }
        // noop 移动：目标父=当前父；显式报错而非静默成功，便于前端识别
        if (Objects.equals(source.getParentId(), target.getId())) {
            throw new BusinessException(ErrorCode.FOLDER_MOVE_TARGET_INVALID,
                    "目标父与当前父相同，请变更后再提交");
        }
        // 防循环：目标父不能是源的后代（即 target.ancestorIds 不能含 source.id）
        if (containsAncestor(target.getAncestorIds(), source.getId())) {
            throw new BusinessException(ErrorCode.FOLDER_CYCLE_NOT_ALLOWED);
        }

        // 权限：根级源会被 canMove 直接拒绝；非根源走 owner/manager/grant 链
        folderPermissionService.checkMove(source);
        folderPermissionService.checkCreateChild(target);

        // 目标父下不允许同名
        ensureNameUnique(target.getId(), source.getName(), null);

        // 取源子树（含自身，level ASC, sort_no ASC）并计算层级偏移
        List<FolderEntity> subtree = folderMapper.selectSubtree(source.getId());
        int delta = (target.getLevel() + 1) - source.getLevel();
        int subtreeMaxLevel = subtree.stream()
                .mapToInt(FolderEntity::getLevel)
                .max()
                .orElse(source.getLevel());
        if (subtreeMaxLevel + delta > MAX_LEVEL) {
            throw new BusinessException(ErrorCode.FOLDER_LEVEL_EXCEEDED);
        }

        // 保留源的 move 前快照，供审计 beforeData
        Long sourceOldParentId = source.getParentId();
        String sourceOldAncestorIds = source.getAncestorIds();
        int sourceOldLevel = source.getLevel();

        // 计算新 ancestorIds 前缀；用 SUBSTRING 而非 REPLACE，避免 oldPrefix 在后代中段被误替换
        String oldPrefix = source.getAncestorIds();
        String newPrefix = target.getAncestorIds() + "," + source.getId();
        int newSortNo = nextSortNo(target.getId());

        source.setParentId(target.getId());
        source.setLevel(target.getLevel() + 1);
        source.setAncestorIds(newPrefix);
        source.setSortNo(newSortNo);
        folderMapper.updateById(source);

        // 批量更新源后代：ancestorIds 前缀替换 + level 整体偏移
        List<Long> descendantIds = subtree.stream()
                .map(FolderEntity::getId)
                .filter(sid -> !Objects.equals(sid, source.getId()))
                .toList();
        if (!descendantIds.isEmpty()) {
            folderMapper.rewriteSubtreeAncestors(
                    descendantIds, newPrefix, oldPrefix.length(), delta, currentOperator());
        }

        // 审计 MOVE
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("parentId", String.valueOf(sourceOldParentId));
        beforeData.put("ancestorIds", sourceOldAncestorIds);
        beforeData.put("level", String.valueOf(sourceOldLevel));
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("parentId", String.valueOf(target.getId()));
        afterData.put("ancestorIds", newPrefix);
        afterData.put("level", String.valueOf(source.getLevel()));
        Map<String, Object> extraData = new HashMap<>();
        List<String> affectedIds = subtree.stream()
                .map(f -> String.valueOf(f.getId()))
                .toList();
        extraData.put("affectedFolderIds", affectedIds);
        extraData.put("affectedCount", subtree.size());
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(source.getId())
                .operationType(AuditOperationTypeEnum.MOVE.getCode())
                .beforeData(beforeData)
                .afterData(afterData)
                .extraData(extraData)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id, FolderCopyReqDTO req) {
        FolderEntity source = getExistingFolder(id, ErrorCode.FOLDER_NOT_FOUND);

        Long targetParentId = req.getTargetParentId();
        if (targetParentId == null || targetParentId == ROOT_PARENT_ID) {
            throw new BusinessException(ErrorCode.FOLDER_MOVE_TARGET_INVALID,
                    "targetParentId 必须为非根级目录");
        }
        FolderEntity target = getExistingFolder(targetParentId, ErrorCode.FOLDER_PARENT_NOT_FOUND);

        folderPermissionService.checkCopy(source);
        folderPermissionService.checkCreateChild(target);

        // 新根名：req.targetName 优先（非空时使用），空则沿用源名
        String copyName = (req.getTargetName() != null && !req.getTargetName().trim().isEmpty())
                ? normalizeName(req.getTargetName())
                : source.getName();
        ensureNameUnique(target.getId(), copyName, null);

        // 子树（level ASC, sort_no ASC）；保证父先于子，oldId→newId 映射可正常构建
        List<FolderEntity> subtree = folderMapper.selectSubtree(source.getId());
        int sourceLevel = source.getLevel();
        int subtreeMaxLevel = subtree.stream()
                .mapToInt(FolderEntity::getLevel)
                .max()
                .orElse(sourceLevel);
        int newMaxLevel = target.getLevel() + 1 + (subtreeMaxLevel - sourceLevel);
        if (newMaxLevel > MAX_LEVEL) {
            throw new BusinessException(ErrorCode.FOLDER_LEVEL_EXCEEDED);
        }

        // BFS 复制：维护 oldId→newId、oldId→newAncestorIds 两份映射
        Map<Long, Long> oldToNewId = new HashMap<>();
        Map<Long, String> oldIdToNewAncestorIds = new HashMap<>();
        Long currentUserId = getCurrentUserId();
        Long newRootId = null;
        int rootSortNo = nextSortNo(target.getId());

        for (FolderEntity src : subtree) {
            FolderEntity copy = new FolderEntity();
            long newId = IdWorker.getId();
            copy.setId(newId);

            if (Objects.equals(src.getId(), source.getId())) {
                // 新根
                copy.setParentId(target.getId());
                copy.setLevel(target.getLevel() + 1);
                String newAncestorIds = target.getAncestorIds() + "," + newId;
                copy.setAncestorIds(newAncestorIds);
                copy.setSortNo(rootSortNo);
                copy.setName(copyName);
                newRootId = newId;
                oldIdToNewAncestorIds.put(src.getId(), newAncestorIds);
            } else {
                // 后代：父必先于子（subtree 已按 level ASC 排序保证）
                Long newParentId = oldToNewId.get(src.getParentId());
                String newParentAncestors = oldIdToNewAncestorIds.get(src.getParentId());
                if (newParentId == null || newParentAncestors == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "子树复制顺序异常：父未先建 sourceId=" + src.getId());
                }
                copy.setParentId(newParentId);
                copy.setLevel(src.getLevel() + (target.getLevel() + 1 - sourceLevel));
                String newAncestorIds = newParentAncestors + "," + newId;
                copy.setAncestorIds(newAncestorIds);
                copy.setSortNo(src.getSortNo());
                copy.setName(src.getName());
                oldIdToNewAncestorIds.put(src.getId(), newAncestorIds);
            }

            // 新节点 owner = 当前用户；owner 部门继承目标父；目标父无部门则用当前用户部门兜底
            copy.setOwnerUserId(currentUserId);
            copy.setOwnerDeptId(target.getOwnerDeptId() != null
                    ? target.getOwnerDeptId()
                    : getCurrentDeptId(target));
            copy.setInheritPermission(src.getInheritPermission());
            copy.setStatus(src.getStatus());
            copy.setRemark(src.getRemark());
            copy.setDeleted(Boolean.FALSE);

            folderMapper.insert(copy);
            oldToNewId.put(src.getId(), newId);
        }

        // 审计 COPY；不放 oldNewIdMap（大子树场景下太长）
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("name", copyName);
        afterData.put("parentId", String.valueOf(target.getId()));
        afterData.put("rootNewId", String.valueOf(newRootId));
        afterData.put("copiedCount", subtree.size());
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("sourceFolderId", String.valueOf(source.getId()));
        extraData.put("targetParentId", String.valueOf(target.getId()));
        extraData.put("copiedCount", subtree.size());
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(newRootId)
                .operationType(AuditOperationTypeEnum.COPY.getCode())
                .afterData(afterData)
                .extraData(extraData)
                .build());

        return newRootId;
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

        List<FolderEntity> nextLevelChildren = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .in(FolderEntity::getParentId, folderIds)
        );

        Set<Long> parentIdsWithChildren = folderPermissionService.filterViewableFolders(nextLevelChildren).stream()
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

    /**
     * 实际批量删除逻辑：父子去重 → 权限校验 → 子树合并 → 级联软删 → 单一审计事件。
     * operationType 由调用方传入（DELETE 来自 controller 的 /{id}；BATCH_DELETE 来自 /batch），
     * 便于审计区分单删 vs 批删入口。
     */
    private void doBatchDelete(List<Long> folderIds, String operationType) {
        if (folderIds == null || folderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "folderIds不能为空");
        }

        List<Long> distinctIds = folderIds.stream().distinct().toList();
        List<FolderEntity> folders = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .in(FolderEntity::getId, distinctIds)
        );
        // 任一 id 找不到（不存在或已删）→ 整批回滚，避免半成功导致前端状态错乱
        if (folders.size() < distinctIds.size()) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }

        // 父子去重：若某 folder 的 ancestorIds 含集合内其他 id，则它被祖先覆盖，从待删根集合中剔除
        Set<Long> idSet = folders.stream().map(FolderEntity::getId).collect(Collectors.toSet());
        List<FolderEntity> retained = folders.stream()
                .filter(f -> !ancestorsInSet(f.getAncestorIds(), idSet, f.getId()))
                .toList();

        // 对每个保留根做权限校验（根级会被 canDelete 拒绝非 SUPER_ADMIN）
        for (FolderEntity f : retained) {
            folderPermissionService.checkDelete(f);
        }

        // 合并所有保留根的子树（含自身），去重
        LinkedHashSet<Long> allAffectedIds = new LinkedHashSet<>();
        for (FolderEntity f : retained) {
            List<FolderEntity> subtree = folderMapper.selectSubtree(f.getId());
            for (FolderEntity s : subtree) {
                allAffectedIds.add(s.getId());
            }
        }
        List<Long> affectedIdList = new ArrayList<>(allAffectedIds);
        if (affectedIdList.isEmpty()) {
            // 防御性：folders 非空但 subtree 全空理论上不会发生
            return;
        }

        // 批量级联软删：folder 自身 + grant/manager/favorite；audit_operation_log 不动
        String operator = currentOperator();
        folderMapper.batchSoftDeleteByIds(affectedIdList, operator);
        int affectedGrants = folderGrantMapper.batchSoftDeleteByFolderIds(affectedIdList, operator);
        int affectedManagers = folderManagerMapper.batchSoftDeleteByFolderIds(affectedIdList, operator);
        int affectedFavorites = folderFavoriteMapper.batchSoftDeleteByFolderIds(affectedIdList);

        // 级联软删除文档（Goal-1 集成）
        int affectedDocuments = documentService.cascadeSoftDeleteByFolderIds(affectedIdList);

        // 单事件审计：bizId 取第一个保留根，extraData 含全集
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("deletedFolderIds",
                affectedIdList.stream().map(String::valueOf).toList());
        extraData.put("rootDeletedIds",
                retained.stream().map(f -> String.valueOf(f.getId())).toList());
        extraData.put("affectedGrants", affectedGrants);
        extraData.put("affectedManagers", affectedManagers);
        extraData.put("affectedFavorites", affectedFavorites);
        extraData.put("affectedDocuments", affectedDocuments);

        Long bizId = retained.get(0).getId();
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(bizId)
                .operationType(operationType)
                .extraData(extraData)
                .build());
    }

    /**
     * 判断 ancestorIds 字符串中是否包含 idSet 内的某个 id（用于 batchDelete 父子去重）。
     * excludeSelf 用于剔除自身——ancestorIds 本身就包含自己，不算"被祖先覆盖"。
     */
    private boolean ancestorsInSet(String ancestorIds, Set<Long> idSet, Long excludeSelf) {
        if (ancestorIds == null || ancestorIds.isBlank()) {
            return false;
        }
        for (String part : ancestorIds.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                Long ancestorId = Long.valueOf(trimmed);
                if (Objects.equals(ancestorId, excludeSelf)) {
                    continue;
                }
                if (idSet.contains(ancestorId)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 判断 ancestorIds 是否含某个 id（用于 move 防循环：目标父若是源的后代则其 ancestorIds 必含源 id）。
     */
    private boolean containsAncestor(String ancestorIds, Long needleId) {
        if (ancestorIds == null || ancestorIds.isBlank() || needleId == null) {
            return false;
        }
        for (String part : ancestorIds.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                if (Objects.equals(Long.valueOf(trimmed), needleId)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 当前操作者 id 字符串（用于 @Update 注解 SQL 手动填充 updated_by，对齐 MyMetaObjectHandler 行为）。
     */
    private String currentOperator() {
        Long userId = getCurrentUserId();
        return userId != null ? String.valueOf(userId) : "system";
    }
}
