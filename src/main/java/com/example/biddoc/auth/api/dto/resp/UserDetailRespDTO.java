package com.example.biddoc.auth.api.dto.resp;

import lombok.Data;

@Data
public class UserDetailRespDTO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String mobile;
    private Integer jobLevel;
    private String role;
    private Integer status;
}

