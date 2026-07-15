package com.example.biddoc.dashboard.service.impl;

import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.document.dto.req.DocumentSearchReqDTO;
import com.example.biddoc.document.dto.resp.DocumentListItemRespDTO;
import com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO;
import com.example.biddoc.document.dto.resp.StorageStatsRespDTO;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectChecklistItemMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.workflow.mapper.ApprovalTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceImplTest {

    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final ProjectMemberMapper projectMemberMapper = mock(ProjectMemberMapper.class);
    private final ProjectChecklistItemMapper checklistItemMapper = mock(ProjectChecklistItemMapper.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final ApprovalTaskMapper approvalTaskMapper = mock(ApprovalTaskMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final DashboardServiceImpl service = new DashboardServiceImpl(
            projectMapper,
            projectMemberMapper,
            checklistItemMapper,
            documentService,
            approvalTaskMapper,
            sysUserMapper
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getStatsAggregatesSystemUserAndRecentData() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        when(projectMapper.selectList(any())).thenReturn(List.of(
                project(100L, "BD-001", "国网投标项目", "NORMAL", "2026-06-20T10:00:00+08:00"),
                project(101L, "BD-002", "城投归档项目", "ARCHIVED", "2026-06-19T10:00:00+08:00")
        ));
        when(projectMemberMapper.selectCount(any())).thenReturn(2L);
        when(approvalTaskMapper.selectCount(any())).thenReturn(4L);
        when(sysUserMapper.selectCount(any())).thenReturn(3L);

        StorageStatsRespDTO storageStats = new StorageStatsRespDTO();
        storageStats.setTotalDocuments(7L);
        storageStats.setMyDocuments(2L);
        when(documentService.getStorageStats()).thenReturn(storageStats);

        DocumentTypeStatsRespDTO pdfType = new DocumentTypeStatsRespDTO();
        pdfType.setMimeType("application/pdf");
        pdfType.setCount(5L);
        when(documentService.getDocumentTypeStats(10)).thenReturn(List.of(pdfType));

        DocumentListItemRespDTO recentDocument = new DocumentListItemRespDTO();
        recentDocument.setId("200");
        recentDocument.setName("投标文件.pdf");
        recentDocument.setCurrentVersionNo(3);
        recentDocument.setUpdatedAt(OffsetDateTime.parse("2026-06-21T09:00:00+08:00"));
        when(documentService.searchDocuments(any(DocumentSearchReqDTO.class))).thenReturn(
                new PageResponse<>(List.of(recentDocument), 1, 5, 1, 1, false));

        var stats = service.getStats();

        assertEquals(2L, stats.getSystemStats().getTotalProjects());
        assertEquals(1L, stats.getSystemStats().getActiveProjects());
        assertEquals(7L, stats.getSystemStats().getTotalDocuments());
        assertEquals(3L, stats.getSystemStats().getTotalUsers());
        assertEquals(2L, stats.getMyStats().getMyProjects());
        assertEquals(4L, stats.getMyStats().getMyTasks());
        assertEquals(2L, stats.getMyStats().getMyDocuments());
        assertEquals(4L, stats.getMyStats().getPendingApprovals());
        assertEquals("BD-001", stats.getRecentProjects().get(0).getProjectNo());
        assertEquals("200", stats.getRecentDocuments().get(0).getId());
        assertEquals(1L, stats.getProjectStatusDistribution().get("NORMAL"));
        assertEquals(1L, stats.getProjectStatusDistribution().get("ARCHIVED"));
        assertEquals("application/pdf", stats.getDocumentTypeDistribution().get(0).getMimeType());
        assertEquals(5L, stats.getDocumentTypeDistribution().get(0).getCount());
    }

    private ProjectEntity project(Long id, String no, String name, String status, String updatedAt) {
        ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setProjectNo(no);
        project.setProjectName(name);
        project.setProjectStatus(status);
        project.setUpdatedAt(OffsetDateTime.parse(updatedAt));
        project.setDeleted(false);
        return project;
    }
}
