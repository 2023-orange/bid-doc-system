package com.example.biddoc.auth.application.Convertor;

import com.example.biddoc.auth.api.dto.req.DepartmentCreateReqDTO;
import com.example.biddoc.auth.domain.enity.SysDepartment;

public class DepartmentConvertor {

    public static SysDepartment toEntity(DepartmentCreateReqDTO dto) {
        SysDepartment dept = new SysDepartment();
        dept.setName(dto.getName());
        dept.setParentId(dto.getParentId());
        return dept;
    }
}

