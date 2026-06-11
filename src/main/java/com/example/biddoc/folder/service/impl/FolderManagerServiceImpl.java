package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.auth.service.UserRoleService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.constant.FolderManageScopeEnum;
import com.example.biddoc.folder.dto.req.FolderManagerSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderManagerRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderManagerEntity;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderManagerService;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderManagerServiceImpl implements FolderManagerService {

    private final FolderMapper folderMapper;
    private final FolderManagerMapper folderManagerMapper;
    private final FolderPermissionService folderPermissionService;
    private final UserRoleService userRoleService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    public List<FolderManagerRespDTO> list(Long folderId) {
        FolderEntity folder = getExistingFolder(folderId);
        folderPermissionService.checkView(folder);

        List<FolderManagerEntity> managers = folderManagerMapper.selectList(
                Wrappers.<FolderManagerEntity>lambdaQuery()
                        .eq(FolderManagerEntity::getFolderId, folderId)
                        .eq(FolderManagerEntity::getDeleted, false)
                        .orderByDesc(FolderManagerEntity::getCreatedAt)
        );
        return managers.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long folderId, FolderManagerSaveReqDTO req) {
        FolderEntity folder = getExistingFolder(folderId);

        // 根级目录不允许设置 manager，根级由 SUPER_ADMIN 直接管理
        if (isRoot(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED, "根级文件夹不允许设置管理员");
        }

        checkManageAuthority(folder);

        // manageScope 枚举校验
        if (FolderManageScopeEnum.getByCode(req.getManageScope()) == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "manageScope 非法: " + req.getManageScope());
        }

        // 目标用户必须持有 FOLDER_ADMIN 角色，否则无资格成为目录管理员
        List<String> targetRoles = userRoleService.getActiveRoleCodes(req.getUserId());
        if (targetRoles == null || !targetRoles.contains(RoleCodeEnum.FOLDER_ADMIN.getCode())) {
            throw new BusinessException(ErrorCode.FOLDER_MANAGER_REQUIRES_ROLE);
        }

        FolderManagerEntity entity = new FolderManagerEntity();
        entity.setId(IdWorker.getId());
        entity.setFolderId(folderId);
        entity.setUserId(req.getUserId());
        entity.setManageScope(req.getManageScope());
        entity.setDeleted(Boolean.FALSE);

        try {
            folderManagerMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_doc_folder_manager_active 冲突
            throw new BusinessException(ErrorCode.FOLDER_MANAGER_DUPLICATED);
        }

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("userId", String.valueOf(req.getUserId()));
        afterData.put("manageScope", req.getManageScope());

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("managerId", String.valueOf(entity.getId()));

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(folderId)
                .operationType(AuditOperationTypeEnum.MANAGER_ADD.getCode())
                .afterData(afterData)
                .extraData(extraData)
                .build());

        notificationService.send(req.getUserId(),
                "FOLDER_MANAGER",
                "被设置为文件夹管理员",
                "你被设置为文件夹管理员：" + folder.getName(),
                "FOLDER",
                folderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long folderId, Long managerId) {
        FolderManagerEntity manager = folderManagerMapper.selectOne(
                Wrappers.<FolderManagerEntity>lambdaQuery()
                        .eq(FolderManagerEntity::getId, managerId)
                        .eq(FolderManagerEntity::getDeleted, false)
                        .last("limit 1")
        );
        // managerId 不存在或不属于当前 folderId，统一返回"不存在"
        if (manager == null || !Objects.equals(manager.getFolderId(), folderId)) {
            throw new BusinessException(ErrorCode.FOLDER_MANAGER_NOT_FOUND);
        }

        FolderEntity folder = getExistingFolder(folderId);
        checkManageAuthority(folder);

        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("userId", String.valueOf(manager.getUserId()));
        beforeData.put("manageScope", manager.getManageScope());

        manager.setDeleted(Boolean.TRUE);
        folderManagerMapper.updateById(manager);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("managerId", String.valueOf(managerId));

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(folderId)
                .operationType(AuditOperationTypeEnum.MANAGER_REMOVE.getCode())
                .beforeData(beforeData)
                .extraData(extraData)
                .build());
    }

    // ---- 私有辅助方法 ----

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

    private boolean isRoot(FolderEntity folder) {
        return folder.getLevel() != null && folder.getLevel() == 0;
    }

    /**
     * 校验当前用户是否有权管理该目录的 manager 列表：
     * SUPER_ADMIN > 目录 owner > 该目录有效 manager（含 SELF_AND_DESCENDANTS 继承）。
     */
    private void checkManageAuthority(FolderEntity folder) {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
        }
        if (user.isSuperAdmin()) {
            return;
        }
        if (Objects.equals(folder.getOwnerUserId(), user.getUserId())) {
            return;
        }
        if (isManagerOf(folder, user.getUserId())) {
            return;
        }
        throw new BusinessException(ErrorCode.FOLDER_PERMISSION_DENIED);
    }

    private boolean isManagerOf(FolderEntity folder, Long userId) {
        return FolderManagerScopeSupport.isManagerOf(folder, userId, folderManagerMapper);
    }

    private FolderManagerRespDTO toRespDTO(FolderManagerEntity entity) {
        FolderManagerRespDTO dto = new FolderManagerRespDTO();
        dto.setManagerId(entity.getId());
        dto.setFolderId(entity.getFolderId());
        dto.setUserId(entity.getUserId());
        dto.setManageScope(entity.getManageScope());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
