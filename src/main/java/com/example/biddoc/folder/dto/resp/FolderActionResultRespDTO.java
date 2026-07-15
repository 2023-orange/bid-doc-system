package com.example.biddoc.folder.dto.resp;

import lombok.Data;

@Data
public class FolderActionResultRespDTO {

    private Long id;
    private String name;
    private Long parentId;
    private String fullPath;
}
