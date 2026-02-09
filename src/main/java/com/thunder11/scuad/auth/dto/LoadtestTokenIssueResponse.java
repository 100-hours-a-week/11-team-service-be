package com.thunder11.scuad.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoadtestTokenIssueResponse {
    private Long userId;
    private String nickname;
    private String accessToken;
    private String tokenType;
}
