package com.example.biddoc.project.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectWorkbenchRespDTO {

    private SummaryRespDTO summary;
    private BasicInfoRespDTO basicInfo;
    private OrganizationRespDTO organization;
    private ChecklistStatsRespDTO checklistStats;
    private List<RiskSummaryRespDTO> riskSummaries = new ArrayList<>();
    private List<MemberResponsibilityRespDTO> memberResponsibilities = new ArrayList<>();
    private List<ChecklistItemRespDTO> checklist = new ArrayList<>();
    private List<ProjectFileRespDTO> projectFiles = new ArrayList<>();
    private List<ApprovalSummaryRespDTO> approvalSummaries = new ArrayList<>();
    private List<ProjectArchiveRecordRespDTO> archiveRecords = new ArrayList<>();
    private List<OperationLogRespDTO> operationLogs = new ArrayList<>();
    private List<TabRespDTO> tabs = new ArrayList<>();

    @Data
    public static class SummaryRespDTO {
        private Long projectId;
        private String projectNo;
        private String projectName;
        private String projectStage;
        private String projectStatus;
        private OffsetDateTime bidDeadline;
        private Integer checklistCompletionRate;
        private Integer riskCount;
        private Boolean readOnly;
        private List<String> primaryActions = new ArrayList<>();
    }

    @Data
    public static class BasicInfoRespDTO {
        private Long id;
        private String projectNo;
        private String projectName;
        private String tenderUnit;
        private String projectType;
        private String projectStage;
        private String projectStatus;
        private OffsetDateTime bidDeadline;
        private String remark;
    }

    @Data
    public static class OrganizationRespDTO {
        private Long ownerDeptId;
        private String ownerDeptName;
        private String businessUnit;
        private List<UserBriefRespDTO> owners = new ArrayList<>();
        private List<UserBriefRespDTO> materialOwners = new ArrayList<>();
        private Integer participantCount;
    }

    @Data
    public static class ChecklistStatsRespDTO {
        private Integer total;
        private Integer complete;
        private Integer pendingCollect;
        private Integer pendingReview;
        private Integer needSupplement;
        private Integer archived;
        private Integer overdue;
        private Integer boundDocumentCount;
    }

    @Data
    public static class RiskSummaryRespDTO {
        private String code;
        private String name;
        private String level;
        private Integer count;
    }

    @Data
    public static class MemberResponsibilityRespDTO {
        private Long userId;
        private String userName;
        private List<String> memberRoles = new ArrayList<>();
        private Boolean checklistOwner;
        private Integer responsibleChecklistCount;
        private Integer completeCount;
        private Integer pendingCollectCount;
        private Integer needSupplementCount;
        private Integer overdueCount;
        private OffsetDateTime recentProcessedAt;
    }

    @Data
    public static class ChecklistItemRespDTO {
        private Long id;
        private String itemName;
        private String businessCategory;
        private String tenderStructureCategory;
        private Boolean required;
        private Integer minCount;
        private Integer maxCount;
        private Integer boundDocumentCount;
        private String status;
        private Long ownerUserId;
        private String ownerName;
        private OffsetDateTime deadline;
        private Boolean overdue;
        private List<String> riskTips = new ArrayList<>();
        private List<BoundDocumentRespDTO> boundDocuments = new ArrayList<>();
        private String approvalEntryPath;
    }

    @Data
    public static class BoundDocumentRespDTO {
        private Long documentId;
        private String documentName;
        private String documentNo;
        private String documentStatus;
        private Integer versionNo;
    }

    @Data
    public static class ProjectFileRespDTO {
        private Long documentId;
        private String documentName;
        private String documentNo;
        private String documentStatus;
        private Integer currentVersionNo;
        private Long checklistItemId;
        private String checklistItemName;
        private Integer boundVersionNo;
    }

    @Data
    public static class ApprovalSummaryRespDTO {
        private Long instanceId;
        private String bizType;
        private Long bizId;
        private Long checklistItemId;
        private String checklistItemName;
        private String status;
        private Long submitterUserId;
        private String submitterName;
        private OffsetDateTime submittedAt;
        private OffsetDateTime completedAt;
        private String entryPath;
    }

    @Data
    public static class OperationLogRespDTO {
        private Long id;
        private String operationType;
        private Long operatorUserId;
        private String operatorName;
        private OffsetDateTime operationTime;
    }

    @Data
    public static class TabRespDTO {
        private String code;
        private String label;
        private Boolean readOnly;
        private String entryPath;
    }

    @Data
    public static class UserBriefRespDTO {
        private Long userId;
        private String userName;
    }
}
