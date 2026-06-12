package com.example.biddoc.project.dto.req;

import lombok.Data;

@Data
public class ChecklistTemplateItemSaveReqDTO {
    private String itemName;
    private String description;
    private Boolean required;
    private String businessCategory;
    private String tenderStructureCategory;
    private String suggestedSensitiveLevel;
    private String allowedSource;
    private String allowedFileTypes;
    private Integer minCount;
    private Integer maxCount;
    private Integer sortOrder;
}
