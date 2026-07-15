package com.example.biddoc.auth.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UserManageRespDTO {

    private Long id;
    private String username;
    private String realName;
    private String mobile;
    private String email;
    private Long deptId;
    private String deptName;
    private List<String> roleCodes;
    private Boolean status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
