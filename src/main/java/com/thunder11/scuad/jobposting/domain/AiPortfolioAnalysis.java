package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import lombok.*;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "ai_portfolio_analysis")
@SQLDelete(sql = "UPDATE ai_portfolio_analysis SET deleted_at = NOW() WHERE portfolio_analysis_id = ?")
@Where(clause = "deleted_at IS NULL")
public class AiPortfolioAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_analysis_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "ai_analysis_report", nullable = false, columnDefinition = "TEXT")
    private String aiAnalysisReport;

    @Column(name = "problem_solving_score", nullable = false, columnDefinition = "TEXT")
    private String problemSolvingScore;

    @Column(name = "contribution_clarity_score", nullable = false, columnDefinition = "TEXT")
    private String contributionClarityScore;

    @Column(name = "technical_depth_score", nullable = false, columnDefinition = "TEXT")
    private String technicalDepthScore;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public AiPortfolioAnalysis(JobApplication jobApplication, String aiAnalysisReport,
                               String problemSolvingScore, String contributionClarityScore, String technicalDepthScore) {
        this.jobApplication = jobApplication;
        this.aiAnalysisReport = aiAnalysisReport;
        this.problemSolvingScore = problemSolvingScore;
        this.contributionClarityScore = contributionClarityScore;
        this.technicalDepthScore = technicalDepthScore;
    }

    public void update(String aiAnalysisReport, String problemSolvingScore,
                       String contributionClarityScore, String technicalDepthScore) {
        this.aiAnalysisReport = aiAnalysisReport;
        this.problemSolvingScore = problemSolvingScore;
        this.contributionClarityScore = contributionClarityScore;
        this.technicalDepthScore = technicalDepthScore;
    }
}
