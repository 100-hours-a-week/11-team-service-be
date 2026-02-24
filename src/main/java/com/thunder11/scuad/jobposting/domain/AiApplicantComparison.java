package com.thunder11.scuad.jobposting.domain;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "ai_applicant_comparison",
        indexes = {
                // (my_application_id, competitor_application_id) 조합으로 중복 체크 조회 최적화
                @Index(name = "idx_comparison_my_competitor",
                        columnList = "my_application_id, competitor_application_id")
        }
)

public class AiApplicantComparison extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comparison_id")
    private Long id;

    // 비교 기준 공고 (같은 공고에 지원한 사람끼리만 비교 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_master_id", nullable = false)
    private JobMaster jobMaster;

    // 비교를 요청한 본인의 지원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "my_application_id", nullable = false)
    private JobApplication myApplication;

    // 비교 대상의 지원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_application_id", nullable = false)
    private JobApplication competitorApplication;

    // AI가 반환한 항목별 비교 점수 리스트 (JSON 저장)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comparison_metrics", columnDefinition = "json", nullable = false)
    private List<ComparisonMetric> comparisonMetrics;

    @Column(name = "strengths_report", nullable = false, columnDefinition = "TEXT")
    private String strengthsReport;

    @Column(name = "weaknesses_report", nullable = false, columnDefinition = "TEXT")
    private String weaknessesReport;

    @Builder
    public AiApplicantComparison(
            JobMaster jobMaster,
            JobApplication myApplication,
            JobApplication competitorApplication,
            List<ComparisonMetric> comparisonMetrics,
            String strengthsReport,
            String weaknessesReport
    ) {
        this.jobMaster = jobMaster;
        this.myApplication = myApplication;
        this.competitorApplication = competitorApplication;
        this.comparisonMetrics = comparisonMetrics;
        this.strengthsReport = strengthsReport;
        this.weaknessesReport = weaknessesReport;
    }
}