package com.example.biddoc.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String roleCode;
    private String roleName;

    private Integer status;
    private Boolean deleted;

    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;

    private String remark;
}

