package com.example.biddoc.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.biddoc.common.constant.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时，填充 createdAt 和 updatedAt
        // 注意：这里的 "createdAt" 和 "updatedAt" 是实体类的属性名，不是数据库字段名
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 2. 自动填充创建人/更新人
        // 从 ThreadLocal 中获取用户信息
        UserContext.UserInfo userInfo = UserContext.get();

        // 如果是从 Web 请求进来的，userInfo 不为空；如果是定时任务或系统启动，可能为空
        if (userInfo != null) {
            // 假设数据库存的是 String 类型的 ID
            String userIdStr = String.valueOf(userInfo.getUserId());

            this.strictInsertFill(metaObject, "createdBy", String.class, userIdStr);
            this.strictInsertFill(metaObject, "updatedBy", String.class, userIdStr);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 1. 更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 2. 更新操作人
        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo != null) {
            String userIdStr = String.valueOf(userInfo.getUserId());
            this.strictUpdateFill(metaObject, "updatedBy", String.class, userIdStr);
        }
    }
}