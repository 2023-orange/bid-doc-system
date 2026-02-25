package com.example.biddoc.auth.service;

import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;

public interface UserService {

    UserDetailRespDTO getById(Long id);

    void changeStatus(Long userId, Integer status);
}

