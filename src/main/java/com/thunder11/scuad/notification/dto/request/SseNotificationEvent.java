package com.thunder11.scuad.notification.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SseNotificationEvent {
    private Long notificationId;
    private String type;
    private String title;
    private String body;
    private Long refId;
}