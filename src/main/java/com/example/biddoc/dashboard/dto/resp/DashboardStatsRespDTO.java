package com.example.biddoc.dashboard.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsRespDTO {

    private SystemStatsRespDTO systemStats;
    private MyStatsRespDTO myStats;
    private List<RecentProjectRespDTO> recentProjects;
    private List<RecentDocumentRespDTO> recentDocuments;
    private Map<String, Long> projectStatusDistribution;
    private List<DocumentTypeDistributionRespDTO> documentTypeDistribution;

    @Data
    public static class SystemStatsRespDTO {
        private Long totalProjects;
        private Long activeProjects;
        private Long totalDocuments;
        private Long totalUsers;
    }

    @Data
    public static class MyStatsRespDTO {
        private Long myProjects;
        private Long myTasks;
        private Long myDocuments;
        private Long pendingApprovals;
    }

    @Data
    public static class RecentProjectRespDTO {
        private Long id;
        private String projectNo;
        private String projectName;
        private String projectStatus;
        private OffsetDateTime updatedAt;
    }

    @Data
    public static class RecentDocumentRespDTO {
        private String id;
        private String name;
        private Integer currentVersionNo;
        private OffsetDateTime updatedAt;
    }

    @Data
    public static class DocumentTypeDistributionRespDTO {
        private String mimeType;
        private Long count;
    }
}
