package com.thunder11.scuad.infra.ai.client;

import com.thunder11.scuad.infra.ai.dto.request.AiCompareRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiCompareResponse;

// AI 비교 분석 호출 추상화 인터페이스
// 도입 의도: ChatMemberComparisonService가 이 인터페이스에만 의존하게 하여
//   - 프로덕션: AiServiceClient가 실제 AI 서버 호출
//   - 로컬:     MockAiCompareClient(@Primary)가 Thread.sleep으로 지연 시뮬레이션
//              → AI 서버 없이도 이슈8 race condition 로컬 재현 가능
public interface AiComparePort {
    AiCompareResponse compareApplicants(AiCompareRequest request);
}
