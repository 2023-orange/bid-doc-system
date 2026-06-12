package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.constant.ChecklistItemStatusEnum;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.constant.ProjectStageEnum;
import com.example.biddoc.project.constant.ProjectStatusEnum;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.dto.req.ProjectMemberSaveReqDTO;
import com.example.biddoc.project.dto.req.ProjectUpdateReqDTO;
import com.example.biddoc.project.dto.resp.ProjectRespDTO;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.entity.ProjectNoSequenceEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectNoSequenceMapper;
import com.example.biddoc.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final DateTimeFormatter PROJECT_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectNoSequenceMapper sequenceMapper;
    private final ProjectChecklistItemMapper checklistItemMapper;
    private final SysDepartmentMapper departmentMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateReqDTO req) {
        UserContext.UserInfo user = requireUser();
        if (CollectionUtils.isEmpty(req.getOwnerUserIds())) {
            throw new BusinessException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }

        Long projectId = IdWorker.getId();
        String projectNo = generateProjectNo(req.getOwnerDeptId());

        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setProjectNo(projectNo);
        project.setProjectName(req.getProjectName());
        project.setTenderUnit(req.getTenderUnit());
        project.setOwnerDeptId(req.getOwnerDeptId());
        project.setProjectType(req.getProjectType());
        project.setProjectStage(ProjectStageEnum.COLLECTING.getCode());
        project.setProjectStatus(ProjectStatusEnum.NORMAL.getCode());
        project.setBidDeadline(req.getBidDeadline());
        project.setFolderId(req.getFolderId());
        project.setDeleted(Boolean.FALSE);
        projectMapper.insert(project);

        addMemberSet(projectId, req.getOwnerUserIds(), ProjectMemberRoleEnum.OWNER.getCode());
        addMemberSet(projectId, req.getMaterialOwnerUserIds(), ProjectMemberRoleEnum.MATERIAL_OWNER.getCode());
        addMemberSet(projectId, req.getMemberUserIds(), ProjectMemberRoleEnum.MEMBER.getCode());

        recordAudit(projectId, AuditOperationTypeEnum.CREATE.getCode(), Map.of("projectNo", projectNo));
        notifyUsers(projectId, req.getProjectName(), req.getOwnerUserIds());
        notifyUsers(projectId, req.getProjectName(), req.getMaterialOwnerUserIds());
        notifyUsers(projectId, req.getProjectName(), req.getMemberUserIds());
        return projectId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProjectUpdateReqDTO req) {
        ProjectEntity project = requireProject(id);
        checkManage(project);
        ensureEditable(project);
        project.setProjectName(StringUtils.hasText(req.getProjectName()) ? req.getProjectName() : project.getProjectName());
        project.setTenderUnit(req.getTenderUnit());
        project.setProjectType(req.getProjectType());
        project.setBidDeadline(req.getBidDeadline());
        project.setFolderId(req.getFolderId());
        projectMapper.updateById(project);
        recordAudit(id, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("projectName", project.getProjectName()));
    }

    @Override
    public ProjectRespDTO get(Long id) {
        ProjectEntity project = requireProject(id);
        checkView(project);
        return toResp(project);
    }

    @Override
    public PageResponse<ProjectRespDTO> list(String keyword, String projectNo, Long ownerDeptId, String projectType,
                                             String projectStage, String projectStatus, Long ownerUserId,
                                             OffsetDateTime deadlineFrom, OffsetDateTime deadlineTo,
                                             Integer page, Integer size) {
        UserContext.UserInfo user = requireUser();
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 || size > 100 ? 20 : size;
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<ProjectEntity>()
                .like(StringUtils.hasText(keyword), ProjectEntity::getProjectName, keyword)
                .eq(StringUtils.hasText(projectNo), ProjectEntity::getProjectNo, projectNo)
                .eq(ownerDeptId != null, ProjectEntity::getOwnerDeptId, ownerDeptId)
                .eq(StringUtils.hasText(projectType), ProjectEntity::getProjectType, projectType)
                .eq(StringUtils.hasText(projectStage), ProjectEntity::getProjectStage, projectStage)
                .eq(StringUtils.hasText(projectStatus), ProjectEntity::getProjectStatus, projectStatus)
                .ge(deadlineFrom != null, ProjectEntity::getBidDeadline, deadlineFrom)
                .le(deadlineTo != null, ProjectEntity::getBidDeadline, deadlineTo)
                .orderByDesc(ProjectEntity::getUpdatedAt);
        if (!user.isSuperAdmin()) {
            // 后端权限是安全边界：普通用户项目列表只返回自己参与的项目。
            wrapper.inSql(ProjectEntity::getId,
                    "select project_id from bid_project_member where deleted = false and user_id = " + user.getUserId());
        }
        if (ownerUserId != null) {
            wrapper.inSql(ProjectEntity::getId,
                    "select project_id from bid_project_member where deleted = false and member_role = 'OWNER' and user_id = " + ownerUserId);
        }
        Page<ProjectEntity> result = projectMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<ProjectRespDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toResp).toList());
        return PageResponse.of(dtoPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long projectId, ProjectMemberSaveReqDTO req) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        ensureEditable(project);
        String role = StringUtils.hasText(req.getMemberRole()) ? req.getMemberRole() : ProjectMemberRoleEnum.MEMBER.getCode();
        addMemberSet(projectId, req.getUserIds(), role);
        notifyUsers(projectId, project.getProjectName(), req.getUserIds());
        recordAudit(projectId, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("memberRole", role));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long projectId, Long userId) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        ensureEditable(project);
        ProjectMemberEntity update = new ProjectMemberEntity();
        update.setDeleted(Boolean.TRUE);
        projectMemberMapper.update(update, new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getUserId, userId));
        recordAudit(projectId, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("removedUserId", userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStage(Long projectId, String projectStage) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        ensureEditable(project);
        project.setProjectStage(projectStage);
        projectMapper.updateById(project);
        recordAudit(projectId, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("projectStage", projectStage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long projectId, String projectStatus) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        if (!ProjectStatusEnum.ARCHIVED.getCode().equals(projectStatus)) {
            ensureEditable(project);
        }
        if (ProjectStatusEnum.ARCHIVED.getCode().equals(projectStatus)) {
            ensureCanArchive(projectId);
            archiveChecklist(projectId);
        }
        project.setProjectStatus(projectStatus);
        if (ProjectStatusEnum.ARCHIVED.getCode().equals(projectStatus)) {
            project.setProjectStage(ProjectStageEnum.ARCHIVED.getCode());
        }
        projectMapper.updateById(project);
        recordAudit(projectId, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("projectStatus", projectStatus));
        if (ProjectStatusEnum.ARCHIVED.getCode().equals(projectStatus)) {
            notifyProjectMembers(projectId, project.getProjectName(), "PROJECT_ARCHIVED", "投标项目已归档");
        }
    }

    private String generateProjectNo(Long deptId) {
        SysDepartment dept = departmentMapper.selectById(deptId);
        Object abbr = dept != null && dept.getExtensionData() != null ? dept.getExtensionData().get("abbr") : null;
        if (abbr == null || !StringUtils.hasText(String.valueOf(abbr))) {
            throw new BusinessException(ErrorCode.PROJECT_DEPT_ABBR_REQUIRED);
        }
        LocalDate today = LocalDate.now();
        ProjectNoSequenceEntity sequence = sequenceMapper.selectOne(new LambdaQueryWrapper<ProjectNoSequenceEntity>()
                .eq(ProjectNoSequenceEntity::getDeptId, deptId)
                .eq(ProjectNoSequenceEntity::getBizDate, today));
        int nextSeq;
        if (sequence == null) {
            sequence = new ProjectNoSequenceEntity();
            sequence.setId(IdWorker.getId());
            sequence.setDeptId(deptId);
            sequence.setBizDate(today);
            sequence.setCurrentSeq(1);
            sequenceMapper.insert(sequence);
            nextSeq = 1;
        } else {
            nextSeq = sequence.getCurrentSeq() + 1;
            sequence.setCurrentSeq(nextSeq);
            sequenceMapper.updateById(sequence);
        }
        return String.format("%s-%s-%03d", abbr, today.format(PROJECT_NO_DATE), nextSeq);
    }

    private void addMemberSet(Long projectId, Iterable<Long> userIds, String role) {
        if (userIds == null) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>();
        userIds.forEach(distinct::add);
        for (Long userId : distinct) {
            ProjectMemberEntity member = new ProjectMemberEntity();
            member.setId(IdWorker.getId());
            member.setProjectId(projectId);
            member.setUserId(userId);
            member.setMemberRole(role);
            member.setDeleted(Boolean.FALSE);
            projectMemberMapper.insert(member);
        }
    }

    private ProjectEntity requireProject(Long id) {
        ProjectEntity project = projectMapper.selectById(id);
        if (project == null || Boolean.TRUE.equals(project.getDeleted())) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private void checkView(ProjectEntity project) {
        UserContext.UserInfo user = requireUser();
        if (user.isSuperAdmin() || isProjectMember(project.getId(), user.getUserId())) {
            return;
        }
        throw new BusinessException(ErrorCode.PROJECT_PERMISSION_DENIED);
    }

    private void checkManage(ProjectEntity project) {
        UserContext.UserInfo user = requireUser();
        if (user.isSuperAdmin() || isProjectOwner(project.getId(), user.getUserId())) {
            return;
        }
        throw new BusinessException(ErrorCode.PROJECT_PERMISSION_DENIED);
    }

    private boolean isProjectOwner(Long projectId, Long userId) {
        return projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getUserId, userId)
                .eq(ProjectMemberEntity::getMemberRole, ProjectMemberRoleEnum.OWNER.getCode())
                .eq(ProjectMemberEntity::getDeleted, false)) > 0;
    }

    private boolean isProjectMember(Long projectId, Long userId) {
        return projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getUserId, userId)
                .eq(ProjectMemberEntity::getDeleted, false)) > 0;
    }

    private void ensureEditable(ProjectEntity project) {
        if (Objects.equals(project.getProjectStatus(), ProjectStatusEnum.ARCHIVED.getCode())) {
            recordAudit(project.getId(), AuditOperationTypeEnum.UPDATE.getCode(),
                    Map.of("rejected", true, "reason", "PROJECT_ARCHIVED_READONLY"));
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED_READONLY);
        }
    }

    private void ensureCanArchive(Long projectId) {
        long unfinishedRequired = checklistItemMapper.selectCount(new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                .eq(ProjectChecklistItemEntity::getDeleted, false)
                .eq(ProjectChecklistItemEntity::getRequired, true)
                .ne(ProjectChecklistItemEntity::getStatus, ChecklistItemStatusEnum.COMPLETE.getCode()));
        if (unfinishedRequired > 0) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVE_CHECKLIST_INCOMPLETE);
        }
    }

    private void archiveChecklist(Long projectId) {
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setStatus(ChecklistItemStatusEnum.ARCHIVED.getCode());
        update.setUpdatedAt(OffsetDateTime.now());
        checklistItemMapper.update(update, new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                .eq(ProjectChecklistItemEntity::getDeleted, false));
    }

    private UserContext.UserInfo requireUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return user;
    }

    private void notifyUsers(Long projectId, String projectName, Iterable<Long> userIds) {
        if (userIds == null) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>();
        userIds.forEach(distinct::add);
        for (Long userId : distinct) {
            notificationService.send(userId, "PROJECT_MEMBER", "你已加入投标项目", "项目：" + projectName, "PROJECT", projectId);
        }
    }

    private void notifyProjectMembers(Long projectId, String projectName, String type, String title) {
        projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getDeleted, false))
                .forEach(member -> notificationService.send(member.getUserId(), type, title,
                        "项目：" + projectName, "PROJECT", projectId));
    }

    private void recordAudit(Long projectId, String operation, Map<String, Object> afterData) {
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("PROJECT")
                .bizId(projectId)
                .operationType(operation)
                .afterData(afterData)
                .build());
    }

    private ProjectRespDTO toResp(ProjectEntity entity) {
        ProjectRespDTO dto = new ProjectRespDTO();
        dto.setId(entity.getId());
        dto.setProjectNo(entity.getProjectNo());
        dto.setProjectName(entity.getProjectName());
        dto.setTenderUnit(entity.getTenderUnit());
        dto.setOwnerDeptId(entity.getOwnerDeptId());
        dto.setProjectType(entity.getProjectType());
        dto.setProjectStage(entity.getProjectStage());
        dto.setProjectStatus(entity.getProjectStatus());
        dto.setBidDeadline(entity.getBidDeadline());
        dto.setFolderId(entity.getFolderId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
