package com.example.biddoc.project.dto.req;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ChecklistOwnerUpdateReqDTO {
    private Long ownerUserId;
    private OffsetDateTime deadline;
}
