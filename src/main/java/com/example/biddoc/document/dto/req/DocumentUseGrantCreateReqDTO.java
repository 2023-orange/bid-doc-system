package com.example.biddoc.document.dto.req;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DocumentUseGrantCreateReqDTO {

    private Integer versionNo;

    private Long projectId;

    private Long granteeId;

    private Long approvalInstanceId;

    private String grantType;

    private String scenario;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private String reason;
}
