package com.example.biddoc.notify.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.notify.dto.resp.NotificationRespDTO;
import com.example.biddoc.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationRespDTO>> list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") Boolean unreadOnly) {
        return ApiResponse.success(notificationService.listMine(page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.success(Map.of("count", notificationService.unreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.success();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.success();
    }
}
