package com.example.biddoc.auth.domain.enity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName("sys_department")
public class SysDepartment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private Long parentId;
    private Integer level;

    private Long managerUserId;

    private Integer status;
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT) // 自动填充创建时间
    private LocalDateTime createdAt;

    // 加上自动填充注解
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 自动填充更新时间
    private LocalDateTime updatedAt;

    // 加上自动填充注解
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    private String remark;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extensionData;
}
