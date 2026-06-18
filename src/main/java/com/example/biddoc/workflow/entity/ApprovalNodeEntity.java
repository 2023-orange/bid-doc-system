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
@TableName("wf_approval_node")
public class ApprovalNodeEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long definitionId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String approveMode;
    private String assigneeType;
    private String assigneeValue;
    private Integer sortOrder;
    private String nextNodeCode;
    private String rejectToNodeCode;

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
