package com.thunder11.scuad.jobposting.dto.response;

import lombok.Getter;
import lombok.AllArgsConstructor;

import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;

@Getter
@AllArgsConstructor
public class JobPostingConfirmResponse {

    private Long jobMasterId;
    private Long jobPostingId;
    private RegistrationStatus registrationStatus;

}
