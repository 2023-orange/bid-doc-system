package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

