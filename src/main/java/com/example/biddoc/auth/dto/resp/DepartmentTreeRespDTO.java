package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentTreeRespDTO {

    private Long id;
    private String name;
    private Integer level;
    private List<DepartmentTreeRespDTO> children;
}
