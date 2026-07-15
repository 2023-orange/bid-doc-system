package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.folder.constant.FolderGrantScopeEnum;
import com.example.biddoc.folder.constant.FolderPermissionCodeEnum;
import com.example.biddoc.folder.constant.FolderSubjectTypeEnum;
import com.example.biddoc.folder.dto.req.FolderGrantSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderGrantRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderGrantEntity;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderGrantService;
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
public class FolderGrantServiceImpl implements FolderGrantService {

    private final FolderMapper folderMapper;
    private final FolderGrantMapper folderGrantMapper;
    // 直接注入 mapper 而非 FolderPermissionService，避免循环依赖
    private final FolderManagerMapper folderManagerMapper;
    private final FolderPermissionService folderPermissionService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    public List<FolderGrantRespDTO> list(Long folderId) {
        FolderEntity folder = getExistingFolder(folderId);
        // 查看授权列表需要对该目录有可见权限
        folderPermissionService.checkView(folder);

        List<FolderGrantEntity> grants = folderGrantMapper.selectList(
                Wrappers.<FolderGrantEntity>lambdaQuery()
                        .eq(FolderGrantEntity::getFolderId, folderId)
                        .eq(FolderGrantEntity::getDeleted, false)
                        .orderByDesc(FolderGrantEntity::getCreatedAt)
        );
        return grants.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long folderId, FolderGrantSaveReqDTO req) {
        FolderEntity folder = getExistingFolder(folderId);

        // 根级目录（level=0）不允许授权，防止通过授权绕过根级保护规则
        if (isRoot(folder)) {
            throw new BusinessException(ErrorCode.FOLDER_GRANT_ON_ROOT_FORBIDDEN);
        }

        // 操作者必须是 SUPER_ADMIN、目录 owner 或该目录的有效 manager
        checkManageAuthority(folder);

        // 枚举合法性校验：subjectType / grantScope / 每个 permissionCode
        validateGrantParams(req);

        List<Long> insertedIds = new ArrayList<>();
        for (String permCode : req.getPermissionCodes()) {
            FolderGrantEntity entity = new FolderGrantEntity();
            entity.setId(IdWorker.getId());
            entity.setFolderId(folderId);
            entity.setSubjectType(req.getSubjectType());
            entity.setSubjectId(req.getSubjectId());
            entity.setPermissionCode(permCode);
            entity.setGrantScope(req.getGrantScope());
            entity.setEffectiveFrom(req.getEffectiveFrom());
            entity.setEffectiveTo(req.getEffectiveTo());
            entity.setDeleted(Boolean.FALSE);
            try {
                folderGrantMapper.insert(entity);
                insertedIds.add(entity.getId());
            } catch (DuplicateKeyException e) {
                // 唯一索引 uk_doc_folder_grant_active 冲突，转换为业务异常
                throw new BusinessException(ErrorCode.FOLDER_GRANT_DUPLICATED);
            }
        }

        // 审计：每次 add 写一条汇总记录，extraData 包含所有新增 grantId
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("subjectType", req.getSubjectType());
        afterData.put("subjectId", req.getSubjectId());
        afterData.put("permissionCodes", req.getPermissionCodes());
        afterData.put("grantScope", req.getGrantScope());

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("grantIds", insertedIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        extraData.put("subjectType", req.getSubjectType());
        extraData.put("subjectId", req.getSubjectId());

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(folderId)
                .operationType(AuditOperationTypeEnum.GRANT_ADD.getCode())
                .objectName(folder.getName())
                .actionSummary(currentActorName() + " 为文件夹《" + folder.getName() + "》新增授权")
                .afterData(afterData)
                .extraData(extraData)
                .build());

        if (FolderSubjectTypeEnum.USER.getCode().equals(req.getSubjectType())) {
            try {
                notificationService.send(Long.valueOf(req.getSubjectId()),
                        "FOLDER_GRANT",
                        "获得文件夹权限",
                        "你获得了文件夹权限：" + folder.getName(),
                        "FOLDER",
                        folderId);
            } catch (NumberFormatException ignored) {
                // ROLE / DEPT 或非法用户 id 不在这里展开通知，授权校验仍以业务结果为准。
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long folderId, Long grantId) {
        FolderGrantEntity grant = folderGrantMapper.selectOne(
                Wrappers.<FolderGrantEntity>lambdaQuery()
                        .eq(FolderGrantEntity::getId, grantId)
                        .eq(FolderGrantEntity::getDeleted, false)
                        .last("limit 1")
        );
        // grantId 不存在或不属于当前 folderId，统一返回"不存在"，避免信息泄露
        if (grant == null || !Objects.equals(grant.getFolderId(), folderId)) {
            throw new BusinessException(ErrorCode.FOLDER_GRANT_NOT_FOUND);
        }

        FolderEntity folder = getExistingFolder(folderId);
        checkManageAuthority(folder);

        // 记录删除前快照，用于审计 before
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("subjectType", grant.getSubjectType());
        beforeData.put("subjectId", grant.getSubjectId());
        beforeData.put("permissionCode", grant.getPermissionCode());
        beforeData.put("grantScope", grant.getGrantScope());

        // 逻辑删除
        grant.setDeleted(Boolean.TRUE);
        folderGrantMapper.updateById(grant);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("grantId", String.valueOf(grantId));

        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.FOLDER.getCode())
                .bizType("FOLDER")
                .bizId(folderId)
                .operationType(AuditOperationTypeEnum.GRANT_REMOVE.getCode())
                .objectName(folder.getName())
                .actionSummary(currentActorName() + " 移除了文件夹《" + folder.getName() + "》授权")
                .beforeData(beforeData)
                .extraData(extraData)
                .build());
    }

    // ---- 私有辅助方法 ----

    private String currentActorName() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            return "系统";
        }
        return user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : String.valueOf(user.getUserId());
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

    private boolean isRoot(FolderEntity folder) {
        return folder.getLevel() != null && folder.getLevel() == 0;
    }

    /**
     * 校验当前用户是否有权管理该目录的授权：
     * SUPER_ADMIN > 目录 owner > 该目录有效 manager（含 SELF_AND_DESCENDANTS 继承）。
     * 直接查 FolderManagerMapper 而非调用 FolderPermissionService，避免循环依赖。
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

    /**
     * 判断 userId 是否是 folder 的有效 manager（含 SELF_AND_DESCENDANTS 继承）。
     * 逻辑与 FolderPermissionServiceImpl.hasManagerScope 保持一致。
     */
    private boolean isManagerOf(FolderEntity folder, Long userId) {
        return FolderManagerScopeSupport.isManagerOf(folder, userId, folderManagerMapper);
    }

    private void validateGrantParams(FolderGrantSaveReqDTO req) {
        // subjectType 枚举校验
        if (!FolderSubjectTypeEnum.isValid(req.getSubjectType())) {
            throw new BusinessException(ErrorCode.FOLDER_GRANT_SUBJECT_INVALID,
                    "subjectType 非法: " + req.getSubjectType());
        }
        // ROLE 类型的 subjectId 必须是合法的 roleCode
        if (FolderSubjectTypeEnum.ROLE.getCode().equals(req.getSubjectType())
                && !RoleCodeEnum.isValid(req.getSubjectId())) {
            throw new BusinessException(ErrorCode.FOLDER_GRANT_SUBJECT_INVALID,
                    "ROLE 类型的 subjectId 必须是合法的 roleCode: " + req.getSubjectId());
        }
        // grantScope 枚举校验
        if (FolderGrantScopeEnum.getByCode(req.getGrantScope()) == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "grantScope 非法: " + req.getGrantScope());
        }
        // 每个 permissionCode 必须在枚举内
        for (String code : req.getPermissionCodes()) {
            if (!FolderPermissionCodeEnum.isValid(code)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        "permissionCode 非法: " + code);
            }
        }
    }

    private FolderGrantRespDTO toRespDTO(FolderGrantEntity entity) {
        FolderGrantRespDTO dto = new FolderGrantRespDTO();
        dto.setGrantId(entity.getId());
        dto.setFolderId(entity.getFolderId());
        dto.setSubjectType(entity.getSubjectType());
        dto.setSubjectId(entity.getSubjectId());
        dto.setPermissionCode(entity.getPermissionCode());
        dto.setGrantScope(entity.getGrantScope());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
