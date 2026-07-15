package com.example.biddoc.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.dashboard.dto.resp.DashboardStatsRespDTO;
import com.example.biddoc.dashboard.service.DashboardService;
import com.example.biddoc.document.dto.req.DocumentSearchReqDTO;
import com.example.biddoc.document.dto.resp.DocumentListItemRespDTO;
import com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO;
import com.example.biddoc.document.dto.resp.StorageStatsRespDTO;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.project.constant.ProjectStatusEnum;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int DOCUMENT_TYPE_LIMIT = 10;
    private static final String APPROVAL_PENDING = "PENDING";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    @SuppressWarnings("unused")
    private final ProjectChecklistItemMapper checklistItemMapper;
    private final DocumentService documentService;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public DashboardStatsRespDTO getStats() {
        UserContext.UserInfo currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        List<ProjectEntity> visibleProjects = listVisibleProjects(currentUser);
        StorageStatsRespDTO storageStats = documentService.getStorageStats();
        DashboardStatsRespDTO resp = new DashboardStatsRespDTO();
        resp.setSystemStats(buildSystemStats(currentUser, visibleProjects, storageStats));
        resp.setMyStats(buildMyStats(currentUser, storageStats));
        resp.setRecentProjects(buildRecentProjects(visibleProjects));
        resp.setRecentDocuments(buildRecentDocuments());
        resp.setProjectStatusDistribution(buildProjectStatusDistribution(visibleProjects));
        resp.setDocumentTypeDistribution(buildDocumentTypeDistribution());
        return resp;
    }

    private List<ProjectEntity> listVisibleProjects(UserContext.UserInfo currentUser) {
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getDeleted, false)
                .orderByDesc(ProjectEntity::getUpdatedAt);
        if (!currentUser.isSuperAdmin()) {
            Long userId = currentUser.getUserId();
            // 首页项目统计必须沿用项目列表的数据范围，避免普通用户看到无权项目数量。
            wrapper.and(q -> q.inSql(ProjectEntity::getId,
                            "select project_id from bid_project_member where deleted = false and user_id = " + userId)
                    .or()
                    .inSql(ProjectEntity::getId,
                            "select project_id from bid_project_checklist_item where deleted = false and owner_user_id = " + userId));
        }
        return projectMapper.selectList(wrapper);
    }

    private DashboardStatsRespDTO.SystemStatsRespDTO buildSystemStats(UserContext.UserInfo currentUser,
                                                                      List<ProjectEntity> projects,
                                                                      StorageStatsRespDTO storageStats) {
        DashboardStatsRespDTO.SystemStatsRespDTO stats = new DashboardStatsRespDTO.SystemStatsRespDTO();
        stats.setTotalProjects((long) projects.size());
        stats.setActiveProjects(projects.stream()
                .filter(project -> ProjectStatusEnum.NORMAL.getCode().equals(project.getProjectStatus()))
                .count());
        stats.setTotalDocuments(storageStats != null && storageStats.getTotalDocuments() != null
                ? storageStats.getTotalDocuments() : 0L);
        stats.setTotalUsers(countVisibleUsers(currentUser));
        return stats;
    }

    private DashboardStatsRespDTO.MyStatsRespDTO buildMyStats(UserContext.UserInfo currentUser,
                                                              StorageStatsRespDTO storageStats) {
        Long pendingTasks = safeCount(approvalTaskMapper.selectCount(new LambdaQueryWrapper<ApprovalTaskEntity>()
                .eq(ApprovalTaskEntity::getApproverUserId, currentUser.getUserId())
                .eq(ApprovalTaskEntity::getStatus, APPROVAL_PENDING)
                .eq(ApprovalTaskEntity::getDeleted, false)));

        DashboardStatsRespDTO.MyStatsRespDTO stats = new DashboardStatsRespDTO.MyStatsRespDTO();
        stats.setMyProjects(safeCount(projectMemberMapper.selectCount(
                new LambdaQueryWrapper<com.example.biddoc.project.entity.ProjectMemberEntity>()
                        .eq(com.example.biddoc.project.entity.ProjectMemberEntity::getUserId, currentUser.getUserId())
                        .eq(com.example.biddoc.project.entity.ProjectMemberEntity::getDeleted, false))));
        stats.setMyTasks(pendingTasks);
        stats.setMyDocuments(storageStats != null && storageStats.getMyDocuments() != null
                ? storageStats.getMyDocuments() : 0L);
        stats.setPendingApprovals(pendingTasks);
        return stats;
    }

    private Long countVisibleUsers(UserContext.UserInfo currentUser) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, false);
        if (!currentUser.isSuperAdmin() && currentUser.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, currentUser.getDeptId());
        }
        return safeCount(sysUserMapper.selectCount(wrapper));
    }

    private List<DashboardStatsRespDTO.RecentProjectRespDTO> buildRecentProjects(List<ProjectEntity> projects) {
        return projects.stream()
                .sorted(Comparator.comparing(ProjectEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_LIMIT)
                .map(this::toRecentProject)
                .toList();
    }

    private List<DashboardStatsRespDTO.RecentDocumentRespDTO> buildRecentDocuments() {
        DocumentSearchReqDTO req = new DocumentSearchReqDTO();
        req.setPage(1);
        req.setSize(RECENT_LIMIT);
        req.setSortBy("updatedAt");
        req.setSortOrder("desc");
        PageResponse<DocumentListItemRespDTO> page = documentService.searchDocuments(req);
        if (page == null || page.getList() == null) {
            return List.of();
        }
        return page.getList().stream().map(this::toRecentDocument).toList();
    }

    private Map<String, Long> buildProjectStatusDistribution(List<ProjectEntity> projects) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put(ProjectStatusEnum.NORMAL.getCode(), 0L);
        distribution.put(ProjectStatusEnum.PAUSED.getCode(), 0L);
        distribution.put(ProjectStatusEnum.ARCHIVED.getCode(), 0L);
        for (ProjectEntity project : projects) {
            if (distribution.containsKey(project.getProjectStatus())) {
                distribution.compute(project.getProjectStatus(), (key, value) -> value == null ? 1L : value + 1L);
            }
        }
        return distribution;
    }

    private List<DashboardStatsRespDTO.DocumentTypeDistributionRespDTO> buildDocumentTypeDistribution() {
        List<DocumentTypeStatsRespDTO> types = documentService.getDocumentTypeStats(DOCUMENT_TYPE_LIMIT);
        if (types == null) {
            return List.of();
        }
        return types.stream().map(type -> {
            DashboardStatsRespDTO.DocumentTypeDistributionRespDTO dto =
                    new DashboardStatsRespDTO.DocumentTypeDistributionRespDTO();
            dto.setMimeType(type.getMimeType());
            dto.setCount(type.getCount());
            return dto;
        }).toList();
    }

    private DashboardStatsRespDTO.RecentProjectRespDTO toRecentProject(ProjectEntity project) {
        DashboardStatsRespDTO.RecentProjectRespDTO dto = new DashboardStatsRespDTO.RecentProjectRespDTO();
        dto.setId(project.getId());
        dto.setProjectNo(project.getProjectNo());
        dto.setProjectName(project.getProjectName());
        dto.setProjectStatus(project.getProjectStatus());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }

    private DashboardStatsRespDTO.RecentDocumentRespDTO toRecentDocument(DocumentListItemRespDTO document) {
        DashboardStatsRespDTO.RecentDocumentRespDTO dto = new DashboardStatsRespDTO.RecentDocumentRespDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setCurrentVersionNo(document.getCurrentVersionNo());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }
}
