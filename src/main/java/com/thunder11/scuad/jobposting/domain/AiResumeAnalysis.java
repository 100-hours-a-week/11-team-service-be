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
@Table(name = "ai_resume_analysis")
@SQLDelete(sql = "UPDATE ai_resume_analysis SET deleted_at = CURRENT_TIME WHERE resume_analysis_id = ?")
@Where(clause = "deleted_at IS NULL")
public class AiResumeAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_analysis_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "ai_analysis_report", nullable = false, columnDefinition = "TEXT")
    private String aiAnalysisReport;

    @Column(name = "job_fit_score", nullable = false, columnDefinition = "TEXT")
    private String jobFitScore;

    @Column(name = "experience_clarity_score", nullable = false, columnDefinition = "TEXT")
    private String experienceClarityScore;

    @Column(name = "readability_score", nullable = false, columnDefinition = "TEXT")
    private String readabilityScore;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public AiResumeAnalysis(JobApplication jobApplication, String aiAnalysisReport,
                            String jobFitScore, String experienceClarityScore, String readabilityScore) {
        this.jobApplication = jobApplication;
        this.aiAnalysisReport = aiAnalysisReport;
        this.jobFitScore = jobFitScore;
        this.experienceClarityScore = experienceClarityScore;
        this.readabilityScore = readabilityScore;
    }

    public void update(String aiAnalysisReport, String jobFitScore,
                       String experienceClarityScore, String readabilityScore) {
        this.aiAnalysisReport = aiAnalysisReport;
        this.jobFitScore = jobFitScore;
        this.experienceClarityScore = experienceClarityScore;
        this.readabilityScore = readabilityScore;
    }
}
