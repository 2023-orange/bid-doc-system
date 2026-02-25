package com.example.biddoc.auth.service;

import com.example.biddoc.auth.entity.SysDepartment;

import java.util.List;

public interface DepartmentService {

    List<SysDepartment> listAll();

    void create(SysDepartment department);

}

