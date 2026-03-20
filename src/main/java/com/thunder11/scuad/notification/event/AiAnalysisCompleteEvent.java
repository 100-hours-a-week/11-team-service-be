package com.thunder11.scuad.notification.event;

public record AiAnalysisCompleteEvent(
    Long userId, 
    String type, 
    String jobPostingTitle, 
    Long applicationId
) {}
