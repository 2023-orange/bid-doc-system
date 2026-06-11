package com.example.biddoc.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.notify.entity.NotificationEntity;
import com.example.biddoc.notify.mapper.NotificationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void sendCreatesUnreadNotification() {
        service.send(7L, "FOLDER_GRANT", "获得文件夹权限", "你获得了文件夹查看权限", "FOLDER", 10L);

        verify(notificationMapper).insert(argThat(entity ->
                Long.valueOf(7L).equals(entity.getReceiverUserId())
                        && "FOLDER_GRANT".equals(entity.getType())
                        && Boolean.FALSE.equals(entity.getReadFlag())
                        && Boolean.FALSE.equals(entity.getDeleted())
        ));
    }

    @Test
    void listMineReturnsOnlyCurrentUserNotifications() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        NotificationEntity entity = new NotificationEntity();
        entity.setId(1L);
        entity.setReceiverUserId(7L);
        entity.setType("WORKFLOW_TASK");
        entity.setTitle("审批任务");
        entity.setContent("请审批文档");
        entity.setReadFlag(false);
        entity.setCreatedAt(OffsetDateTime.now());

        Page<NotificationEntity> page = new Page<>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(entity));
        when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = service.listMine(1, 20, false);

        assertEquals(1, result.getTotal());
        assertEquals("审批任务", result.getList().get(0).getTitle());
    }
}
