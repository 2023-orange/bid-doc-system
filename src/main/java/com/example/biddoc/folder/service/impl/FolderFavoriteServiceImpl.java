package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.dto.resp.FolderFavoriteRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderFavoriteService;
import com.example.biddoc.folder.service.FolderPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderFavoriteServiceImpl implements FolderFavoriteService {

    private final FolderMapper folderMapper;
    private final FolderFavoriteMapper folderFavoriteMapper;
    private final FolderPermissionService folderPermissionService;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long folderId) {
        UserContext.UserInfo user = requireCurrentUser();
        FolderEntity folder = getExistingFolder(folderId);
        folderPermissionService.checkView(folder);

        FolderFavoriteEntity existing = getActiveFavorite(folderId, user.getUserId());
        if (existing != null) {
            return;
        }

        FolderFavoriteEntity entity = new FolderFavoriteEntity();
        entity.setFolderId(folderId);
        entity.setUserId(user.getUserId());
        entity.setDeleted(Boolean.FALSE);
        folderFavoriteMapper.insert(entity);

        // 只有真实新增收藏时写审计，避免重复点击产生无意义审计噪声。
        recordFavoriteAudit(folderId, AuditOperationTypeEnum.FAVORITE, Map.of("folderName", folder.getName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long folderId) {
        UserContext.UserInfo user = requireCurrentUser();
        FolderEntity folder = getExistingFolder(folderId);
        folderPermissionService.checkView(folder);

        FolderFavoriteEntity existing = getActiveFavorite(folderId, user.getUserId());
        if (existing == null) {
            return;
        }

        existing.setDeleted(Boolean.TRUE);
        folderFavoriteMapper.updateById(existing);

        // 只有真实取消收藏时写审计，幂等空操作不写审计。
        recordFavoriteAudit(folderId, AuditOperationTypeEnum.UNFAVORITE, Map.of("favoriteId", String.valueOf(existing.getId())));
    }

    @Override
    public List<FolderFavoriteRespDTO> listFavorites() {
        UserContext.UserInfo user = requireCurrentUser();
        List<FolderFavoriteEntity> favorites = folderFavoriteMapper.selectList(
                Wrappers.<FolderFavoriteEntity>lambdaQuery()
                        .eq(FolderFavoriteEntity::getUserId, user.getUserId())
                        .eq(FolderFavoriteEntity::getDeleted, false)
                        .orderByDesc(FolderFavoriteEntity::getCreatedAt)
        );
        if (CollectionUtils.isEmpty(favorites)) {
            return Collections.emptyList();
        }

        List<Long> folderIds = favorites.stream().map(FolderFavoriteEntity::getFolderId).distinct().toList();
        Map<Long, FolderEntity> folderMap = folderMapper.selectBatchIds(folderIds).stream()
                .filter(folder -> Boolean.FALSE.equals(folder.getDeleted()))
                .collect(Collectors.toMap(FolderEntity::getId, Function.identity(), (left, right) -> left));

        return favorites.stream()
                .map(favorite -> toRespDTO(favorite, folderMap.get(favorite.getFolderId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private UserContext.UserInfo requireCurrentUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "请先登录");
        }
        return user;
    }

    private FolderEntity getExistingFolder(Long folderId) {
        FolderEntity folder = folderMapper.selectOne(
                Wrappers.<FolderEntity>lambdaQuery()
                        .eq(FolderEntity::getId, folderId)
                        .eq(FolderEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private FolderFavoriteEntity getActiveFavorite(Long folderId, Long userId) {
        return folderFavoriteMapper.selectOne(
                Wrappers.<FolderFavoriteEntity>lambdaQuery()
                        .eq(FolderFavoriteEntity::getFolderId, folderId)
                        .eq(FolderFavoriteEntity::getUserId, userId)
                        .eq(FolderFavoriteEntity::getDeleted, false)
                        .last("limit 1")
        );
    }

    private FolderFavoriteRespDTO toRespDTO(FolderFavoriteEntity favorite, FolderEntity folder) {
        if (folder == null || !folderPermissionService.canView(folder)) {
            return null;
        }
        FolderFavoriteRespDTO dto = new FolderFavoriteRespDTO();
        dto.setFavoriteId(favorite.getId());
        dto.setFolderId(folder.getId());
        dto.setParentId(folder.getParentId());
        dto.setFolderName(folder.getName());
        dto.setLevel(folder.getLevel());
        dto.setSortNo(folder.getSortNo());
        dto.setCreatedAt(favorite.getCreatedAt());
        dto.setCreatedBy(favorite.getCreatedBy());
        return dto;
    }

    private void recordFavoriteAudit(Long folderId, AuditOperationTypeEnum operationType, Map<String, Object> extraData) {
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(folderId)
                .operationType(operationType.getCode())
                .extraData(extraData)
                .build());
    }
}
