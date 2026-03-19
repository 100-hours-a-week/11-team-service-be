package com.thunder11.scuad.jobposting.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiJobPostingAnalysisCreateEvent {
    private final Long evalJobId;
    private final Long userId;
}
