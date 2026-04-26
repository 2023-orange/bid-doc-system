package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FolderUpdateReqDTO {

    private String remark;

    @NotNull(message = "status不能为空")
    @Min(value = 0, message = "status只允许为0或1")
    @Max(value = 1, message = "status只允许为0或1")
    private Integer status;

    @NotNull(message = "inheritPermission不能为空")
    private Boolean inheritPermission;

    @NotNull(message = "sortNo不能为空")
    @Min(value = 1, message = "sortNo必须大于等于1")
    private Integer sortNo;
}
