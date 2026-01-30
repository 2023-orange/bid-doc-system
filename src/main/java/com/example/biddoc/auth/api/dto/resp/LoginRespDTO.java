package com.example.biddoc.auth.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRespDTO {

    private String token;
    private long expireIn;
    private UserDetailRespDTO user;
}
