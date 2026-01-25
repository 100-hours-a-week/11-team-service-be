package com.thunder11.scuad.jobposting.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;

@Getter
@NoArgsConstructor
public class JobPostingConfirmRequest {

    @NotNull(message = "등록 상태 값은 필수입니다.")
    private RegistrationStatus registrationStatus;
}
