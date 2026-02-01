package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_applicant_evaluation")
@SQLDelete(sql = "UPDATE ai_applicant_evaluation SET deleted_at = NOW() WHERE evaluation_id = ?")
@Where(clause = "deleted_at IS NULL")
public class AiApplicantEvaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "one_line_review", nullable = false, columnDefinition = "TEXT")
    private String oneLineReview;

    @Column(name = "feedback_detail", nullable = false, columnDefinition = "TEXT")
    private String feedbackDetail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "comparison_scores", columnDefinition = "json", nullable = false)
    private List<EvaluationScore> comparisonScores;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public AiApplicantEvaluation(JobApplication jobApplication, Integer overallScore,
            String oneLineReview, String feedbackDetail,
            List<EvaluationScore> comparisonScores) {
        this.jobApplication = jobApplication;
        this.overallScore = overallScore;
        this.oneLineReview = oneLineReview;
        this.feedbackDetail = feedbackDetail;
        this.comparisonScores = comparisonScores;
    }
}