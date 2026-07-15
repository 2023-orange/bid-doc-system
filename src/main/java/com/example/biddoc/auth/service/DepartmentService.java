package com.example.biddoc.auth.service;

import com.example.biddoc.auth.dto.req.DepartmentStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.req.DepartmentUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.DepartmentTreeRespDTO;
import com.example.biddoc.auth.entity.SysDepartment;

import java.util.List;

public interface DepartmentService {

    List<SysDepartment> listAll();

    List<DepartmentTreeRespDTO> listTree();

    void create(SysDepartment department);

    void update(Long id, DepartmentUpdateReqDTO req);

    void changeStatus(Long id, DepartmentStatusUpdateReqDTO req);

    void delete(Long id);

}

