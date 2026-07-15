package com.example.biddoc.auth.service;

import com.example.biddoc.auth.dto.req.UserCreateReqDTO;
import com.example.biddoc.auth.dto.req.UserPasswordResetReqDTO;
import com.example.biddoc.auth.dto.req.UserUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.UserDetailRespDTO;
import com.example.biddoc.auth.dto.resp.UserManageRespDTO;
import com.example.biddoc.auth.dto.resp.UserOptionRespDTO;
import com.example.biddoc.common.result.PageResponse;

import java.util.List;

public interface UserService {

    UserDetailRespDTO getById(Long id);

    PageResponse<UserManageRespDTO> pageUsers(String keyword, Long deptId, Boolean status, String roleCode,
                                              Integer page, Integer size);

    List<UserOptionRespDTO> listOptions(String keyword, Long deptId, Integer status, Integer size);

    Long createUser(UserCreateReqDTO req);

    void updateUser(Long userId, UserUpdateReqDTO req);

    void resetPassword(Long userId, UserPasswordResetReqDTO req);

    void changeStatus(Long userId, Integer status);
}

