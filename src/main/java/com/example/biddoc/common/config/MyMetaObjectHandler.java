package com.example.biddoc.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.biddoc.common.constant.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, OffsetDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());

        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo != null) {
            String userIdStr = String.valueOf(userInfo.getUserId());
            this.strictInsertFill(metaObject, "createdBy", String.class, userIdStr);
            this.strictInsertFill(metaObject, "updatedBy", String.class, userIdStr);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());

        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo != null) {
            String userIdStr = String.valueOf(userInfo.getUserId());
            this.strictUpdateFill(metaObject, "updatedBy", String.class, userIdStr);
        }
    }
}