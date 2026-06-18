package com.example.biddoc.workflow.dto.req;

import lombok.Data;

@Data
public class ApprovalDefinitionSaveReqDTO {

    private String name;
    private String scenario;
    private String bizModule;
    private String bizType;
    private Long deptId;
    private String businessCategory;
}
