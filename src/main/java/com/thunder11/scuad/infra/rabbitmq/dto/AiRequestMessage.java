package com.thunder11.scuad.infra.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestMessage {
    private String evalJobId;
    private String userId;
    private String jobPostingId;
}
