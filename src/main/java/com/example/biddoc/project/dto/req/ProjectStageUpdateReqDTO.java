package com.example.biddoc.project.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectStageUpdateReqDTO {
    @NotBlank
    private String projectStage;
}
