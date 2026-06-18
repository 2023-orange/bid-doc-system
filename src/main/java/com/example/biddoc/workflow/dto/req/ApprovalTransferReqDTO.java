package com.example.biddoc.workflow.dto.req;

import lombok.Data;

@Data
public class ApprovalTransferReqDTO {

    private Long targetUserId;
    private String comment;
}
