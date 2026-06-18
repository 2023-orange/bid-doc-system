package com.example.biddoc.workflow.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class ApprovalNodeSaveReqDTO {

    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String approveMode;
    private String assigneeType;
    private String assigneeValue;
    private Integer sortOrder;
    private String nextNodeCode;
    private String rejectToNodeCode;
    private List<ApprovalConditionSaveReqDTO> conditions;
}
