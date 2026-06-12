package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("bid_project_checklist_item")
public class ProjectChecklistItemEntity {
    @TableId
    private Long id;
    private Long projectId;
    private Long templateItemId;
    private String itemName;
    private String description;
    private Boolean required;
    private String businessCategory;
    private String tenderStructureCategory;
    private String sensitiveLevel;
    private String allowedSource;
    private String allowedFileTypes;
    private Integer minCount;
    private Integer maxCount;
    private OffsetDateTime deadline;
    private Long ownerUserId;
    private String status;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
