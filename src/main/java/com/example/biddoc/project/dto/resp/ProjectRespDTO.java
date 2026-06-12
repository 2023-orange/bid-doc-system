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
    private String projectType;
    private String projectStage;
    private String projectStatus;
    private OffsetDateTime bidDeadline;
    private Long folderId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
