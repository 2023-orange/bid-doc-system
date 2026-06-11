package com.example.biddoc.notify.service;

import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.notify.dto.resp.NotificationRespDTO;

public interface NotificationService {

    void send(Long receiverUserId, String type, String title, String content, String bizType, Long bizId);

    PageResponse<NotificationRespDTO> listMine(Integer page, Integer size, Boolean unreadOnly);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();
}
