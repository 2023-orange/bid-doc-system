package com.example.biddoc.auth.convertor;

import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.dto.req.DepartmentCreateReqDTO;

public class DepartmentConvertor {

    public static SysDepartment toEntity(DepartmentCreateReqDTO dto) {
        SysDepartment dept = new SysDepartment();
        dept.setName(dto.getName());
        dept.setParentId(dto.getParentId());
        dept.setLevel(dto.getLevel());
        dept.setRemark(dto.getRemark());
        return dept;
    }
}

