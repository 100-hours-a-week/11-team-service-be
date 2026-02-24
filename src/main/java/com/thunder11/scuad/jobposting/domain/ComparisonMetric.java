package com.thunder11.scuad.jobposting.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonMetric {

    private String name;

    @JsonProperty("my_score")
    private Integer myScore;

    @JsonProperty("competitor_score")
    private Integer competitorScore;
}