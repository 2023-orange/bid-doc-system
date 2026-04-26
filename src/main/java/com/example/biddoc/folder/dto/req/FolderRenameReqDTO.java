package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderRenameReqDTO {

    @NotBlank(message = "name不能为空")
    private String name;
}
