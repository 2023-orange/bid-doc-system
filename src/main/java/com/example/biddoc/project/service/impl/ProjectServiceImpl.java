package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.entity.AuditOperationLogEntity;
import com.example.biddoc.audit.mapper.AuditOperationLogMapper;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DocumentVersionMapper;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.constant.ChecklistItemStatusEnum;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.constant.ProjectStageEnum;
import com.example.biddoc.project.constant.ProjectStatusEnum;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.dto.req.ProjectMemberSaveReqDTO;
import com.example.biddoc.project.dto.req.ProjectUpdateReqDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveChecklistSnapshotRespDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveDetailRespDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveDocumentSnapshotRespDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveRecordRespDTO;
import com.example.biddoc.project.dto.resp.ProjectRespDTO;
import com.example.biddoc.project.dto.resp.ProjectWorkbenchRespDTO;
import com.example.biddoc.project.entity.ProjectArchiveChecklistSnapshotEntity;
import com.example.biddoc.project.entity.ProjectArchiveDocumentSnapshotEntity;
import com.example.biddoc.project.entity.ProjectArchiveRecordEntity;
import com.example.biddoc.project.entity.ProjectChecklistDocumentEntity;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.entity.ProjectNoSequenceEntity;
import com.example.biddoc.project.mapper.ProjectArchiveChecklistSnapshotMapper;
import com.example.biddoc.project.mapper.ProjectArchiveDocumentSnapshotMapper;
import com.example.biddoc.project.mapper.ProjectArchiveRecordMapper;
import com.example.biddoc.project.mapper.ProjectChecklistDocumentMapper;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectNoSequenceMapper;
import com.example.biddoc.project.service.ProjectService;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.mapper.ApprovalInstanceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final DateTimeFormatter PROJECT_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper();

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectNoSequenceMapper sequenceMapper;
    private final ProjectChecklistItemMapper checklistItemMapper;
    private final ProjectChecklistDocumentMapper checklistDocumentMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final ApprovalInstanceMapper approvalInstanceMapper;
    private final ProjectArchiveRecordMapper archiveRecordMapper;
    private final ProjectArchiveChecklistSnapshotMapper archiveChecklistSnapshotMapper;
    private final ProjectArchiveDocumentSnapshotMapper archiveDocumentSnapshotMapper;
    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditOperationLogMapper auditOperationLogMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateReqDTO req) {
        UserContext.UserInfo user = requireUser();
        if (CollectionUtils.isEmpty(req.getOwnerUserIds())) {
            throw new BusinessException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }
        validateAssignableUsers(req.getOwnerUserIds(), "项目负责人");
        validateAssignableUsers(req.getMaterialOwnerUserIds(), "资料负责人");
        validateAssignableUsers(req.getMemberUserIds(), "项目成员");

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
        project.setRemark(req.getRemark());
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
        project.setRemark(req.getRemark());
        projectMapper.updateById(project);
        recordAudit(id, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("projectName", project.getProjectName()));
    }

    @Override
    public ProjectRespDTO get(Long id) {
        ProjectEntity project = requireProject(id);
        checkView(project);
        ProjectRespDTO dto = toResp(project);
        enrichProjectSummaries(List.of(dto));
        return dto;
    }

    @Override
    public ProjectWorkbenchRespDTO getWorkbench(Long id) {
        ProjectEntity project = requireProject(id);
        checkView(project);

        List<ProjectChecklistItemEntity> checklistItems = safeList(checklistItemMapper.selectList(
                new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                        .eq(ProjectChecklistItemEntity::getProjectId, id)
                        .eq(ProjectChecklistItemEntity::getDeleted, false)
                        .orderByAsc(ProjectChecklistItemEntity::getSortOrder)
                        .orderByAsc(ProjectChecklistItemEntity::getId)));
        List<Long> itemIds = checklistItems.stream().map(ProjectChecklistItemEntity::getId).toList();
        List<ProjectChecklistDocumentEntity> bindings = itemIds.isEmpty() ? List.of() : safeList(
                checklistDocumentMapper.selectList(new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                        .in(ProjectChecklistDocumentEntity::getChecklistItemId, itemIds)
                        .eq(ProjectChecklistDocumentEntity::getDeleted, false)));
        List<ProjectMemberEntity> members = safeList(projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getProjectId, id)
                        .eq(ProjectMemberEntity::getDeleted, false)
                        .orderByAsc(ProjectMemberEntity::getCreatedAt)));
        List<ApprovalInstanceEntity> approvals = itemIds.isEmpty() ? List.of() : safeList(
                approvalInstanceMapper.selectList(new LambdaQueryWrapper<ApprovalInstanceEntity>()
                        .eq(ApprovalInstanceEntity::getBizType, "CHECKLIST_ITEM")
                        .in(ApprovalInstanceEntity::getBizId, itemIds)
                        .eq(ApprovalInstanceEntity::getDeleted, false)
                        .orderByDesc(ApprovalInstanceEntity::getSubmittedAt)));
        List<ProjectArchiveRecordEntity> archiveRecords = safeList(archiveRecordMapper.selectList(
                new LambdaQueryWrapper<ProjectArchiveRecordEntity>()
                        .eq(ProjectArchiveRecordEntity::getProjectId, id)
                        .eq(ProjectArchiveRecordEntity::getDeleted, false)
                        .orderByDesc(ProjectArchiveRecordEntity::getArchivedAt)
                        .last("LIMIT 5")));
        List<AuditOperationLogEntity> operationLogs = safeList(auditOperationLogMapper.selectList(
                new LambdaQueryWrapper<AuditOperationLogEntity>()
                        .eq(AuditOperationLogEntity::getModuleCode, AuditModuleCodeEnum.PROJECT.getCode())
                        .eq(AuditOperationLogEntity::getBizId, id)
                        .eq(AuditOperationLogEntity::getDeleted, false)
                        .orderByDesc(AuditOperationLogEntity::getOperationTime)
                        .last("LIMIT 10")));

        Map<Long, List<ProjectChecklistDocumentEntity>> bindingsByItem = groupBindingsByItem(bindings);
        Map<Long, ProjectChecklistItemEntity> itemMap = mapChecklistItems(checklistItems);
        Map<Long, DocumentEntity> documentMap = loadDocuments(bindings);
        Set<Long> userIds = collectWorkbenchUserIds(members, checklistItems, approvals, archiveRecords, operationLogs);
        Map<Long, SysUser> userMap = loadUsers(userIds);
        SysDepartment ownerDept = project.getOwnerDeptId() != null ? departmentMapper.selectById(project.getOwnerDeptId()) : null;

        ProjectWorkbenchRespDTO dto = new ProjectWorkbenchRespDTO();
        ProjectWorkbenchRespDTO.ChecklistStatsRespDTO stats = buildChecklistStats(checklistItems, bindingsByItem);
        List<ProjectWorkbenchRespDTO.RiskSummaryRespDTO> risks = buildRiskSummaries(checklistItems, approvals);
        dto.setSummary(buildWorkbenchSummary(project, stats, risks));
        dto.setBasicInfo(buildWorkbenchBasicInfo(project));
        dto.setOrganization(buildOrganization(project, ownerDept, members, userMap));
        dto.setChecklistStats(stats);
        dto.setRiskSummaries(risks);
        dto.setMemberResponsibilities(buildMemberResponsibilities(members, checklistItems, userMap));
        dto.setChecklist(buildChecklistItems(id, checklistItems, bindingsByItem, documentMap, userMap));
        dto.setProjectFiles(buildProjectFiles(checklistItems, bindings, documentMap, itemMap));
        dto.setApprovalSummaries(buildApprovalSummaries(id, approvals, itemMap, userMap));
        dto.setArchiveRecords(archiveRecords.stream().map(this::toArchiveRecordResp).toList());
        dto.setOperationLogs(buildOperationLogs(operationLogs, userMap));
        dto.setTabs(buildWorkbenchTabs(id, ProjectStatusEnum.ARCHIVED.getCode().equals(project.getProjectStatus())));
        return dto;
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
            // 后端权限是安全边界：普通用户只能看到自己参与或负责清单的项目。
            wrapper.and(q -> q.inSql(ProjectEntity::getId,
                            "select project_id from bid_project_member where deleted = false and user_id = " + user.getUserId())
                    .or()
                    .inSql(ProjectEntity::getId,
                            "select project_id from bid_project_checklist_item where deleted = false and owner_user_id = "
                                    + user.getUserId()));
        }
        if (ownerUserId != null) {
            wrapper.inSql(ProjectEntity::getId,
                    "select project_id from bid_project_member where deleted = false and member_role = 'OWNER' and user_id = " + ownerUserId);
        }
        Page<ProjectEntity> result = projectMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<ProjectRespDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ProjectRespDTO> records = result.getRecords().stream().map(this::toResp).toList();
        enrichProjectSummaries(records);
        dtoPage.setRecords(records);
        return PageResponse.of(dtoPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long projectId, ProjectMemberSaveReqDTO req) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        ensureEditable(project);
        String role = StringUtils.hasText(req.getMemberRole()) ? req.getMemberRole() : ProjectMemberRoleEnum.MEMBER.getCode();
        validateAssignableUsers(req.getUserIds(), "项目成员");
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
        validateProjectStage(projectStage);
        project.setProjectStage(projectStage);
        projectMapper.updateById(project);
        recordAudit(projectId, AuditOperationTypeEnum.UPDATE.getCode(), Map.of("projectStage", projectStage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long projectId, String projectStatus) {
        ProjectEntity project = requireProject(projectId);
        checkManage(project);
        validateProjectStatus(projectStatus);
        if (ProjectStatusEnum.ARCHIVED.getCode().equals(projectStatus)) {
            ensureEditable(project);
            archiveProject(project);
        } else {
            ensureEditable(project);
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

    @Override
    public ProjectArchiveDetailRespDTO getArchiveDetail(Long projectId) {
        ProjectEntity project = requireProject(projectId);
        checkView(project);
        ProjectArchiveRecordEntity record = archiveRecordMapper.selectOne(
                new LambdaQueryWrapper<ProjectArchiveRecordEntity>()
                        .eq(ProjectArchiveRecordEntity::getProjectId, projectId)
                        .eq(ProjectArchiveRecordEntity::getDeleted, false)
                        .orderByDesc(ProjectArchiveRecordEntity::getArchivedAt)
                        .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "项目归档记录不存在");
        }

        ProjectArchiveDetailRespDTO dto = new ProjectArchiveDetailRespDTO();
        dto.setRecord(toArchiveRecordResp(record));
        dto.setChecklistSnapshots(archiveChecklistSnapshotMapper.selectList(
                        new LambdaQueryWrapper<ProjectArchiveChecklistSnapshotEntity>()
                                .eq(ProjectArchiveChecklistSnapshotEntity::getArchiveRecordId, record.getId())
                                .eq(ProjectArchiveChecklistSnapshotEntity::getDeleted, false))
                .stream().map(this::toChecklistSnapshotResp).toList());
        dto.setDocumentSnapshots(archiveDocumentSnapshotMapper.selectList(
                        new LambdaQueryWrapper<ProjectArchiveDocumentSnapshotEntity>()
                                .eq(ProjectArchiveDocumentSnapshotEntity::getArchiveRecordId, record.getId())
                                .eq(ProjectArchiveDocumentSnapshotEntity::getDeleted, false))
                .stream().map(this::toDocumentSnapshotResp).toList());
        return dto;
    }

    private ProjectWorkbenchRespDTO.SummaryRespDTO buildWorkbenchSummary(
            ProjectEntity project,
            ProjectWorkbenchRespDTO.ChecklistStatsRespDTO stats,
            List<ProjectWorkbenchRespDTO.RiskSummaryRespDTO> risks) {
        ProjectWorkbenchRespDTO.SummaryRespDTO summary = new ProjectWorkbenchRespDTO.SummaryRespDTO();
        summary.setProjectId(project.getId());
        summary.setProjectNo(project.getProjectNo());
        summary.setProjectName(project.getProjectName());
        summary.setProjectStage(project.getProjectStage());
        summary.setProjectStatus(project.getProjectStatus());
        summary.setBidDeadline(project.getBidDeadline());
        summary.setChecklistCompletionRate(stats.getTotal() == null || stats.getTotal() == 0
                ? 0
                : (int) Math.round(stats.getComplete() * 100.0 / stats.getTotal()));
        summary.setRiskCount(risks.stream().mapToInt(ProjectWorkbenchRespDTO.RiskSummaryRespDTO::getCount).sum());
        boolean readOnly = ProjectStatusEnum.ARCHIVED.getCode().equals(project.getProjectStatus());
        summary.setReadOnly(readOnly);
        summary.setPrimaryActions(buildPrimaryActions(project, readOnly));
        return summary;
    }

    private ProjectWorkbenchRespDTO.BasicInfoRespDTO buildWorkbenchBasicInfo(ProjectEntity project) {
        ProjectWorkbenchRespDTO.BasicInfoRespDTO basicInfo = new ProjectWorkbenchRespDTO.BasicInfoRespDTO();
        basicInfo.setId(project.getId());
        basicInfo.setProjectNo(project.getProjectNo());
        basicInfo.setProjectName(project.getProjectName());
        basicInfo.setTenderUnit(project.getTenderUnit());
        basicInfo.setProjectType(project.getProjectType());
        basicInfo.setProjectStage(project.getProjectStage());
        basicInfo.setProjectStatus(project.getProjectStatus());
        basicInfo.setBidDeadline(project.getBidDeadline());
        basicInfo.setRemark(project.getRemark());
        return basicInfo;
    }

    private ProjectWorkbenchRespDTO.OrganizationRespDTO buildOrganization(ProjectEntity project,
                                                                          SysDepartment ownerDept,
                                                                          List<ProjectMemberEntity> members,
                                                                          Map<Long, SysUser> userMap) {
        ProjectWorkbenchRespDTO.OrganizationRespDTO organization = new ProjectWorkbenchRespDTO.OrganizationRespDTO();
        organization.setOwnerDeptId(project.getOwnerDeptId());
        organization.setOwnerDeptName(ownerDept != null ? ownerDept.getName() : null);
        Object abbr = ownerDept != null && ownerDept.getExtensionData() != null
                ? ownerDept.getExtensionData().get("abbr")
                : null;
        organization.setBusinessUnit(abbr != null ? String.valueOf(abbr) : null);
        organization.setOwners(members.stream()
                .filter(member -> ProjectMemberRoleEnum.OWNER.getCode().equals(member.getMemberRole()))
                .map(member -> userBrief(member.getUserId(), userMap))
                .toList());
        organization.setMaterialOwners(members.stream()
                .filter(member -> ProjectMemberRoleEnum.MATERIAL_OWNER.getCode().equals(member.getMemberRole()))
                .map(member -> userBrief(member.getUserId(), userMap))
                .toList());
        organization.setParticipantCount((int) members.stream().map(ProjectMemberEntity::getUserId).distinct().count());
        return organization;
    }

    private ProjectWorkbenchRespDTO.ChecklistStatsRespDTO buildChecklistStats(
            List<ProjectChecklistItemEntity> items,
            Map<Long, List<ProjectChecklistDocumentEntity>> bindingsByItem) {
        ProjectWorkbenchRespDTO.ChecklistStatsRespDTO stats = new ProjectWorkbenchRespDTO.ChecklistStatsRespDTO();
        stats.setTotal(items.size());
        stats.setComplete((int) items.stream().filter(item -> isCompleteStatus(item.getStatus())).count());
        stats.setPendingCollect(countStatus(items, ChecklistItemStatusEnum.PENDING_COLLECT.getCode()));
        stats.setPendingReview(countStatus(items, ChecklistItemStatusEnum.PENDING_REVIEW.getCode()));
        stats.setNeedSupplement(countStatus(items, ChecklistItemStatusEnum.NEED_SUPPLEMENT.getCode()));
        stats.setArchived(countStatus(items, ChecklistItemStatusEnum.ARCHIVED.getCode()));
        stats.setOverdue((int) items.stream().filter(this::isOverdue).count());
        stats.setBoundDocumentCount(bindingsByItem.values().stream().mapToInt(List::size).sum());
        return stats;
    }

    private List<ProjectWorkbenchRespDTO.RiskSummaryRespDTO> buildRiskSummaries(
            List<ProjectChecklistItemEntity> items,
            List<ApprovalInstanceEntity> approvals) {
        int overdue = (int) items.stream().filter(this::isOverdue).count();
        int needSupplement = countStatus(items, ChecklistItemStatusEnum.NEED_SUPPLEMENT.getCode());
        int pendingApproval = (int) approvals.stream()
                .filter(instance -> "PENDING".equals(instance.getStatus()))
                .count();
        return List.of(
                risk("OVERDUE_CHECKLIST", "清单逾期", "HIGH", overdue),
                risk("NEED_SUPPLEMENT", "需补充资料", "MEDIUM", needSupplement),
                risk("PENDING_APPROVAL", "待处理审批", "MEDIUM", pendingApproval)
        );
    }

    private ProjectWorkbenchRespDTO.RiskSummaryRespDTO risk(String code, String name, String level, int count) {
        ProjectWorkbenchRespDTO.RiskSummaryRespDTO risk = new ProjectWorkbenchRespDTO.RiskSummaryRespDTO();
        risk.setCode(code);
        risk.setName(name);
        risk.setLevel(level);
        risk.setCount(count);
        return risk;
    }

    private List<ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO> buildMemberResponsibilities(
            List<ProjectMemberEntity> members,
            List<ProjectChecklistItemEntity> checklistItems,
            Map<Long, SysUser> userMap) {
        Map<Long, ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO> responsibilityMap = new LinkedHashMap<>();
        for (ProjectMemberEntity member : members) {
            ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO resp =
                    responsibilityMap.computeIfAbsent(member.getUserId(), userId -> memberResponsibility(userId, userMap));
            if (member.getMemberRole() != null && !resp.getMemberRoles().contains(member.getMemberRole())) {
                resp.getMemberRoles().add(member.getMemberRole());
            }
        }
        for (ProjectChecklistItemEntity item : checklistItems) {
            if (item.getOwnerUserId() == null) {
                continue;
            }
            ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO resp =
                    responsibilityMap.computeIfAbsent(item.getOwnerUserId(), userId -> memberResponsibility(userId, userMap));
            resp.setChecklistOwner(Boolean.TRUE);
            resp.setResponsibleChecklistCount(resp.getResponsibleChecklistCount() + 1);
            if (isCompleteStatus(item.getStatus())) {
                resp.setCompleteCount(resp.getCompleteCount() + 1);
            }
            if (ChecklistItemStatusEnum.PENDING_COLLECT.getCode().equals(item.getStatus())) {
                resp.setPendingCollectCount(resp.getPendingCollectCount() + 1);
            }
            if (ChecklistItemStatusEnum.NEED_SUPPLEMENT.getCode().equals(item.getStatus())) {
                resp.setNeedSupplementCount(resp.getNeedSupplementCount() + 1);
            }
            if (isOverdue(item)) {
                resp.setOverdueCount(resp.getOverdueCount() + 1);
            }
            resp.setRecentProcessedAt(latest(resp.getRecentProcessedAt(), item.getUpdatedAt()));
        }
        return new ArrayList<>(responsibilityMap.values());
    }

    private ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO memberResponsibility(Long userId,
                                                                                    Map<Long, SysUser> userMap) {
        ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO resp = new ProjectWorkbenchRespDTO.MemberResponsibilityRespDTO();
        resp.setUserId(userId);
        resp.setUserName(displayName(userMap.get(userId)));
        resp.setChecklistOwner(Boolean.FALSE);
        resp.setResponsibleChecklistCount(0);
        resp.setCompleteCount(0);
        resp.setPendingCollectCount(0);
        resp.setNeedSupplementCount(0);
        resp.setOverdueCount(0);
        return resp;
    }

    private List<ProjectWorkbenchRespDTO.ChecklistItemRespDTO> buildChecklistItems(
            Long projectId,
            List<ProjectChecklistItemEntity> checklistItems,
            Map<Long, List<ProjectChecklistDocumentEntity>> bindingsByItem,
            Map<Long, DocumentEntity> documentMap,
            Map<Long, SysUser> userMap) {
        List<ProjectWorkbenchRespDTO.ChecklistItemRespDTO> result = new ArrayList<>();
        for (ProjectChecklistItemEntity item : checklistItems) {
            List<ProjectChecklistDocumentEntity> itemBindings = bindingsByItem.getOrDefault(item.getId(), List.of());
            ProjectWorkbenchRespDTO.ChecklistItemRespDTO resp = new ProjectWorkbenchRespDTO.ChecklistItemRespDTO();
            resp.setId(item.getId());
            resp.setItemName(item.getItemName());
            resp.setBusinessCategory(item.getBusinessCategory());
            resp.setTenderStructureCategory(item.getTenderStructureCategory());
            resp.setRequired(item.getRequired());
            resp.setMinCount(item.getMinCount());
            resp.setMaxCount(item.getMaxCount());
            resp.setBoundDocumentCount(itemBindings.size());
            resp.setStatus(item.getStatus());
            resp.setOwnerUserId(item.getOwnerUserId());
            resp.setOwnerName(displayName(userMap.get(item.getOwnerUserId())));
            resp.setDeadline(item.getDeadline());
            resp.setOverdue(isOverdue(item));
            if (Boolean.TRUE.equals(resp.getOverdue())) {
                resp.getRiskTips().add("清单项已逾期");
            }
            if (ChecklistItemStatusEnum.NEED_SUPPLEMENT.getCode().equals(item.getStatus())) {
                resp.getRiskTips().add("资料需补充");
            }
            resp.setApprovalEntryPath("/api/v1/projects/" + projectId + "/checklist/items/" + item.getId()
                    + "/approval/submit");
            resp.setBoundDocuments(itemBindings.stream()
                    .map(binding -> boundDocument(binding, documentMap.get(binding.getDocumentId())))
                    .toList());
            result.add(resp);
        }
        return result;
    }

    private ProjectWorkbenchRespDTO.BoundDocumentRespDTO boundDocument(ProjectChecklistDocumentEntity binding,
                                                                       DocumentEntity document) {
        ProjectWorkbenchRespDTO.BoundDocumentRespDTO resp = new ProjectWorkbenchRespDTO.BoundDocumentRespDTO();
        resp.setDocumentId(binding.getDocumentId());
        resp.setVersionNo(binding.getVersionNo());
        if (document != null) {
            resp.setDocumentName(document.getName());
            resp.setDocumentNo(document.getDocumentNo());
            resp.setDocumentStatus(document.getDocumentStatus());
        }
        return resp;
    }

    private List<ProjectWorkbenchRespDTO.ProjectFileRespDTO> buildProjectFiles(
            List<ProjectChecklistItemEntity> checklistItems,
            List<ProjectChecklistDocumentEntity> bindings,
            Map<Long, DocumentEntity> documentMap,
            Map<Long, ProjectChecklistItemEntity> itemMap) {
        List<ProjectWorkbenchRespDTO.ProjectFileRespDTO> result = new ArrayList<>();
        Set<String> relationKeys = new LinkedHashSet<>();
        for (ProjectChecklistDocumentEntity binding : bindings) {
            String relationKey = binding.getChecklistItemId() + ":" + binding.getDocumentId() + ":" + binding.getVersionNo();
            if (!relationKeys.add(relationKey)) {
                continue;
            }
            DocumentEntity document = documentMap.get(binding.getDocumentId());
            ProjectChecklistItemEntity item = itemMap.get(binding.getChecklistItemId());
            ProjectWorkbenchRespDTO.ProjectFileRespDTO resp = new ProjectWorkbenchRespDTO.ProjectFileRespDTO();
            resp.setDocumentId(binding.getDocumentId());
            resp.setBoundVersionNo(binding.getVersionNo());
            if (document != null) {
                resp.setDocumentName(document.getName());
                resp.setDocumentNo(document.getDocumentNo());
                resp.setDocumentStatus(document.getDocumentStatus());
                resp.setCurrentVersionNo(document.getCurrentVersionNo());
            }
            if (item != null) {
                resp.setChecklistItemId(item.getId());
                resp.setChecklistItemName(item.getItemName());
            }
            result.add(resp);
        }
        return result;
    }

    private List<ProjectWorkbenchRespDTO.ApprovalSummaryRespDTO> buildApprovalSummaries(
            Long projectId,
            List<ApprovalInstanceEntity> approvals,
            Map<Long, ProjectChecklistItemEntity> itemMap,
            Map<Long, SysUser> userMap) {
        return approvals.stream().map(instance -> {
            ProjectChecklistItemEntity item = itemMap.get(instance.getBizId());
            ProjectWorkbenchRespDTO.ApprovalSummaryRespDTO resp = new ProjectWorkbenchRespDTO.ApprovalSummaryRespDTO();
            resp.setInstanceId(instance.getId());
            resp.setBizType(instance.getBizType());
            resp.setBizId(instance.getBizId());
            resp.setChecklistItemId(instance.getBizId());
            resp.setChecklistItemName(item != null ? item.getItemName() : null);
            resp.setStatus(instance.getStatus());
            resp.setSubmitterUserId(instance.getSubmitterUserId());
            resp.setSubmitterName(displayName(userMap.get(instance.getSubmitterUserId())));
            resp.setSubmittedAt(instance.getSubmittedAt());
            resp.setCompletedAt(instance.getCompletedAt());
            resp.setEntryPath("/api/v1/projects/" + projectId + "/approval/history");
            return resp;
        }).toList();
    }

    private List<ProjectWorkbenchRespDTO.OperationLogRespDTO> buildOperationLogs(
            List<AuditOperationLogEntity> logs,
            Map<Long, SysUser> userMap) {
        return logs.stream().map(log -> {
            ProjectWorkbenchRespDTO.OperationLogRespDTO resp = new ProjectWorkbenchRespDTO.OperationLogRespDTO();
            resp.setId(log.getId());
            resp.setOperationType(log.getOperationType());
            resp.setOperatorUserId(log.getOperatorUserId());
            resp.setOperatorName(displayName(userMap.get(log.getOperatorUserId())));
            resp.setOperationTime(log.getOperationTime());
            return resp;
        }).toList();
    }

    private List<ProjectWorkbenchRespDTO.TabRespDTO> buildWorkbenchTabs(Long projectId, boolean readOnly) {
        return List.of(
                tab("OVERVIEW", "概览", readOnly, "/api/v1/projects/" + projectId + "/workbench"),
                tab("PROJECT_BASIC_INFO", "项目基本信息", readOnly, "/api/v1/projects/" + projectId),
                tab("MEMBER_RESPONSIBILITIES", "成员职责", readOnly, "/api/v1/projects/" + projectId + "/members"),
                tab("CHECKLIST", "资料清单", readOnly, "/api/v1/projects/" + projectId + "/checklist"),
                tab("PROJECT_FILES", "项目文件", readOnly, "/api/v1/projects/" + projectId + "/workbench"),
                tab("RELATED_APPROVALS", "关联审批", true, "/api/v1/projects/" + projectId + "/approval/history"),
                tab("ARCHIVE_RECORDS", "归档记录", true, "/api/v1/projects/" + projectId + "/archive"),
                tab("OPERATION_LOGS", "操作日志", true, "/api/v1/audit/logs")
        );
    }

    private ProjectWorkbenchRespDTO.TabRespDTO tab(String code, String label, boolean readOnly, String entryPath) {
        ProjectWorkbenchRespDTO.TabRespDTO tab = new ProjectWorkbenchRespDTO.TabRespDTO();
        tab.setCode(code);
        tab.setLabel(label);
        tab.setReadOnly(readOnly);
        tab.setEntryPath(entryPath);
        return tab;
    }

    private List<String> buildPrimaryActions(ProjectEntity project, boolean readOnly) {
        if (readOnly) {
            return List.of("VIEW_ARCHIVE", "VIEW_RELATED_APPROVALS", "VIEW_OPERATION_LOGS");
        }
        UserContext.UserInfo user = requireUser();
        List<String> actions = new ArrayList<>();
        actions.add("VIEW_PROJECT_FILES");
        actions.add("VIEW_RELATED_APPROVALS");
        if (user.isSuperAdmin() || isProjectOwner(project.getId(), user.getUserId())) {
            actions.add("UPDATE_PROJECT");
            actions.add("MANAGE_MEMBERS");
            actions.add("GENERATE_CHECKLIST");
            actions.add("ARCHIVE_PROJECT");
        }
        return actions;
    }

    private ProjectWorkbenchRespDTO.UserBriefRespDTO userBrief(Long userId, Map<Long, SysUser> userMap) {
        ProjectWorkbenchRespDTO.UserBriefRespDTO user = new ProjectWorkbenchRespDTO.UserBriefRespDTO();
        user.setUserId(userId);
        user.setUserName(displayName(userMap.get(userId)));
        return user;
    }

    private Map<Long, List<ProjectChecklistDocumentEntity>> groupBindingsByItem(
            List<ProjectChecklistDocumentEntity> bindings) {
        Map<Long, List<ProjectChecklistDocumentEntity>> result = new LinkedHashMap<>();
        for (ProjectChecklistDocumentEntity binding : bindings) {
            result.computeIfAbsent(binding.getChecklistItemId(), key -> new ArrayList<>()).add(binding);
        }
        return result;
    }

    private Map<Long, ProjectChecklistItemEntity> mapChecklistItems(List<ProjectChecklistItemEntity> items) {
        Map<Long, ProjectChecklistItemEntity> result = new HashMap<>();
        for (ProjectChecklistItemEntity item : items) {
            result.put(item.getId(), item);
        }
        return result;
    }

    private Map<Long, DocumentEntity> loadDocuments(List<ProjectChecklistDocumentEntity> bindings) {
        Set<Long> documentIds = new LinkedHashSet<>();
        for (ProjectChecklistDocumentEntity binding : bindings) {
            if (binding.getDocumentId() != null) {
                documentIds.add(binding.getDocumentId());
            }
        }
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DocumentEntity> result = new HashMap<>();
        for (DocumentEntity document : safeList(documentMapper.selectBatchIds(documentIds))) {
            result.put(document.getId(), document);
        }
        return result;
    }

    private Set<Long> collectWorkbenchUserIds(List<ProjectMemberEntity> members,
                                              List<ProjectChecklistItemEntity> checklistItems,
                                              List<ApprovalInstanceEntity> approvals,
                                              List<ProjectArchiveRecordEntity> archiveRecords,
                                              List<AuditOperationLogEntity> operationLogs) {
        Set<Long> userIds = new LinkedHashSet<>();
        members.stream().map(ProjectMemberEntity::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        checklistItems.stream().map(ProjectChecklistItemEntity::getOwnerUserId).filter(Objects::nonNull).forEach(userIds::add);
        approvals.stream().map(ApprovalInstanceEntity::getSubmitterUserId).filter(Objects::nonNull).forEach(userIds::add);
        archiveRecords.stream().map(ProjectArchiveRecordEntity::getArchivedBy).filter(Objects::nonNull).forEach(userIds::add);
        operationLogs.stream().map(AuditOperationLogEntity::getOperatorUserId).filter(Objects::nonNull).forEach(userIds::add);
        return userIds;
    }

    private Map<Long, SysUser> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> result = new HashMap<>();
        for (SysUser user : safeList(sysUserMapper.selectBatchIds(userIds))) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private int countStatus(List<ProjectChecklistItemEntity> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private boolean isCompleteStatus(String status) {
        return ChecklistItemStatusEnum.COMPLETE.getCode().equals(status)
                || ChecklistItemStatusEnum.ARCHIVED.getCode().equals(status);
    }

    private boolean isOverdue(ProjectChecklistItemEntity item) {
        return item.getDeadline() != null
                && item.getDeadline().isBefore(OffsetDateTime.now())
                && !isCompleteStatus(item.getStatus());
    }

    private OffsetDateTime latest(OffsetDateTime left, OffsetDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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

    private void validateAssignableUsers(Iterable<Long> userIds, String roleName) {
        if (userIds == null) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>();
        userIds.forEach(distinct::add);
        for (Long userId : distinct) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, roleName + "不能为空");
            }
            SysUser user = sysUserMapper.selectById(userId);
            // 负责人和项目成员会获得后续资料、清单、审批权限，必须先排除停用或已删除账号。
            if (user == null || Boolean.TRUE.equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, roleName + "账号不可用");
            }
        }
    }

    private void validateProjectStage(String projectStage) {
        boolean matched = false;
        for (ProjectStageEnum stage : ProjectStageEnum.values()) {
            if (stage.getCode().equals(projectStage)) {
                matched = true;
                break;
            }
        }
        // 当前仓库只定义了合法枚举，尚未沉淀完整流转矩阵；先拒绝未知值，避免任意字符串污染项目状态。
        if (!matched) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "项目阶段非法");
        }
    }

    private void validateProjectStatus(String projectStatus) {
        boolean matched = false;
        for (ProjectStatusEnum status : ProjectStatusEnum.values()) {
            if (status.getCode().equals(projectStatus)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "项目状态非法");
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
        if (user.isSuperAdmin()
                || isProjectMember(project.getId(), user.getUserId())
                || isChecklistOwner(project.getId(), user.getUserId())) {
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

    private boolean isChecklistOwner(Long projectId, Long userId) {
        // 清单责任人需要进入项目工作台查看上下文，但不因此获得项目成员维护权限。
        return checklistItemMapper.selectCount(new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                .eq(ProjectChecklistItemEntity::getOwnerUserId, userId)
                .eq(ProjectChecklistItemEntity::getDeleted, false)) > 0;
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

    private void archiveProject(ProjectEntity project) {
        ensureCanArchive(project.getId());
        ArchiveSnapshot snapshot = buildArchiveSnapshot(project.getId());
        ProjectArchiveRecordEntity record = createArchiveRecord(project, snapshot);
        archiveRecordMapper.insert(record);

        // 归档快照必须先固化历史视图，再更新项目和清单状态；整个方法在外层事务中执行，任一步失败都会回滚。
        for (ChecklistSnapshotSource item : snapshot.items()) {
            archiveChecklistSnapshotMapper.insert(toChecklistSnapshot(record.getId(), project.getId(), item));
        }
        for (DocumentSnapshotSource document : snapshot.documents()) {
            archiveDocumentSnapshotMapper.insert(toDocumentSnapshot(record.getId(), project.getId(), document));
        }
        archiveChecklist(project.getId());
    }

    private ArchiveSnapshot buildArchiveSnapshot(Long projectId) {
        List<ProjectChecklistItemEntity> items = checklistItemMapper.selectList(
                new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                        .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                        .eq(ProjectChecklistItemEntity::getDeleted, false)
                        .orderByAsc(ProjectChecklistItemEntity::getSortOrder)
                        .orderByAsc(ProjectChecklistItemEntity::getId));
        List<ChecklistSnapshotSource> checklistSnapshots = new ArrayList<>();
        List<DocumentSnapshotSource> documentSnapshots = new ArrayList<>();
        for (ProjectChecklistItemEntity item : items) {
            List<ProjectChecklistDocumentEntity> bindings = checklistDocumentMapper.selectList(
                    new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                            .eq(ProjectChecklistDocumentEntity::getChecklistItemId, item.getId())
                            .eq(ProjectChecklistDocumentEntity::getDeleted, false));
            checklistSnapshots.add(new ChecklistSnapshotSource(item, bindings.size()));
            for (ProjectChecklistDocumentEntity binding : bindings) {
                documentSnapshots.add(buildDocumentSnapshot(item, binding));
            }
        }
        if (documentSnapshots.stream().anyMatch(DocumentSnapshotSource::hasPendingApproval)) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "项目存在审批中的资料或版本，不能归档");
        }
        return new ArchiveSnapshot(checklistSnapshots, documentSnapshots);
    }

    private DocumentSnapshotSource buildDocumentSnapshot(ProjectChecklistItemEntity item,
                                                         ProjectChecklistDocumentEntity binding) {
        DocumentEntity document = documentMapper.selectById(binding.getDocumentId());
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        if ("VOIDED".equals(document.getDocumentStatus()) || "DELETED".equals(document.getDocumentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "作废或删除资料不能归档");
        }
        if (isExpired(document)) {
            throw new BusinessException(ErrorCode.DOCUMENT_EXPIRED);
        }
        Integer versionNo = binding.getVersionNo() != null ? binding.getVersionNo() : document.getCurrentVersionNo();
        DocumentVersionEntity version = documentVersionMapper.selectOne(
                new LambdaQueryWrapper<DocumentVersionEntity>()
                        .eq(DocumentVersionEntity::getDocumentId, document.getId())
                        .eq(DocumentVersionEntity::getVersionNo, versionNo)
                        .eq(DocumentVersionEntity::getDeleted, false));
        if (version == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }
        if (!"APPROVED".equals(document.getDocumentStatus()) || !"APPROVED".equals(version.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "资料或版本未审批通过，不能归档");
        }
        boolean pendingApproval = hasPendingApproval(item.getId(), document.getId(), versionNo);
        return new DocumentSnapshotSource(item, binding, document, version, versionNo, pendingApproval);
    }

    private boolean hasPendingApproval(Long checklistItemId, Long documentId, Integer versionNo) {
        Long documentApprovalCount = approvalInstanceMapper.selectCount(new LambdaQueryWrapper<ApprovalInstanceEntity>()
                .eq(ApprovalInstanceEntity::getStatus, "PENDING")
                .eq(ApprovalInstanceEntity::getDeleted, false)
                .and(q -> q
                        .eq(ApprovalInstanceEntity::getDocumentId, documentId)
                        .or(w -> w.eq(ApprovalInstanceEntity::getBizId, documentId)
                                .in(ApprovalInstanceEntity::getBizType, List.of("DOCUMENT", "DOCUMENT_VERSION")))
                        .or(w -> w.eq(ApprovalInstanceEntity::getBizId, checklistItemId)
                                .eq(ApprovalInstanceEntity::getBizType, "CHECKLIST_ITEM"))));
        if (documentApprovalCount != null && documentApprovalCount > 0) {
            return true;
        }
        return versionNo != null && approvalInstanceMapper.selectCount(new LambdaQueryWrapper<ApprovalInstanceEntity>()
                .eq(ApprovalInstanceEntity::getStatus, "PENDING")
                .eq(ApprovalInstanceEntity::getDeleted, false)
                .eq(ApprovalInstanceEntity::getDocumentId, documentId)
                .eq(ApprovalInstanceEntity::getVersionNo, versionNo)) > 0;
    }

    private ProjectArchiveRecordEntity createArchiveRecord(ProjectEntity project, ArchiveSnapshot snapshot) {
        UserContext.UserInfo user = requireUser();
        ProjectArchiveRecordEntity record = new ProjectArchiveRecordEntity();
        record.setId(IdWorker.getId());
        record.setProjectId(project.getId());
        record.setArchiveNo("ARCH-" + project.getProjectNo());
        record.setArchiveStatus(ProjectStatusEnum.ARCHIVED.getCode());
        record.setArchivedAt(OffsetDateTime.now());
        record.setArchivedBy(user.getUserId());
        record.setArchiveReason("项目归档");
        record.setChecklistTotal(snapshot.items().size());
        record.setChecklistComplete((int) snapshot.items().stream()
                .filter(item -> ChecklistItemStatusEnum.COMPLETE.getCode().equals(item.item().getStatus()))
                .count());
        record.setDocumentTotal(snapshot.documents().size());
        record.setSnapshotHash(snapshotHash(snapshot));
        record.setDeleted(Boolean.FALSE);
        return record;
    }

    private ProjectArchiveChecklistSnapshotEntity toChecklistSnapshot(Long archiveRecordId, Long projectId,
                                                                      ChecklistSnapshotSource source) {
        ProjectChecklistItemEntity item = source.item();
        ProjectArchiveChecklistSnapshotEntity snapshot = new ProjectArchiveChecklistSnapshotEntity();
        snapshot.setId(IdWorker.getId());
        snapshot.setArchiveRecordId(archiveRecordId);
        snapshot.setProjectId(projectId);
        snapshot.setChecklistItemId(item.getId());
        snapshot.setTemplateItemId(item.getTemplateItemId());
        snapshot.setItemName(item.getItemName());
        snapshot.setRequiredFlag(Boolean.TRUE.equals(item.getRequired()));
        snapshot.setItemStatus(ChecklistItemStatusEnum.ARCHIVED.getCode());
        snapshot.setBoundDocumentCount(source.boundDocumentCount());
        snapshot.setSnapshotJson(json(Map.of(
                "originalStatus", item.getStatus(),
                "businessCategory", nullToEmpty(item.getBusinessCategory()),
                "tenderStructureCategory", nullToEmpty(item.getTenderStructureCategory()),
                "ownerUserId", item.getOwnerUserId() == null ? "" : String.valueOf(item.getOwnerUserId()),
                "minCount", item.getMinCount() == null ? "" : String.valueOf(item.getMinCount()),
                "maxCount", item.getMaxCount() == null ? "" : String.valueOf(item.getMaxCount())
        )));
        snapshot.setDeleted(Boolean.FALSE);
        return snapshot;
    }

    private ProjectArchiveDocumentSnapshotEntity toDocumentSnapshot(Long archiveRecordId, Long projectId,
                                                                    DocumentSnapshotSource source) {
        DocumentEntity document = source.document();
        DocumentVersionEntity version = source.version();
        ProjectArchiveDocumentSnapshotEntity snapshot = new ProjectArchiveDocumentSnapshotEntity();
        snapshot.setId(IdWorker.getId());
        snapshot.setArchiveRecordId(archiveRecordId);
        snapshot.setProjectId(projectId);
        snapshot.setChecklistItemId(source.item().getId());
        snapshot.setDocumentId(document.getId());
        snapshot.setVersionNo(source.versionNo());
        snapshot.setDocumentName(document.getName());
        snapshot.setDocumentStatus(document.getDocumentStatus());
        snapshot.setVersionStatus(version.getApprovalStatus());
        snapshot.setExpireAt(document.getExpireDate());
        snapshot.setStorageType("SYSTEM");
        snapshot.setFileSize(version.getSize());
        snapshot.setSnapshotJson(json(Map.of(
                "documentNo", nullToEmpty(document.getDocumentNo()),
                "businessCategory", nullToEmpty(document.getBusinessCategory()),
                "sensitiveLevel", nullToEmpty(document.getSensitiveLevel()),
                "currentVersionNo", document.getCurrentVersionNo() == null ? "" : document.getCurrentVersionNo(),
                "mimeType", nullToEmpty(version.getMimeType()),
                "originalFilename", nullToEmpty(version.getOriginalFilename())
        )));
        snapshot.setDeleted(Boolean.FALSE);
        return snapshot;
    }

    private void archiveChecklist(Long projectId) {
        ProjectChecklistItemEntity update = new ProjectChecklistItemEntity();
        update.setStatus(ChecklistItemStatusEnum.ARCHIVED.getCode());
        update.setUpdatedAt(OffsetDateTime.now());
        checklistItemMapper.update(update, new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                .eq(ProjectChecklistItemEntity::getProjectId, projectId)
                .eq(ProjectChecklistItemEntity::getDeleted, false));
    }

    private boolean isExpired(DocumentEntity document) {
        return Boolean.TRUE.equals(document.getHasExpireDate())
                && document.getExpireDate() != null
                && document.getExpireDate().isBefore(OffsetDateTime.now());
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
        ProjectEntity project = projectMapper.selectById(projectId);
        String projectName = project != null && project.getProjectName() != null
                ? project.getProjectName()
                : String.valueOf(projectId);
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.PROJECT.getCode())
                .bizType("PROJECT")
                .bizId(projectId)
                .operationType(operation)
                .objectName(projectName)
                .actionSummary(currentActorName() + " " + projectActionName(operation) + "项目《" + projectName + "》")
                .afterData(afterData)
                .build());
    }

    private String projectActionName(String operation) {
        if (AuditOperationTypeEnum.CREATE.getCode().equals(operation)) {
            return "创建了";
        }
        if (AuditOperationTypeEnum.DELETE.getCode().equals(operation)) {
            return "删除了";
        }
        return "更新了";
    }

    private String currentActorName() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            return "系统";
        }
        return user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : String.valueOf(user.getUserId());
    }

    private String snapshotHash(ArchiveSnapshot snapshot) {
        List<String> values = new ArrayList<>();
        snapshot.items().stream()
                .sorted(Comparator.comparing(source -> source.item().getId()))
                .forEach(source -> values.add("I:" + source.item().getId() + ":" + source.item().getStatus()
                        + ":" + source.boundDocumentCount()));
        snapshot.documents().stream()
                .sorted(Comparator.comparing(source -> source.document().getId()))
                .forEach(source -> values.add("D:" + source.document().getId() + ":" + source.versionNo()
                        + ":" + source.document().getDocumentStatus() + ":" + source.version().getApprovalStatus()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.join("|", values).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "归档快照哈希生成失败");
        }
    }

    private String json(Map<String, ?> data) {
        try {
            return SNAPSHOT_MAPPER.writeValueAsString(new LinkedHashMap<>(data));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "归档快照序列化失败");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void enrichProjectSummaries(List<ProjectRespDTO> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        List<Long> projectIds = projects.stream().map(ProjectRespDTO::getId).toList();
        Map<Long, ProjectRespDTO> dtoMap = new HashMap<>();
        Set<Long> deptIds = new LinkedHashSet<>();
        for (ProjectRespDTO project : projects) {
            dtoMap.put(project.getId(), project);
            if (project.getOwnerDeptId() != null) {
                deptIds.add(project.getOwnerDeptId());
            }
            project.setMemberCount(0L);
            project.setDocumentCount(0L);
            project.setChecklistProgress(checklistProgress(0L, 0L));
        }

        Map<Long, SysDepartment> departmentMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (SysDepartment department : safeList(departmentMapper.selectBatchIds(deptIds))) {
                departmentMap.put(department.getId(), department);
            }
        }
        for (ProjectRespDTO project : projects) {
            SysDepartment department = departmentMap.get(project.getOwnerDeptId());
            project.setOwnerDeptName(department == null ? null : department.getName());
        }

        List<ProjectMemberEntity> members = safeList(projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .in(ProjectMemberEntity::getProjectId, projectIds)
                        .eq(ProjectMemberEntity::getDeleted, false)));
        Map<Long, Set<Long>> memberUserIds = new HashMap<>();
        Map<Long, List<Long>> ownerUserIds = new HashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (ProjectMemberEntity member : members) {
            memberUserIds.computeIfAbsent(member.getProjectId(), key -> new LinkedHashSet<>()).add(member.getUserId());
            if (ProjectMemberRoleEnum.OWNER.getCode().equals(member.getMemberRole())) {
                ownerUserIds.computeIfAbsent(member.getProjectId(), key -> new ArrayList<>()).add(member.getUserId());
                userIds.add(member.getUserId());
            }
        }
        Map<Long, SysUser> userMap = loadUsers(userIds);
        for (ProjectRespDTO project : projects) {
            project.setMemberCount((long) memberUserIds.getOrDefault(project.getId(), Set.of()).size());
            project.setOwnerUserName(joinOwnerNames(ownerUserIds.get(project.getId()), userMap));
        }

        List<ProjectChecklistItemEntity> checklistItems = safeList(checklistItemMapper.selectList(
                new LambdaQueryWrapper<ProjectChecklistItemEntity>()
                        .in(ProjectChecklistItemEntity::getProjectId, projectIds)
                        .eq(ProjectChecklistItemEntity::getDeleted, false)));
        Map<Long, Long> checklistTotal = new HashMap<>();
        Map<Long, Long> checklistCompleted = new HashMap<>();
        Map<Long, Long> itemProjectMap = new HashMap<>();
        for (ProjectChecklistItemEntity item : checklistItems) {
            itemProjectMap.put(item.getId(), item.getProjectId());
            checklistTotal.merge(item.getProjectId(), 1L, Long::sum);
            if (ChecklistItemStatusEnum.COMPLETE.getCode().equals(item.getStatus())) {
                checklistCompleted.merge(item.getProjectId(), 1L, Long::sum);
            }
        }

        Map<Long, Set<Long>> documentIdsByProject = new HashMap<>();
        if (!itemProjectMap.isEmpty()) {
            List<ProjectChecklistDocumentEntity> bindings = safeList(checklistDocumentMapper.selectList(
                    new LambdaQueryWrapper<ProjectChecklistDocumentEntity>()
                            .in(ProjectChecklistDocumentEntity::getChecklistItemId, itemProjectMap.keySet())
                            .eq(ProjectChecklistDocumentEntity::getDeleted, false)));
            for (ProjectChecklistDocumentEntity binding : bindings) {
                Long projectId = itemProjectMap.get(binding.getChecklistItemId());
                if (projectId != null && binding.getDocumentId() != null) {
                    documentIdsByProject.computeIfAbsent(projectId, key -> new LinkedHashSet<>())
                            .add(binding.getDocumentId());
                }
            }
        }

        for (ProjectRespDTO project : projects) {
            Long total = checklistTotal.getOrDefault(project.getId(), 0L);
            Long completed = checklistCompleted.getOrDefault(project.getId(), 0L);
            project.setChecklistProgress(checklistProgress(total, completed));
            project.setDocumentCount((long) documentIdsByProject.getOrDefault(project.getId(), Set.of()).size());
        }
    }

    private ProjectRespDTO.ChecklistProgressRespDTO checklistProgress(Long total, Long completed) {
        ProjectRespDTO.ChecklistProgressRespDTO progress = new ProjectRespDTO.ChecklistProgressRespDTO();
        progress.setTotal(total);
        progress.setCompleted(completed);
        progress.setPercentage(total == null || total == 0 ? 0 : (int) Math.round(completed * 100.0 / total));
        return progress;
    }

    private String joinOwnerNames(List<Long> ownerUserIds, Map<Long, SysUser> userMap) {
        if (ownerUserIds == null || ownerUserIds.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long ownerUserId : ownerUserIds) {
            SysUser user = userMap.get(ownerUserId);
            if (user == null) {
                continue;
            }
            if (StringUtils.hasText(user.getRealName())) {
                names.add(user.getRealName());
            } else if (StringUtils.hasText(user.getUsername())) {
                names.add(user.getUsername());
            } else {
                names.add(String.valueOf(ownerUserId));
            }
        }
        return names.isEmpty() ? null : String.join("、", names);
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
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private ProjectArchiveRecordRespDTO toArchiveRecordResp(ProjectArchiveRecordEntity entity) {
        ProjectArchiveRecordRespDTO dto = new ProjectArchiveRecordRespDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setArchiveNo(entity.getArchiveNo());
        dto.setArchiveStatus(entity.getArchiveStatus());
        dto.setArchivedAt(entity.getArchivedAt());
        dto.setArchivedBy(entity.getArchivedBy());
        dto.setArchiveReason(entity.getArchiveReason());
        dto.setChecklistTotal(entity.getChecklistTotal());
        dto.setChecklistComplete(entity.getChecklistComplete());
        dto.setDocumentTotal(entity.getDocumentTotal());
        dto.setSnapshotHash(entity.getSnapshotHash());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private ProjectArchiveChecklistSnapshotRespDTO toChecklistSnapshotResp(ProjectArchiveChecklistSnapshotEntity entity) {
        ProjectArchiveChecklistSnapshotRespDTO dto = new ProjectArchiveChecklistSnapshotRespDTO();
        dto.setId(entity.getId());
        dto.setArchiveRecordId(entity.getArchiveRecordId());
        dto.setProjectId(entity.getProjectId());
        dto.setChecklistItemId(entity.getChecklistItemId());
        dto.setTemplateItemId(entity.getTemplateItemId());
        dto.setItemName(entity.getItemName());
        dto.setRequiredFlag(entity.getRequiredFlag());
        dto.setItemStatus(entity.getItemStatus());
        dto.setBoundDocumentCount(entity.getBoundDocumentCount());
        dto.setSnapshotJson(entity.getSnapshotJson());
        return dto;
    }

    private ProjectArchiveDocumentSnapshotRespDTO toDocumentSnapshotResp(ProjectArchiveDocumentSnapshotEntity entity) {
        ProjectArchiveDocumentSnapshotRespDTO dto = new ProjectArchiveDocumentSnapshotRespDTO();
        dto.setId(entity.getId());
        dto.setArchiveRecordId(entity.getArchiveRecordId());
        dto.setProjectId(entity.getProjectId());
        dto.setChecklistItemId(entity.getChecklistItemId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setVersionNo(entity.getVersionNo());
        dto.setDocumentName(entity.getDocumentName());
        dto.setDocumentStatus(entity.getDocumentStatus());
        dto.setVersionStatus(entity.getVersionStatus());
        dto.setExpireAt(entity.getExpireAt());
        dto.setStorageType(entity.getStorageType());
        dto.setFileSize(entity.getFileSize());
        dto.setSnapshotJson(entity.getSnapshotJson());
        return dto;
    }

    private record ArchiveSnapshot(List<ChecklistSnapshotSource> items, List<DocumentSnapshotSource> documents) {
    }

    private record ChecklistSnapshotSource(ProjectChecklistItemEntity item, int boundDocumentCount) {
    }

    private record DocumentSnapshotSource(ProjectChecklistItemEntity item,
                                          ProjectChecklistDocumentEntity binding,
                                          DocumentEntity document,
                                          DocumentVersionEntity version,
                                          Integer versionNo,
                                          boolean hasPendingApproval) {
    }
}
