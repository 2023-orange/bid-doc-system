package com.example.biddoc.document.dto.req;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocumentMetadataUpdateReqDTO {
    private String documentName;
    private String businessCategory;
    private String tenderStructureCategory;
    private String sensitiveLevel;
    private Long ownerDeptId;
    private String sourceType;
    private Boolean hasExpireDate;
    private OffsetDateTime effectiveDate;
    private OffsetDateTime expireDate;
    private String remark;
}
