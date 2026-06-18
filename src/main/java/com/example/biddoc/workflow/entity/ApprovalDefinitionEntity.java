package com.example.biddoc.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("wf_approval_definition")
public class ApprovalDefinitionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String scenario;
    private String bizModule;
    private String bizType;
    private Long deptId;
    private String businessCategory;
    private Integer version;
    private Boolean enabled;

    @TableLogic
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
