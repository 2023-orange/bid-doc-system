package com.example.biddoc.project.dto.req;

import lombok.Data;

@Data
public class ChecklistTemplateSaveReqDTO {
    private String templateName;
    private String projectType;
    private Boolean enabled;
}
