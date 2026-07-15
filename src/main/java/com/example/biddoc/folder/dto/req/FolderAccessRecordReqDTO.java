package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FolderAccessRecordReqDTO {

    @NotBlank(message = "accessType不能为空")
    @Pattern(regexp = "VIEW|DOWNLOAD|EDIT", message = "accessType只能是VIEW、DOWNLOAD或EDIT")
    private String accessType;
}
