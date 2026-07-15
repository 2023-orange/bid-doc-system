package com.example.biddoc.auth.controller;

import com.example.biddoc.auth.convertor.DepartmentConvertor;
import com.example.biddoc.auth.dto.req.DepartmentCreateReqDTO;
import com.example.biddoc.auth.dto.req.DepartmentStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.req.DepartmentUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.DepartmentTreeRespDTO;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.service.DepartmentService;
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
        departmentService.create(dept);
        return ApiResponse.success();
    }

    /**
     * 查询部门树
     */
    @GetMapping
    public ApiResponse<List<DepartmentTreeRespDTO>> list() {
        return ApiResponse.success(departmentService.listTree());
    }

    /**
     * 编辑部门基础信息，允许在安全校验通过后调整父级部门
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateReqDTO req) {
        departmentService.update(id, req);
        return ApiResponse.success();
    }

    /**
     * 启用 / 禁用部门
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentStatusUpdateReqDTO req) {
        departmentService.changeStatus(id, req);
        return ApiResponse.success();
    }

    /**
     * 删除空部门
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success();
    }
}

