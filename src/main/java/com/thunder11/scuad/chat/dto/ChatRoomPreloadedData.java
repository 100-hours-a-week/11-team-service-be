package com.thunder11.scuad.chat.dto;

import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.JobApplication;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

// getChatRoomsByJobPosting stream 시작 전 일괄 조회한 공통 데이터 묶음
//
// 사용 목적:
//   convertToSummary, determineJoinEligibility 에 개별 파라미터로 넘기면
//   메서드 시그니처가 과도하게 길어지므로 record로 묶어 전달
//
// 포함 데이터:
//   - participantCountMap : chatRoomId → 현재 인원 수 (IN 쿼리 1번)
//   - hostNicknameMap     : userId → 닉네임 (IN 쿼리 1번)
//   - joinedRoomIds       : 현재 사용자가 참여 중인 chatRoomId Set (IN 쿼리 1번)
//   - kickedRoomIds       : 현재 사용자가 강퇴된 chatRoomId Set (IN 쿼리 1번)
//   - jobApplication      : 현재 사용자의 지원서 (stream 밖 1회 조회)
//   - evaluation          : 현재 사용자의 AI 평가 (stream 밖 1회 조회)
//   - hasResume           : 현재 사용자의 이력서 제출 여부 (stream 밖 1회 조회)
//   - otherRoomMember     : 같은 공고 다른 방 참여 여부 (stream 밖 1회 조회)
public record ChatRoomPreloadedData(
        Map<Long, Long> participantCountMap,
        Map<Long, String> hostNicknameMap,
        Set<Long> joinedRoomIds,
        Set<Long> kickedRoomIds,
        JobApplication jobApplication,
        AiApplicantEvaluation evaluation,
        boolean hasResume,
        Optional<ChatRoomMember> otherRoomMember
) {
}
