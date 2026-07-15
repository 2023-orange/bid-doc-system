package com.example.biddoc.auth.dto.resp;

import lombok.Data;

@Data
public class RoleSummaryRespDTO {

    private String code;
    private String name;
    private String description;
    private Long userCount;
}
