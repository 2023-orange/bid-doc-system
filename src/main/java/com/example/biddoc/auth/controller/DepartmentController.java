package com.example.biddoc.auth.controller;

import com.example.biddoc.auth.dto.req.DepartmentCreateReqDTO;
import com.example.biddoc.auth.convertor.DepartmentConvertor;
import com.example.biddoc.auth.service.DepartmentService;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 创建部门
     */
    @PostMapping
    public ApiResponse<Void> create(
            @Valid @RequestBody DepartmentCreateReqDTO req) {

        SysDepartment dept = DepartmentConvertor.toEntity(req);
//        // 临时手动赋值进行测试
//        dept.setCreatedBy("test");
//        dept.setUpdatedBy("test");
        departmentService.create(dept);
        return ApiResponse.success();
    }

    /**
     * 查询部门列表（暂不构建树，后续补）
     */
    @GetMapping
    public ApiResponse<List<SysDepartment>> list() {
        return ApiResponse.success(departmentService.listAll());
    }
}

