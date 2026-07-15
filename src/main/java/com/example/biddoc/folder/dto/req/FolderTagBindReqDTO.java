package com.example.biddoc.folder.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class FolderTagBindReqDTO {

    private List<Long> tagIds;
}
