package com.eventlab.domain.answer.controller;

import com.eventlab.domain.answer.dto.AnswerSubmitRequest;
import com.eventlab.domain.answer.dto.AnswerSubmitResponse;
import com.eventlab.domain.answer.service.AnswerService;
import com.eventlab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/answers")
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnswerSubmitResponse>> submit(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Reconnect-Token", required = false) String reconnectToken,
            @Valid @RequestBody AnswerSubmitRequest request
    ) {
        AnswerSubmitResponse response = answerService.submit(eventId, reconnectToken, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "답변이 제출되었습니다."));
    }
}