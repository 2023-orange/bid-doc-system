package com.example.biddoc.notify.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NotificationRespDTO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Boolean readFlag;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
}
