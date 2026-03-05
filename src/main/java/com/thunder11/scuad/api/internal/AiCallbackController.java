package com.thunder11.scuad.api.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.rabbitmq.dto.AiResponseMessage;
import com.thunder11.scuad.jobposting.service.AiResultProcessingService;

@Slf4j
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class AiCallbackController {
    private final AiResultProcessingService aiResultProcessingService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleAiResultCallback(
            @RequestBody AiResponseMessage response) {
        log.info("AI 분석 결과 콜백 수신 - evalJobId: {}, success: {}", response.getEvalJobId(), response.isSuccess());

        aiResultProcessingService.processResult(response.getEvalJobId(), response);

        return ResponseEntity.ok().build();
    }

}