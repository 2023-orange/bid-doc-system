package com.example.biddoc.workflow.dto.req;

import lombok.Data;

@Data
public class ApprovalAddSignReqDTO {

    private Long assigneeUserId;
    private String comment;
}
