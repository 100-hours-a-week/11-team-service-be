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

    /**
     * [테스트 전용] 동기 블로킹 스레드 점유 재현용 더미 응답 — 테스트 후 반드시 제거
     *
     * 의도: AI 서버 없는 로컬 환경에서도 3초 지연 후 정상 응답을 반환하여
     *       스레드 풀 소진 현상을 수치로 측정하기 위한 픽스처.
     */
    public static AiCompareResponse dummy() {
        ComparisonMetricItem metric1 = new ComparisonMetricItem("기술 스택 숙련도", 85, 78);
        ComparisonMetricItem metric2 = new ComparisonMetricItem("프로젝트 복잡도", 72, 80);
        ComparisonMetricItem metric3 = new ComparisonMetricItem("업무 경력 기간", 90, 75);
        return AiCompareResponse.builder()
                .comparisonMetrics(List.of(metric1, metric2, metric3))
                .strengthsReport("[테스트] 백엔드 경력이 더 길고 기술 문서 능력이 우수합니다.")
                .weaknessesReport("[테스트] 대규모 프로젝트 경험이 상대적으로 부족합니다.")
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonMetricItem {

        @JsonProperty("name")
        private String name;

        @JsonProperty("my_score")
        private Integer myScore;

        @JsonProperty("competitor_score")
        private Integer competitorScore;
    }
}