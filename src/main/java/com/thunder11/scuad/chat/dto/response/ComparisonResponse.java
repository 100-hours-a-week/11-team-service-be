package com.thunder11.scuad.chat.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.AiApplicantComparison;

@Getter
@Builder
public class ComparisonResponse {

    private List<ComparisonMetricResponse> comparisonMetrics;
    private String strengthsReport;
    private String weaknessesReport;

    public static ComparisonResponse from(AiApplicantComparison comparison) {
        List<ComparisonMetricResponse> metrics = comparison.getComparisonMetrics().stream()
                .map(m -> ComparisonMetricResponse.builder()
                        .name(m.getName())
                        .myScore(m.getMyScore())
                        .competitorScore(m.getCompetitorScore())
                        .build())
                .collect(Collectors.toList());

        return ComparisonResponse.builder()
                .comparisonMetrics(metrics)
                .strengthsReport(comparison.getStrengthsReport())
                .weaknessesReport(comparison.getWeaknessesReport())
                .build();
    }

    @Getter
    @Builder
    public static class ComparisonMetricResponse {
        private String name;
        private Integer myScore;
        private Integer competitorScore;
    }
}