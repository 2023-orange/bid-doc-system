package com.example.biddoc.auth.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRegisterRespDTO {

    private Long userId;
    private String status;
}

