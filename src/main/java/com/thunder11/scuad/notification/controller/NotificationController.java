package com.thunder11.scuad.notification.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.notification.dto.response.NotificationResponse;
import com.thunder11.scuad.notification.service.NotificationService;
import com.thunder11.scuad.notification.service.SseEmitterRegistry;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final SseEmitterRegistry emitterRegistry;
    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal principal) {
        return emitterRegistry.register(principal.getUserId());
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.of(200, "SUCCESS", "알림 조회 완료",
                notificationService.getNotifications(principal.getUserId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.of(200, "SUCCESS", "안 읽은 개수 조회",
                notificationService.getUnreadCount(principal.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(notificationId, principal.getUserId());
        return ApiResponse.of(200, "SUCCESS", "읽음 처리 완료");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getUserId());
        return ApiResponse.of(200, "SUCCESS", "전체 읽음 처리 완료");
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> delete(@PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.delete(notificationId, principal.getUserId());
        return ApiResponse.of(200, "SUCCESS", "삭제 완료");
    }
}