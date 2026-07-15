package com.example.biddoc.document.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocumentUseGrantRespDTO {

    private Long id;

    private Long documentId;

    private Integer versionNo;

    private Long projectId;

    private Long applicantId;

    private Long granteeId;

    private Long approvalInstanceId;

    private String grantType;

    private String scenario;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private String status;

    private String reason;

    private OffsetDateTime createdAt;
}
