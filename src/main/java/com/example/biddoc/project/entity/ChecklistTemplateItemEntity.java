package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("bid_checklist_template_item")
public class ChecklistTemplateItemEntity {
    @TableId
    private Long id;
    private Long templateId;
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
    @TableLogic
    private Boolean deleted;
}
