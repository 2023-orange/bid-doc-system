package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.dto.resp.TagRespDTO;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DownloadLogEntity;
import com.example.biddoc.document.entity.TagEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DownloadLogMapper;
import com.example.biddoc.document.mapper.TagMapper;
import com.example.biddoc.folder.constant.FolderGrantScopeEnum;
import com.example.biddoc.folder.constant.FolderPermissionCodeEnum;
import com.example.biddoc.folder.dto.req.FolderAccessRecordReqDTO;
import com.example.biddoc.folder.dto.req.FolderTagBindReqDTO;
import com.example.biddoc.folder.dto.resp.FolderActionResultRespDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderPermissionRespDTO;
import com.example.biddoc.folder.dto.resp.FolderShortcutRespDTO;
import com.example.biddoc.folder.dto.resp.FolderStatsRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.folder.entity.FolderAccessLogEntity;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import com.example.biddoc.folder.entity.FolderGrantEntity;
import com.example.biddoc.folder.entity.FolderTagEntity;
import com.example.biddoc.folder.mapper.FolderAccessLogMapper;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.mapper.FolderTagMapper;
import com.example.biddoc.folder.service.FolderInsightService;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderInsightServiceImpl implements FolderInsightService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final long STORAGE_QUOTA = 100L * 1024 * 1024 * 1024;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final FolderMapper folderMapper;
    private final DocumentMapper documentMapper;
    private final DownloadLogMapper downloadLogMapper;
    private final FolderFavoriteMapper folderFavoriteMapper;
    private final FolderManagerMapper folderManagerMapper;
    private final FolderGrantMapper folderGrantMapper;
    private final FolderAccessLogMapper folderAccessLogMapper;
    private final FolderTagMapper folderTagMapper;
    private final TagMapper tagMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final ProjectMapper projectMapper;
    private final FolderPermissionService folderPermissionService;

    @Override
    public FolderStatsRespDTO getStats() {
        UserContext.UserInfo user = UserContext.get();
        List<FolderEntity> allFolders = listActiveFolders();
        List<FolderEntity> visibleFolders = user != null && user.isSuperAdmin()
                ? allFolders
                : folderPermissionService.filterViewableFolders(allFolders);
        Set<Long> visibleFolderIds = visibleFolders.stream().map(FolderEntity::getId).collect(Collectors.toSet());

        List<DocumentEntity> visibleDocuments = listDocumentsInFolders(visibleFolderIds);
        long totalSize = visibleDocuments.stream()
                .mapToLong(document -> document.getLatestSize() == null ? 0L : document.getLatestSize())
                .sum();

        FolderStatsRespDTO dto = new FolderStatsRespDTO();
        dto.setTotalFolders((long) visibleFolders.size());
        dto.setTotalDocuments((long) visibleDocuments.size());
        dto.setTotalSize(totalSize);
        dto.setActiveFolders(visibleFolders.stream()
                .filter(folder -> Integer.valueOf(1).equals(folder.getStatus()))
                .count());
        dto.setFavoriteFolders(countFavorites(user, visibleFolderIds));
        dto.setManagedFolders(countManaged(user, visibleFolders));
        dto.setOwnedFolders(countOwned(user, visibleFolders));
        dto.setSharedWithMeFolders(countSharedWithMe(user, visibleFolders));
        dto.setRecentAccessCount(countRecentAccess(user, visibleFolderIds));
        dto.setStorageQuota(STORAGE_QUOTA);
        dto.setStorageUsageRate(STORAGE_QUOTA == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalSize)
                .divide(BigDecimal.valueOf(STORAGE_QUOTA), 4, RoundingMode.HALF_UP));
        return dto;
    }

    @Override
    public FolderDetailRespDTO getDetail(Long id) {
        FolderEntity folder = getExistingFolder(id);
        folderPermissionService.checkView(folder);

        FolderDetailRespDTO dto = toBaseDetail(folder);
        dto.setFullPath(buildFullPath(folder));
        dto.setParentName(resolveParentName(folder));
        dto.setPermissions(buildPermissions(folder));
        fillOwnerAndOperatorNames(dto);
        fillFolderStats(dto, folder);
        fillAccessStats(dto, folder);
        dto.setIsFavorite(isFavorite(folder.getId()));
        dto.setRelatedProjects(listRelatedProjects(folder.getId()));
        dto.setTags(listTagsInternal(folder.getId()));
        return dto;
    }

    @Override
    public List<FolderShortcutRespDTO> listRecent(Integer limit) {
        UserContext.UserInfo user = requireCurrentUser();
        int normalizedLimit = normalizeLimit(limit);
        List<FolderAccessLogEntity> logs = folderAccessLogMapper.selectList(
                Wrappers.<FolderAccessLogEntity>lambdaQuery()
                        .eq(FolderAccessLogEntity::getDeleted, false)
                        .eq(FolderAccessLogEntity::getUserId, user.getUserId())
                        .orderByDesc(FolderAccessLogEntity::getAccessTime)
                        .last("limit " + normalizedLimit * 5)
        );

        LinkedHashMap<Long, OffsetDateTime> lastAccessByFolder = new LinkedHashMap<>();
        for (FolderAccessLogEntity log : logs) {
            lastAccessByFolder.putIfAbsent(log.getFolderId(), log.getAccessTime());
            if (lastAccessByFolder.size() >= normalizedLimit) {
                break;
            }
        }
        return buildShortcuts(loadFoldersInOrder(lastAccessByFolder.keySet()), Map.of(), lastAccessByFolder, Map.of());
    }

    @Override
    public List<FolderShortcutRespDTO> listFavorites(Integer limit) {
        UserContext.UserInfo user = requireCurrentUser();
        int normalizedLimit = normalizeLimit(limit);
        List<FolderFavoriteEntity> favorites = folderFavoriteMapper.selectList(
                Wrappers.<FolderFavoriteEntity>lambdaQuery()
                        .eq(FolderFavoriteEntity::getDeleted, false)
                        .eq(FolderFavoriteEntity::getUserId, user.getUserId())
                        .orderByDesc(FolderFavoriteEntity::getCreatedAt)
                        .last("limit " + normalizedLimit)
        );
        LinkedHashSet<Long> folderIds = favorites.stream()
                .map(FolderFavoriteEntity::getFolderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> favoriteIds = favorites.stream()
                .collect(Collectors.toMap(FolderFavoriteEntity::getFolderId, FolderFavoriteEntity::getId, (left, right) -> left));
        Map<Long, FavoriteMeta> favoriteMeta = favorites.stream()
                .collect(Collectors.toMap(FolderFavoriteEntity::getFolderId,
                        favorite -> new FavoriteMeta(favorite.getCreatedAt(), favorite.getCreatedBy()),
                        (left, right) -> left));
        return buildShortcuts(loadFoldersInOrder(folderIds), favoriteIds, Map.of(), favoriteMeta);
    }

    @Override
    public List<FolderShortcutRespDTO> listManaged(Integer limit) {
        requireCurrentUser();
        int normalizedLimit = normalizeLimit(limit);
        List<FolderEntity> managed = listActiveFolders().stream()
                .filter(folder -> folderPermissionService.canView(folder))
                .filter(folder -> {
                    UserContext.UserInfo user = UserContext.get();
                    return user != null && folderPermissionService.isManagerOfFolder(folder, user.getUserId());
                })
                .sorted((left, right) -> nullSafeTime(right.getUpdatedAt()).compareTo(nullSafeTime(left.getUpdatedAt())))
                .limit(normalizedLimit)
                .toList();
        return buildShortcuts(managed, Map.of(), Map.of(), Map.of());
    }

    @Override
    public List<FolderShortcutRespDTO> listOwned(Integer limit) {
        UserContext.UserInfo user = requireCurrentUser();
        int normalizedLimit = normalizeLimit(limit);
        List<FolderEntity> owned = folderMapper.selectList(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getDeleted, false)
                        .eq(FolderEntity::getOwnerUserId, user.getUserId())
                        .orderByDesc(FolderEntity::getCreatedAt)
                        .last("limit " + normalizedLimit)
        ).stream().filter(folderPermissionService::canView).toList();
        return buildShortcuts(owned, Map.of(), Map.of(), Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAccess(Long id, FolderAccessRecordReqDTO req) {
        FolderEntity folder = getExistingFolder(id);
        String accessType = req == null ? null : req.getAccessType();
        if (!StringUtils.hasText(accessType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "accessType不能为空");
        }
        if ("EDIT".equals(accessType)) {
            folderPermissionService.checkEdit(folder);
        } else if ("VIEW".equals(accessType) || "DOWNLOAD".equals(accessType)) {
            folderPermissionService.checkView(folder);
        } else {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "accessType只能是VIEW、DOWNLOAD或EDIT");
        }

        FolderAccessLogEntity log = new FolderAccessLogEntity();
        log.setFolderId(id);
        log.setUserId(requireCurrentUser().getUserId());
        log.setAccessType(accessType);
        log.setAccessTime(OffsetDateTime.now());
        log.setDeleted(Boolean.FALSE);
        folderAccessLogMapper.insert(log);
    }

    @Override
    public List<TagRespDTO> listTags(Long id) {
        FolderEntity folder = getExistingFolder(id);
        folderPermissionService.checkView(folder);
        return listTagsInternal(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTags(Long id, FolderTagBindReqDTO req) {
        FolderEntity folder = getExistingFolder(id);
        folderPermissionService.checkEdit(folder);
        List<Long> tagIds = req == null || req.getTagIds() == null
                ? Collections.emptyList()
                : req.getTagIds().stream().filter(Objects::nonNull).distinct().toList();

        if (!tagIds.isEmpty()) {
            List<TagEntity> tags = tagMapper.selectBatchIds(tagIds);
            long activeCount = tags == null
                    ? 0
                    : tags.stream().filter(tag -> Boolean.FALSE.equals(tag.getDeleted())).count();
            if (activeCount != tagIds.size()) {
                throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
            }
        }

        // 文件夹标签采用全量替换，避免前端在并发编辑时自行计算增删差异。
        folderTagMapper.delete(Wrappers.<FolderTagEntity>lambdaQuery()
                .eq(FolderTagEntity::getFolderId, id));
        for (Long tagId : tagIds) {
            FolderTagEntity relation = new FolderTagEntity();
            relation.setFolderId(id);
            relation.setTagId(tagId);
            relation.setDeleted(Boolean.FALSE);
            folderTagMapper.insert(relation);
        }
    }

    @Override
    public FolderPermissionRespDTO buildPermissions(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        boolean isSuperAdmin = user != null && user.isSuperAdmin();
        boolean isOwner = user != null && Objects.equals(folder.getOwnerUserId(), user.getUserId());
        boolean isManager = user != null && folderPermissionService.isManagerOfFolder(folder, user.getUserId());
        boolean canManage = isSuperAdmin || isOwner || isManager;
        boolean isRoot = folder.getLevel() != null && folder.getLevel() == 0;

        FolderPermissionRespDTO dto = new FolderPermissionRespDTO();
        dto.setCanView(folderPermissionService.canView(folder));
        dto.setCanCreateChild(folderPermissionService.canCreateChild(folder));
        dto.setCanRename(folderPermissionService.canRename(folder));
        dto.setCanEdit(folderPermissionService.canEdit(folder));
        dto.setCanDelete(folderPermissionService.canDelete(folder));
        dto.setCanMove(folderPermissionService.canMove(folder));
        dto.setCanCopy(folderPermissionService.canCopy(folder));
        dto.setCanGrant(!isRoot && canManage);
        dto.setCanManage(canManage);
        dto.setCanFavorite(dto.getCanView());
        dto.setIsOwner(isOwner);
        dto.setIsManager(isManager);
        dto.setIsSuperAdmin(isSuperAdmin);
        return dto;
    }

    @Override
    public FolderActionResultRespDTO toActionResult(Long folderId) {
        FolderEntity folder = getExistingFolder(folderId);
        FolderActionResultRespDTO dto = new FolderActionResultRespDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setParentId(folder.getParentId());
        dto.setFullPath(buildFullPath(folder));
        return dto;
    }

    @Override
    public void fillTreeNodeExtras(List<FolderTreeNodeRespDTO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        List<Long> folderIds = nodes.stream().map(FolderTreeNodeRespDTO::getId).filter(Objects::nonNull).toList();
        Map<Long, List<DocumentEntity>> docsByFolder = groupDocumentsByFolder(folderIds);
        Map<Long, OffsetDateTime> lastAccessByFolder = loadCurrentUserLastAccess(folderIds);
        for (FolderTreeNodeRespDTO node : nodes) {
            List<DocumentEntity> docs = docsByFolder.getOrDefault(node.getId(), List.of());
            node.setDocumentCount((long) docs.size());
            node.setTotalSize(docs.stream()
                    .mapToLong(document -> document.getLatestSize() == null ? 0L : document.getLatestSize())
                    .sum());
            node.setLastAccessTime(lastAccessByFolder.get(node.getId()));
        }
    }

    private FolderDetailRespDTO toBaseDetail(FolderEntity entity) {
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

    private void fillFolderStats(FolderDetailRespDTO dto, FolderEntity folder) {
        List<FolderEntity> subtree = folderMapper.selectSubtree(folder.getId());
        // 详情统计也必须做后端权限过滤，避免通过数量或容量反推出不可见子目录。
        List<FolderEntity> visibleSubtree = subtree.stream()
                .filter(item -> Objects.equals(item.getId(), folder.getId()) || folderPermissionService.canView(item))
                .toList();
        List<Long> subtreeIds = visibleSubtree.stream().map(FolderEntity::getId).toList();
        Set<Long> descendantIds = visibleSubtree.stream()
                .map(FolderEntity::getId)
                .filter(id -> !Objects.equals(id, folder.getId()))
                .collect(Collectors.toSet());
        List<DocumentEntity> directDocs = listDocumentsInFolders(Set.of(folder.getId()));
        List<DocumentEntity> subtreeDocs = listDocumentsInFolders(new LinkedHashSet<>(subtreeIds));

        dto.setDocumentCount((long) directDocs.size());
        dto.setTotalDocumentCount((long) subtreeDocs.size());
        dto.setFolderSize(sumDocumentSize(directDocs));
        dto.setTotalSize(sumDocumentSize(subtreeDocs));
        dto.setChildFolderCount(visibleSubtree.stream()
                .filter(item -> Objects.equals(item.getParentId(), folder.getId()))
                .count());
        dto.setTotalChildFolderCount((long) descendantIds.size());
        dto.setDownloadCount(countDownloads(subtreeDocs));
    }

    private void fillAccessStats(FolderDetailRespDTO dto, FolderEntity folder) {
        dto.setViewCount(folderAccessLogMapper.selectCount(Wrappers.<FolderAccessLogEntity>lambdaQuery()
                .eq(FolderAccessLogEntity::getDeleted, false)
                .eq(FolderAccessLogEntity::getFolderId, folder.getId())
                .eq(FolderAccessLogEntity::getAccessType, "VIEW")));
        FolderAccessLogEntity last = folderAccessLogMapper.selectOne(
                Wrappers.<FolderAccessLogEntity>lambdaQuery()
                        .eq(FolderAccessLogEntity::getDeleted, false)
                        .eq(FolderAccessLogEntity::getFolderId, folder.getId())
                        .orderByDesc(FolderAccessLogEntity::getAccessTime)
                        .last("limit 1")
        );
        if (last != null) {
            dto.setLastAccessTime(last.getAccessTime());
            dto.setLastAccessUserName(resolveUserName(last.getUserId(), Map.of()));
        }
    }

    private void fillOwnerAndOperatorNames(FolderDetailRespDTO dto) {
        Set<Long> userIds = new LinkedHashSet<>();
        addIfNotNull(userIds, dto.getOwnerUserId());
        parseLong(dto.getCreatedBy()).ifPresent(userIds::add);
        parseLong(dto.getUpdatedBy()).ifPresent(userIds::add);
        Map<Long, SysUser> users = loadUsers(userIds);
        dto.setOwnerUserName(resolveUserName(dto.getOwnerUserId(), users));
        parseLong(dto.getCreatedBy()).ifPresent(id -> dto.setCreatedByName(resolveUserName(id, users)));
        parseLong(dto.getUpdatedBy()).ifPresent(id -> dto.setUpdatedByName(resolveUserName(id, users)));

        if (dto.getOwnerDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(dto.getOwnerDeptId());
            if (dept != null && Boolean.FALSE.equals(dept.getDeleted())) {
                dto.setOwnerDeptName(dept.getName());
            }
        }
    }

    private List<FolderDetailRespDTO.RelatedProjectRespDTO> listRelatedProjects(Long folderId) {
        return projectMapper.selectList(Wrappers.<ProjectEntity>lambdaQuery()
                        .eq(ProjectEntity::getDeleted, false)
                        .eq(ProjectEntity::getFolderId, folderId)
                        .orderByDesc(ProjectEntity::getUpdatedAt))
                .stream()
                .map(project -> {
                    FolderDetailRespDTO.RelatedProjectRespDTO dto = new FolderDetailRespDTO.RelatedProjectRespDTO();
                    dto.setProjectId(project.getId());
                    dto.setProjectName(project.getProjectName());
                    dto.setProjectNo(project.getProjectNo());
                    return dto;
                }).toList();
    }

    private List<TagRespDTO> listTagsInternal(Long folderId) {
        List<FolderTagEntity> relations = folderTagMapper.selectList(Wrappers.<FolderTagEntity>lambdaQuery()
                .eq(FolderTagEntity::getDeleted, false)
                .eq(FolderTagEntity::getFolderId, folderId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = relations.stream().map(FolderTagEntity::getTagId).distinct().toList();
        List<TagEntity> tags = tagMapper.selectBatchIds(tagIds);
        if (CollectionUtils.isEmpty(tags)) {
            return List.of();
        }
        return tags.stream()
                .filter(tag -> Boolean.FALSE.equals(tag.getDeleted()))
                .map(this::toTagResp)
                .toList();
    }

    private List<FolderShortcutRespDTO> buildShortcuts(List<FolderEntity> folders,
                                                       Map<Long, Long> favoriteIds,
                                                       Map<Long, OffsetDateTime> lastAccessByFolder,
                                                       Map<Long, FavoriteMeta> favoriteMetaByFolder) {
        if (folders.isEmpty()) {
            return List.of();
        }
        List<FolderEntity> viewable = folders.stream().filter(folderPermissionService::canView).toList();
        List<Long> folderIds = viewable.stream().map(FolderEntity::getId).toList();
        Map<Long, Long> docCounts = groupDocumentsByFolder(folderIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));
        return viewable.stream().map(folder -> {
            FolderShortcutRespDTO dto = new FolderShortcutRespDTO();
            dto.setId(folder.getId());
            dto.setName(folder.getName());
            dto.setParentId(folder.getParentId());
            dto.setFullPath(buildFullPath(folder));
            dto.setLevel(folder.getLevel());
            dto.setSortNo(folder.getSortNo());
            dto.setDocumentCount(docCounts.getOrDefault(folder.getId(), 0L));
            dto.setLastAccessTime(lastAccessByFolder.get(folder.getId()));
            dto.setFavoriteId(favoriteIds.get(folder.getId()));
            dto.setFolderId(folder.getId());
            dto.setFolderName(folder.getName());
            FavoriteMeta favoriteMeta = favoriteMetaByFolder.get(folder.getId());
            dto.setCreatedAt(favoriteMeta != null ? favoriteMeta.createdAt() : folder.getCreatedAt());
            dto.setCreatedBy(favoriteMeta != null ? favoriteMeta.createdBy() : folder.getCreatedBy());
            return dto;
        }).toList();
    }

    private String buildFullPath(FolderEntity folder) {
        List<Long> ancestorIds = parseAncestorIds(folder);
        if (ancestorIds.isEmpty()) {
            return "/" + folder.getName();
        }
        Map<Long, FolderEntity> folderMap = loadFolderMap(ancestorIds);
        List<String> visibleNames = new ArrayList<>();
        boolean hiddenPrefix = false;
        for (Long ancestorId : ancestorIds) {
            FolderEntity ancestor = folderMap.get(ancestorId);
            if (ancestor == null) {
                continue;
            }
            if (folderPermissionService.canView(ancestor)) {
                visibleNames.add(ancestor.getName());
            } else {
                hiddenPrefix = true;
            }
        }
        if (visibleNames.isEmpty()) {
            visibleNames.add(folder.getName());
        }
        String joined = String.join("/", visibleNames);
        return hiddenPrefix ? "/.../" + joined : "/" + joined;
    }

    private String resolveParentName(FolderEntity folder) {
        if (folder.getParentId() == null || Objects.equals(folder.getParentId(), ROOT_PARENT_ID)) {
            return null;
        }
        FolderEntity parent = folderMapper.selectById(folder.getParentId());
        return parent != null && Boolean.FALSE.equals(parent.getDeleted()) && folderPermissionService.canView(parent)
                ? parent.getName()
                : null;
    }

    private FolderEntity getExistingFolder(Long id) {
        FolderEntity entity = folderMapper.selectOne(Wrappers.<FolderEntity>lambdaQuery()
                .eq(FolderEntity::getDeleted, false)
                .eq(FolderEntity::getId, id)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return entity;
    }

    private List<FolderEntity> listActiveFolders() {
        return folderMapper.selectList(Wrappers.<FolderEntity>lambdaQuery()
                .eq(FolderEntity::getDeleted, false)
                .orderByAsc(FolderEntity::getLevel)
                .orderByAsc(FolderEntity::getSortNo));
    }

    private List<DocumentEntity> listDocumentsInFolders(Set<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) {
            return List.of();
        }
        return documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getDeleted, false)
                .in(DocumentEntity::getFolderId, folderIds));
    }

    private Map<Long, List<DocumentEntity>> groupDocumentsByFolder(List<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) {
            return Map.of();
        }
        return documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery()
                        .eq(DocumentEntity::getDeleted, false)
                        .in(DocumentEntity::getFolderId, folderIds))
                .stream()
                .collect(Collectors.groupingBy(DocumentEntity::getFolderId));
    }

    private Map<Long, OffsetDateTime> loadCurrentUserLastAccess(List<Long> folderIds) {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || folderIds == null || folderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, OffsetDateTime> result = new LinkedHashMap<>();
        List<FolderAccessLogEntity> logs = folderAccessLogMapper.selectList(Wrappers.<FolderAccessLogEntity>lambdaQuery()
                .eq(FolderAccessLogEntity::getDeleted, false)
                .eq(FolderAccessLogEntity::getUserId, user.getUserId())
                .in(FolderAccessLogEntity::getFolderId, folderIds)
                .orderByDesc(FolderAccessLogEntity::getAccessTime));
        for (FolderAccessLogEntity log : logs) {
            result.putIfAbsent(log.getFolderId(), log.getAccessTime());
        }
        return result;
    }

    private List<FolderEntity> loadFoldersInOrder(Set<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) {
            return List.of();
        }
        Map<Long, FolderEntity> folderMap = loadFolderMap(folderIds);
        return folderIds.stream().map(folderMap::get).filter(Objects::nonNull).toList();
    }

    private Map<Long, FolderEntity> loadFolderMap(Iterable<Long> folderIds) {
        List<Long> ids = new ArrayList<>();
        folderIds.forEach(id -> {
            if (id != null) {
                ids.add(id);
            }
        });
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<FolderEntity> folders = folderMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(folders)) {
            return Map.of();
        }
        return folders.stream()
                .filter(folder -> Boolean.FALSE.equals(folder.getDeleted()))
                .collect(Collectors.toMap(FolderEntity::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, SysUser> loadUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> Boolean.FALSE.equals(user.getDeleted()))
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
    }

    private String resolveUserName(Long userId, Map<Long, SysUser> knownUsers) {
        if (userId == null) {
            return null;
        }
        SysUser user = knownUsers.get(userId);
        if (user == null) {
            user = sysUserMapper.selectById(userId);
        }
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private long sumDocumentSize(List<DocumentEntity> documents) {
        return documents.stream().mapToLong(document -> document.getLatestSize() == null ? 0L : document.getLatestSize()).sum();
    }

    private long countDownloads(List<DocumentEntity> documents) {
        List<Long> documentIds = documents.stream().map(DocumentEntity::getId).filter(Objects::nonNull).toList();
        if (documentIds.isEmpty()) {
            return 0L;
        }
        return downloadLogMapper.selectCount(Wrappers.<DownloadLogEntity>lambdaQuery()
                .eq(DownloadLogEntity::getDeleted, false)
                .in(DownloadLogEntity::getDocumentId, documentIds));
    }

    private long countFavorites(UserContext.UserInfo user, Set<Long> visibleFolderIds) {
        if (user == null || visibleFolderIds.isEmpty()) {
            return 0L;
        }
        return folderFavoriteMapper.selectList(Wrappers.<FolderFavoriteEntity>lambdaQuery()
                        .eq(FolderFavoriteEntity::getDeleted, false)
                        .eq(FolderFavoriteEntity::getUserId, user.getUserId()))
                .stream()
                .filter(favorite -> visibleFolderIds.contains(favorite.getFolderId()))
                .map(FolderFavoriteEntity::getFolderId)
                .distinct()
                .count();
    }

    private long countManaged(UserContext.UserInfo user, List<FolderEntity> visibleFolders) {
        if (user == null) {
            return 0L;
        }
        return visibleFolders.stream()
                .filter(folder -> folderPermissionService.isManagerOfFolder(folder, user.getUserId()))
                .count();
    }

    private long countOwned(UserContext.UserInfo user, List<FolderEntity> visibleFolders) {
        if (user == null) {
            return 0L;
        }
        return visibleFolders.stream().filter(folder -> Objects.equals(folder.getOwnerUserId(), user.getUserId())).count();
    }

    private long countSharedWithMe(UserContext.UserInfo user, List<FolderEntity> visibleFolders) {
        if (user == null) {
            return 0L;
        }
        List<FolderGrantEntity> grants = folderGrantMapper.selectList(Wrappers.<FolderGrantEntity>lambdaQuery()
                .eq(FolderGrantEntity::getDeleted, false));
        return visibleFolders.stream()
                .filter(folder -> !Objects.equals(folder.getOwnerUserId(), user.getUserId()))
                .filter(folder -> hasExplicitViewGrant(folder, user, grants))
                .count();
    }

    private long countRecentAccess(UserContext.UserInfo user, Set<Long> visibleFolderIds) {
        if (user == null || visibleFolderIds.isEmpty()) {
            return 0L;
        }
        OffsetDateTime start = OffsetDateTime.now().minusDays(7);
        return folderAccessLogMapper.selectList(Wrappers.<FolderAccessLogEntity>lambdaQuery()
                        .eq(FolderAccessLogEntity::getDeleted, false)
                        .eq(FolderAccessLogEntity::getUserId, user.getUserId())
                        .ge(FolderAccessLogEntity::getAccessTime, start))
                .stream()
                .map(FolderAccessLogEntity::getFolderId)
                .filter(visibleFolderIds::contains)
                .distinct()
                .count();
    }

    private boolean hasExplicitViewGrant(FolderEntity folder, UserContext.UserInfo user, List<FolderGrantEntity> grants) {
        List<Long> ancestors = parseAncestorIds(folder);
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> viewCodes = Set.of(
                FolderPermissionCodeEnum.FOLDER_VIEW.getCode(),
                FolderPermissionCodeEnum.FOLDER_EDIT.getCode(),
                FolderPermissionCodeEnum.FOLDER_RENAME.getCode()
        );
        for (FolderGrantEntity grant : grants) {
            if (!viewCodes.contains(grant.getPermissionCode()) || !matchesGrantSubject(grant, user) || !isEffective(grant, now)) {
                continue;
            }
            if (Objects.equals(grant.getFolderId(), folder.getId())) {
                return true;
            }
            if (ancestors.contains(grant.getFolderId())
                    && FolderGrantScopeEnum.SELF_AND_DESCENDANTS.getCode().equals(grant.getGrantScope())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGrantSubject(FolderGrantEntity grant, UserContext.UserInfo user) {
        if (grant.getSubjectType() == null || grant.getSubjectId() == null || user == null) {
            return false;
        }
        return switch (grant.getSubjectType()) {
            case "USER" -> Objects.equals(String.valueOf(user.getUserId()), grant.getSubjectId());
            case "DEPT" -> user.getDeptId() != null && Objects.equals(String.valueOf(user.getDeptId()), grant.getSubjectId());
            case "ROLE" -> user.getRoleCodes() != null && user.getRoleCodes().contains(grant.getSubjectId());
            default -> false;
        };
    }

    private boolean isEffective(FolderGrantEntity grant, OffsetDateTime now) {
        return (grant.getEffectiveFrom() == null || !grant.getEffectiveFrom().isAfter(now))
                && (grant.getEffectiveTo() == null || grant.getEffectiveTo().isAfter(now));
    }

    private boolean isFavorite(Long folderId) {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            return false;
        }
        return folderFavoriteMapper.selectCount(Wrappers.<FolderFavoriteEntity>lambdaQuery()
                .eq(FolderFavoriteEntity::getDeleted, false)
                .eq(FolderFavoriteEntity::getFolderId, folderId)
                .eq(FolderFavoriteEntity::getUserId, user.getUserId())) > 0;
    }

    private UserContext.UserInfo requireCurrentUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "请先登录");
        }
        return user;
    }

    private List<Long> parseAncestorIds(FolderEntity folder) {
        if (folder.getAncestorIds() == null || folder.getAncestorIds().isBlank()) {
            return folder.getId() == null ? List.of() : List.of(folder.getId());
        }
        List<Long> ids = new ArrayList<>();
        for (String part : folder.getAncestorIds().split(",")) {
            parseLong(part).ifPresent(ids::add);
        }
        if (folder.getId() != null && !ids.contains(folder.getId())) {
            ids.add(folder.getId());
        }
        return ids;
    }

    private java.util.Optional<Long> parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.valueOf(value.trim()));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private void addIfNotNull(Set<Long> ids, Long id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 || limit > MAX_LIMIT ? DEFAULT_LIMIT : limit;
    }

    private OffsetDateTime nullSafeTime(OffsetDateTime time) {
        return time == null ? OffsetDateTime.MIN : time;
    }

    private TagRespDTO toTagResp(TagEntity entity) {
        TagRespDTO dto = new TagRespDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }

    private record FavoriteMeta(OffsetDateTime createdAt, String createdBy) {
    }
}
