package com.thunder11.scuad.jobposting.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.type.AnalysisType;

@Getter
@AllArgsConstructor
public class AiAnalysisCreateEvent {

    private final Long userId;
    private final Long jobPostingId;
    private final AnalysisType analysisType;
}