package com.example.biddoc.project.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ProjectMemberSaveReqDTO {
    @NotEmpty
    private List<Long> userIds;
    private String memberRole;
}
