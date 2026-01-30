package com.example.biddoc.auth.domain.enity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    private String remark;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extensionData;
}
