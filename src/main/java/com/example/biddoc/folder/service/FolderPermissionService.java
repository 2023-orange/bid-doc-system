package com.example.biddoc.folder.service;

import com.example.biddoc.folder.entity.FolderEntity;

import java.util.List;

public interface FolderPermissionService {

    boolean canView(FolderEntity folder);

    boolean canRename(FolderEntity folder);

    boolean canEdit(FolderEntity folder);

    boolean canCreateChild(FolderEntity parentFolder);

    boolean canDelete(FolderEntity folder);

    boolean canMove(FolderEntity folder);

    boolean canCopy(FolderEntity folder);

    void checkView(FolderEntity folder);

    void checkRename(FolderEntity folder);

    void checkEdit(FolderEntity folder);

    void checkCreateChild(FolderEntity parentFolder);

    void checkDelete(FolderEntity folder);

    void checkMove(FolderEntity folder);

    void checkCopy(FolderEntity folder);

    List<FolderEntity> filterViewableFolders(List<FolderEntity> folders);

    /**
     * 判断指定用户是否为 folder 的有效管理员（含基于 manage_scope 的祖先链匹配）。
     * 提取自 FolderPermissionServiceImpl 内的私有判定逻辑，供 document 模块的严格三元权限复用。
     *
     * @param folder 目标 folder（非空，未删除）
     * @param userId 待判定用户 ID（非空）
     * @return true 表示用户对该 folder 具备 manager 身份（SELF 或 SELF_AND_DESCENDANTS 覆盖）
     */
    boolean isManagerOfFolder(FolderEntity folder, Long userId);
}
