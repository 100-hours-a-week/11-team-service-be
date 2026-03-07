package com.thunder11.scuad.notification.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.notification.domain.Notification;

@Getter
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String body;
    private String type;
    private Long refId;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .refId(notification.getRefId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}