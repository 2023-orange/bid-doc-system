package com.example.biddoc.auth.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCreateReqDTO {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Long parentId;
}
