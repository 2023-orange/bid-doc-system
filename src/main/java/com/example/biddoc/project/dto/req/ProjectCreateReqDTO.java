package com.example.biddoc.project.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectCreateReqDTO {

    @NotBlank
    private String projectName;
    private String tenderUnit;
    @NotNull
    private Long ownerDeptId;
    private String projectType;
    private OffsetDateTime bidDeadline;
    private Long folderId;
    private List<Long> ownerUserIds = new ArrayList<>();
    private List<Long> materialOwnerUserIds = new ArrayList<>();
    private List<Long> memberUserIds = new ArrayList<>();
}
