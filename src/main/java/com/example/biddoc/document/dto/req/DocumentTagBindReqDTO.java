package com.example.biddoc.document.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class DocumentTagBindReqDTO {

    private List<Long> tagIds;
}
