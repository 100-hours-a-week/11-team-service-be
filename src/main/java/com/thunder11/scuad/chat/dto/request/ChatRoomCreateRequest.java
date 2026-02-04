package com.thunder11.scuad.chat.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.chat.domain.type.RoomGoal;

// 채팅방 생성 요청
@Getter
@NoArgsConstructor
public class ChatRoomCreateRequest {

    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "채팅방 이름은 2~50자여야 합니다")
    private String roomName;

    @NotNull(message = "최대 인원은 필수입니다")
    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다")
    @Max(value = 5, message = "최대 인원은 5명 이하여야 합니다")
    private Integer maxParticipants;

    @NotNull(message = "채팅방 목표는 필수입니다")
    private RoomGoal roomGoal;

    @NotNull(message = "커트라인 점수는 필수입니다")
    @Min(value = 0, message = "커트라인 점수는 0 이상이어야 합니다")
    @Max(value = 100, message = "커트라인 점수는 100 이하여야 합니다")
    private Integer cutlineScore;

    @Size(max = 100, message = "우대사항은 100자 이하여야 합니다")
    private String preferredConditions;
}