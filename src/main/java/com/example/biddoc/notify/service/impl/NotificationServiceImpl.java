package com.example.biddoc.notify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.notify.dto.resp.NotificationRespDTO;
import com.example.biddoc.notify.entity.NotificationEntity;
import com.example.biddoc.notify.mapper.NotificationMapper;
import com.example.biddoc.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long receiverUserId, String type, String title, String content, String bizType, Long bizId) {
        if (receiverUserId == null) {
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setReceiverUserId(receiverUserId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setReadFlag(Boolean.FALSE);
        entity.setDeleted(Boolean.FALSE);
        notificationMapper.insert(entity);
    }

    @Override
    public PageResponse<NotificationRespDTO> listMine(Integer page, Integer size, Boolean unreadOnly) {
        UserContext.UserInfo user = requireCurrentUser();
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 || size > 100 ? 20 : size;
        Page<NotificationEntity> entityPage = notificationMapper.selectPage(
                new Page<>(pageNo, pageSize),
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getReceiverUserId, user.getUserId())
                        .eq(NotificationEntity::getDeleted, false)
                        .eq(Boolean.TRUE.equals(unreadOnly), NotificationEntity::getReadFlag, false)
                        .orderByDesc(NotificationEntity::getCreatedAt)
        );
        Page<NotificationRespDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::toRespDTO).toList());
        return PageResponse.of(dtoPage);
    }

    @Override
    public long unreadCount() {
        UserContext.UserInfo user = requireCurrentUser();
        Long count = notificationMapper.selectCount(
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getReceiverUserId, user.getUserId())
                        .eq(NotificationEntity::getReadFlag, false)
                        .eq(NotificationEntity::getDeleted, false)
        );
        return count != null ? count : 0L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        UserContext.UserInfo user = requireCurrentUser();
        NotificationEntity entity = notificationMapper.selectOne(
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getId, id)
                        .eq(NotificationEntity::getReceiverUserId, user.getUserId())
                        .eq(NotificationEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        entity.setReadFlag(Boolean.TRUE);
        entity.setReadAt(OffsetDateTime.now());
        notificationMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        UserContext.UserInfo user = requireCurrentUser();
        NotificationEntity entity = new NotificationEntity();
        entity.setReadFlag(Boolean.TRUE);
        entity.setReadAt(OffsetDateTime.now());
        notificationMapper.update(
                entity,
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getReceiverUserId, user.getUserId())
                        .eq(NotificationEntity::getReadFlag, false)
                        .eq(NotificationEntity::getDeleted, false)
        );
    }

    private UserContext.UserInfo requireCurrentUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "请先登录");
        }
        return user;
    }

    private NotificationRespDTO toRespDTO(NotificationEntity entity) {
        NotificationRespDTO dto = new NotificationRespDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setReadFlag(entity.getReadFlag());
        dto.setReadAt(entity.getReadAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
