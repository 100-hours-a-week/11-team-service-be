package com.thunder11.scuad.infra.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiApiResponse<T> {

    private boolean success;
    private String timestamp;
    private T data;
    private AiError error;

    @Getter
    @NoArgsConstructor
    public static class AiError {

        private String code;
        private String message;
    }
}
