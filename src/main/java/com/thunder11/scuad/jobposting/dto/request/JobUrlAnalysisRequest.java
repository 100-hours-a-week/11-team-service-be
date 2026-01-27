package com.thunder11.scuad.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JobUrlAnalysisRequest {

    @NotBlank(message = "url은 필수입니다.")
    private String url;

}
