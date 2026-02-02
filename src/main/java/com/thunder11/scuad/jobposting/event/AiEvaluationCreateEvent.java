package com.thunder11.scuad.jobposting.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiEvaluationCreateEvent {

    private final Long userId;
    private final Long jobPostingId;
}