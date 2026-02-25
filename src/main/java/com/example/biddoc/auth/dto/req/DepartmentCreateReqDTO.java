package com.example.biddoc.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentCreateReqDTO {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    @NotNull(message = "父级部门ID不能为空")
    private Long parentId;

    @NotNull(message = "部门层级不能为空")
    @Range(min = 1, max = 2, message = "层级只能是1或2")
    private Integer level; // 强制前端必须传入 1 或 2

    private String remark;
}
