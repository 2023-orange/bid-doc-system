package com.example.biddoc.project.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectStatusUpdateReqDTO {
    @NotBlank
    private String projectStatus;
}
