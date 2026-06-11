package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.folder.constant.FolderManageScopeEnum;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderManagerEntity;
import com.example.biddoc.folder.mapper.FolderManagerMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 文件夹管理员范围判断的模块内复用工具。
 * 该逻辑涉及授权边界，必须保持 grant / manager / permission 三处判断一致。
 */
final class FolderManagerScopeSupport {

    private FolderManagerScopeSupport() {
    }

    static boolean isManagerOf(FolderEntity folder, Long userId, FolderManagerMapper folderManagerMapper) {
        if (folder == null || folder.getId() == null || userId == null) {
            return false;
        }
        List<Long> candidateFolderIds = resolveSelfAndAncestors(folder);
        if (candidateFolderIds.isEmpty()) {
            return false;
        }

        List<FolderManagerEntity> managers = folderManagerMapper.selectList(
                Wrappers.<FolderManagerEntity>lambdaQuery()
                        .eq(FolderManagerEntity::getDeleted, false)
                        .eq(FolderManagerEntity::getUserId, userId)
                        .in(FolderManagerEntity::getFolderId, candidateFolderIds)
        );

        for (FolderManagerEntity manager : managers) {
            if (Objects.equals(manager.getFolderId(), folder.getId())) {
                return true;
            }
            if (FolderManageScopeEnum.SELF_AND_DESCENDANTS.getCode().equals(manager.getManageScope())) {
                return true;
            }
        }
        return false;
    }

    static List<Long> resolveSelfAndAncestors(FolderEntity folder) {
        if (folder == null) {
            return List.of();
        }
        LinkedHashSet<Long> folderIds = new LinkedHashSet<>();
        String ancestorIds = folder.getAncestorIds();
        if (ancestorIds != null && !ancestorIds.isBlank()) {
            for (String part : ancestorIds.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    folderIds.add(Long.valueOf(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (folder.getId() != null) {
            folderIds.add(folder.getId());
        }
        return new ArrayList<>(folderIds);
    }
}
