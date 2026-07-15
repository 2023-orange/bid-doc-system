package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentTreeRespDTO {

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private Integer level;
    private Integer sortOrder;
    private Long managerUserId;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean hasChildren;
    private Integer memberCount;
    private Integer projectCount;
    private List<DepartmentTreeRespDTO> children;
}
