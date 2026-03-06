package com.thunder11.scuad.chat.service;

import com.thunder11.scuad.jobposting.domain.AiApplicantComparison;
import com.thunder11.scuad.jobposting.repository.AiApplicantComparisonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// AI 비교 결과 저장 전용 헬퍼 빈
//
// 분리 이유:
//   ChatMemberComparisonService.compare()는 @Transactional이므로,
//   내부에서 save()가 DataIntegrityViolationException을 던지면
//   JPA 세션이 오염(rollback-only)되어 catch 후 findBy 시 AssertionFailure 발생.
//
//   이 헬퍼를 REQUIRES_NEW로 선언하면 호출마다 독립 트랜잭션이 생성되고,
//   예외 발생 시 해당 트랜잭션만 롤백 → 외부 세션은 오염되지 않음.
//   → catch 블록에서 깨끗한 세션으로 findBy 조회 가능
@Slf4j
@Component
@RequiredArgsConstructor
public class AiComparisonSaveHelper {

    private final AiApplicantComparisonRepository aiApplicantComparisonRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiApplicantComparison trySave(AiApplicantComparison comparison) {
        AiApplicantComparison saved = aiApplicantComparisonRepository.save(comparison);
        log.info("AI 비교 결과 저장 완료: comparisonId={}", saved.getId());
        return saved;
    }
}
