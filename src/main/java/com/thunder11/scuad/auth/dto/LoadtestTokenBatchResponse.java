package com.thunder11.scuad.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoadtestTokenBatchResponse {
    private Integer count;
    private List<LoadtestTokenIssueResponse> tokens;
}
