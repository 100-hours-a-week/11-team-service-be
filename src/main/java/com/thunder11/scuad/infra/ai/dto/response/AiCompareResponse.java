package com.thunder11.scuad.infra.ai.dto.response;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompareResponse {

    @JsonProperty("comparison_metrics")
    private List<ComparisonMetricItem> comparisonMetrics;

    @JsonProperty("strengths_report")
    private String strengthsReport;

    @JsonProperty("weaknesses_report")
    private String weaknessesReport;

    @Getter
    @NoArgsConstructor
    public static class ComparisonMetricItem {

        @JsonProperty("name")
        private String name;

        @JsonProperty("my_score")
        private Integer myScore;

        @JsonProperty("competitor_score")
        private Integer competitorScore;
    }
}