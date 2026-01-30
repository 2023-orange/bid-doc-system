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

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    private String remark;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extensionData;
}
