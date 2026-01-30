package com.example.biddoc.auth.api.dto.resp;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentTreeRespDTO {

    private Long id;
    private String name;
    private Integer level;
    private List<DepartmentTreeRespDTO> children;
}
