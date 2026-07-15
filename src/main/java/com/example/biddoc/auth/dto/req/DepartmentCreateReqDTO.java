package com.example.biddoc.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentCreateReqDTO {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Long parentId;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;

    private Long managerUserId;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    private String remark;
}
