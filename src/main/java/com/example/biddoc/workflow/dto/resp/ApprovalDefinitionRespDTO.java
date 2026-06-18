package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ApprovalDefinitionRespDTO {

    private Long id;
    private String name;
    private String scenario;
    private String bizModule;
    private String bizType;
    private Long deptId;
    private String businessCategory;
    private Integer version;
    private Boolean enabled;
    private List<ApprovalNodeRespDTO> nodes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
