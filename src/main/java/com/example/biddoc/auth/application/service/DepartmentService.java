package com.example.biddoc.auth.application.service;

import com.example.biddoc.auth.domain.enity.SysDepartment;

import java.util.List;

public interface DepartmentService {

    List<SysDepartment> listAll();

    void create(SysDepartment department);
}

