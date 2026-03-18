package com.thunder11.scuad.infra.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestMessage {
    @JsonProperty("eval_job_id")
    private String evalJobId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("job_posting_id")
    private String jobPostingId;
    // COMPARISON 타입 전용: 비교 대상 사용자 ID
    // AI 서버가 두 지원자를 비교하려면 경쟁자 ID가 필요하므로 추가.
    // EVALUATION/RESUME/PORTFOLIO 요청 시에는 null로 전송되며 기존 동작에 영향 없음.
    @JsonProperty("competitor")
    private String competitor;
}
