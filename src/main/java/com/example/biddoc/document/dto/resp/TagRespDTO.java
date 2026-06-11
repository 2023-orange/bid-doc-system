package com.example.biddoc.document.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TagRespDTO {

    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private String createdBy;
}
