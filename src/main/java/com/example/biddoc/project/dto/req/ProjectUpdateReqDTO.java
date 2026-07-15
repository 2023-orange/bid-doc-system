package com.example.biddoc.project.dto.req;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectUpdateReqDTO {
    private String projectName;
    private String tenderUnit;
    private String projectType;
    private OffsetDateTime bidDeadline;
    private Long folderId;
    private String remark;
}
