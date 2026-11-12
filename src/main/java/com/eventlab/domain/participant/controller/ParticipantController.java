package com.eventlab.domain.participant.controller;

import com.eventlab.domain.answer.dto.EventResultResponse;
import com.eventlab.domain.answer.service.EventResultService;
import com.eventlab.domain.event.dto.EventQuestionDetailResponse;
import com.eventlab.domain.participant.dto.ParticipantCreateRequest;
import com.eventlab.domain.participant.dto.ParticipantJoinResponse;
import com.eventlab.domain.participant.dto.ParticipantResponse;
import com.eventlab.domain.participant.service.ParticipantService;
import com.eventlab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final EventResultService eventResultService;

    @PostMapping
    public ResponseEntity<ApiResponse<ParticipantJoinResponse>> join(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Reconnect-Token", required = false) String reconnectToken,
            @Valid @RequestBody ParticipantCreateRequest request
    ) {
        ParticipantJoinResponse response = participantService.join(eventId, request, reconnectToken);

        if (response.reconnected()) {
            return ResponseEntity.ok(ApiResponse.success(response, "이미 참여 중인 이벤트입니다."));
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "이벤트 참여가 시작되었습니다."));
    }

    @GetMapping("/me")
    public ApiResponse<ParticipantResponse> getMe(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Reconnect-Token", required = false) String reconnectToken
    ) {
        return ApiResponse.success(participantService.getMe(eventId, reconnectToken), "참여 정보 조회에 성공했습니다.");
    }

    @GetMapping("/me/questions")
    public ApiResponse<List<EventQuestionDetailResponse>> getMyQuestions(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Reconnect-Token", required = false) String reconnectToken
    ) {
        return ApiResponse.success(
                participantService.getQuestionsForParticipant(eventId, reconnectToken),
                "질문 조회에 성공했습니다."
        );
    }

    @GetMapping("/me/results")
    public ApiResponse<EventResultResponse> getMyResults(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Reconnect-Token", required = false) String reconnectToken
    ) {
        return ApiResponse.success(
                eventResultService.getResultsForParticipant(eventId, reconnectToken),
                "이벤트 결과 조회에 성공했습니다."
        );
    }
}