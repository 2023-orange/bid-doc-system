package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOptionRespDTO {

    private Long id;
    private String realName;
    private String username;
    private Long deptId;
    private String deptName;
    private Integer status;
}
