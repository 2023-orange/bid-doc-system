package com.example.biddoc.auth.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserRoleRespDTO {

    private Long id;
    private Long userId;
    private String roleCode;
    private String roleName;
    private Boolean isPrimary;
    private Integer status;
    private OffsetDateTime effectiveStartTime;
    private OffsetDateTime effectiveEndTime;
    private Integer sourceType;
    private OffsetDateTime createdAt;
}

