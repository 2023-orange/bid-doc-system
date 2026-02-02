package com.example.biddoc.auth.domain.enity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private String email;
    private String mobile;

    private Long deptId;
    private Integer jobLevel;
    private Long roleId;

    private Integer status;
    private Boolean deleted;

    private LocalDateTime lastLoginTime;
    private Integer loginCount;

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
