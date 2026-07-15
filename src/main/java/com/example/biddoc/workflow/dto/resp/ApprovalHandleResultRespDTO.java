package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

@Data
public class ApprovalHandleResultRespDTO {

    private String taskStatus;
    private String instanceStatus;
    private Boolean completed;
    private Long nextTaskId;
    private String nextNodeName;
}
