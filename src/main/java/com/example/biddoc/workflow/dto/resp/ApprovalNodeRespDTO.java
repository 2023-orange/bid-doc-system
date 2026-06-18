package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ApprovalNodeRespDTO {

    private Long id;
    private Long definitionId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String approveMode;
    private String assigneeType;
    private String assigneeValue;
    private Integer sortOrder;
    private String nextNodeCode;
    private String rejectToNodeCode;
    private List<ApprovalConditionRespDTO> conditions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
