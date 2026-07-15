package com.example.biddoc.project.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectRespDTO {
    private Long id;
    private String projectNo;
    private String projectName;
    private String tenderUnit;
    private Long ownerDeptId;
    private String ownerDeptName;
    private String ownerUserName;
    private String projectType;
    private String projectStage;
    private String projectStatus;
    private Long memberCount;
    private Long documentCount;
    private ChecklistProgressRespDTO checklistProgress;
    private OffsetDateTime bidDeadline;
    private Long folderId;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    public static class ChecklistProgressRespDTO {
        private Long total;
        private Long completed;
        private Integer percentage;
    }
}
