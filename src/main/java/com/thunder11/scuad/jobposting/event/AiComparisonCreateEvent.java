package com.thunder11.scuad.jobposting.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiComparisonCreateEvent {

    // AiEvalJob PK — 트랜잭션 커밋 후 MQ 발행 시 evalJobId로 바로 조회하기 위해 사용
    private final Long evalJobId;
    private final Long userId;
    private final Long jobPostingId;
    private final Long competitorUserId;
}
