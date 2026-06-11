package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.constant.FolderGrantScopeEnum;
import com.example.biddoc.folder.constant.FolderPermissionCodeEnum;
import com.example.biddoc.folder.constant.FolderSubjectTypeEnum;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderGrantEntity;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FolderPermissionServiceImpl implements FolderPermissionService {

    private static final int ROOT_LEVEL = 0;

    private final FolderManagerMapper folderManagerMapper;
    private final FolderGrantMapper folderGrantMapper;

    @Override
    public boolean canView(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        // 第一版只给同部门查看权限，避免直接放开编辑能力。
        if (folder.getOwnerDeptId() != null && Objects.equals(folder.getOwnerDeptId(), user.getDeptId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(
                FolderPermissionCodeEnum.FOLDER_VIEW.getCode(),
                FolderPermissionCodeEnum.FOLDER_EDIT.getCode(),
                FolderPermissionCodeEnum.FOLDER_RENAME.getCode()
        ));
    }

    @Override
    public boolean canRename(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        // 根级目录的敏感操作继续只允许超级管理员。
        if (isRoot(folder)) {
            return false;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(FolderPermissionCodeEnum.FOLDER_RENAME.getCode()));
    }

    @Override
    public boolean canEdit(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        // 根级目录的敏感操作继续只允许超级管理员。
        if (isRoot(folder)) {
            return false;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(FolderPermissionCodeEnum.FOLDER_EDIT.getCode()));
    }

    @Override
    public boolean canDelete(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        // 根级目录的删除属于敏感操作，仅 SUPER_ADMIN 可执行（v4 §3.1）
        if (isRoot(folder)) {
            return false;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(FolderPermissionCodeEnum.FOLDER_DELETE.getCode()));
    }

    @Override
    public boolean canMove(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        // 根级源永远不可 move：MVP 不开放根目录"降级"为非根；
        // 即使是 SUPER_ADMIN，也不能通过 move 接口让根级变非根，需另外的"重设父级"接口（未来扩展）
        if (isRoot(folder)) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(FolderPermissionCodeEnum.FOLDER_MOVE.getCode()));
    }

    @Override
    public boolean canCopy(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (folder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        // copy 不在 v4 §3.1 根级敏感操作清单内；root 源对非 SUPER_ADMIN 通过 grant/owner 通常也不可达，
        // 故此处不做 isRoot 显式拦截，按常规权限链处理即可
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(folder, user.getUserId())) {
            return true;
        }
        return hasGrantPermission(folder, user, Set.of(FolderPermissionCodeEnum.FOLDER_COPY.getCode()));
    }

    @Override
    public boolean canCreateChild(FolderEntity parentFolder) {
        UserContext.UserInfo user = UserContext.get();
        if (parentFolder == null || user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        if (Objects.equals(parentFolder.getOwnerUserId(), user.getUserId())) {
            return true;
        }
        if (hasManagerScope(parentFolder, user.getUserId())) {
            return true;
        }
        // 第一版优先识别显式创建权限；若未建完整创建模型，则允许 FOLDER_EDIT 兜底复用。
        return hasGrantPermission(parentFolder, user, Set.of(
                FolderPermissionCodeEnum.FOLDER_CREATE.getCode(),
                FolderPermissionCodeEnum.FOLDER_EDIT.getCode()
        ));
    }

    @Override
    public void checkView(FolderEntity folder) {
        if (!canView(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkRename(FolderEntity folder) {
        if (!canRename(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkEdit(FolderEntity folder) {
        if (!canEdit(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkCreateChild(FolderEntity parentFolder) {
        if (!canCreateChild(parentFolder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkDelete(FolderEntity folder) {
        if (!canDelete(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkMove(FolderEntity folder) {
        if (!canMove(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public void checkCopy(FolderEntity folder) {
        if (!canCopy(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
    }

    @Override
    public List<FolderEntity> filterViewableFolders(List<FolderEntity> folders) {
        if (folders == null || folders.isEmpty()) {
            return Collections.emptyList();
        }
        return folders.stream()
                .filter(this::canView)
                .toList();
    }

    @Override
    public boolean isManagerOfFolder(FolderEntity folder, Long userId) {
        return hasManagerScope(folder, userId);
    }

    private boolean hasManagerScope(FolderEntity folder, Long userId) {
        return FolderManagerScopeSupport.isManagerOf(folder, userId, folderManagerMapper);
    }

    private boolean hasGrantPermission(FolderEntity folder,
                                       UserContext.UserInfo user,
                                       Set<String> permissionCodes) {
        if (folder.getId() == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }
        List<Long> candidateFolderIds = FolderManagerScopeSupport.resolveSelfAndAncestors(folder);
        if (candidateFolderIds.isEmpty()) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<FolderGrantEntity> grants = folderGrantMapper.selectList(
                Wrappers.<FolderGrantEntity>lambdaQuery()
                        .eq(FolderGrantEntity::getDeleted, false)
                        .in(FolderGrantEntity::getFolderId, candidateFolderIds)
                        .in(FolderGrantEntity::getPermissionCode, permissionCodes)
                        .and(w -> w.isNull(FolderGrantEntity::getEffectiveFrom)
                                .or().le(FolderGrantEntity::getEffectiveFrom, now))
                        .and(w -> w.isNull(FolderGrantEntity::getEffectiveTo)
                                .or().gt(FolderGrantEntity::getEffectiveTo, now))
        );

        for (FolderGrantEntity grant : grants) {
            if (!matchesGrantSubject(grant, user)) {
                continue;
            }
            if (Objects.equals(grant.getFolderId(), folder.getId())) {
                return true;
            }
            if (FolderGrantScopeEnum.SELF_AND_DESCENDANTS.getCode().equals(grant.getGrantScope())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGrantSubject(FolderGrantEntity grant, UserContext.UserInfo user) {
        if (grant == null || user == null || grant.getSubjectType() == null || grant.getSubjectId() == null) {
            return false;
        }
        if (FolderSubjectTypeEnum.USER.getCode().equals(grant.getSubjectType())) {
            return Objects.equals(String.valueOf(user.getUserId()), grant.getSubjectId());
        }
        if (FolderSubjectTypeEnum.DEPT.getCode().equals(grant.getSubjectType())) {
            return user.getDeptId() != null && Objects.equals(String.valueOf(user.getDeptId()), grant.getSubjectId());
        }
        if (FolderSubjectTypeEnum.ROLE.getCode().equals(grant.getSubjectType())) {
            return user.getRoleCodes() != null && user.getRoleCodes().contains(grant.getSubjectId());
        }
        return false;
    }

    private boolean isRoot(FolderEntity folder) {
        return folder.getLevel() != null && folder.getLevel() == ROOT_LEVEL;
    }
}
