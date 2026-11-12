package com.eventlab.domain.event.controller;

import com.eventlab.domain.event.dto.EventQuestionDetailResponse;
import com.eventlab.domain.event.dto.QuestionCreateRequest;
import com.eventlab.domain.event.dto.QuestionReorderRequest;
import com.eventlab.domain.event.dto.QuestionUpdateRequest;
import com.eventlab.domain.event.service.EventQuestionService;
import com.eventlab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/questions")
public class EventQuestionController {
    private final EventQuestionService eventQuestionService;

    @PostMapping
    public ApiResponse<EventQuestionDetailResponse> createQuestion(@PathVariable Long eventId, @Valid @RequestBody QuestionCreateRequest request) {
        EventQuestionDetailResponse response = eventQuestionService.createQuestion(eventId, request);
        return ApiResponse.success(response, "질문 생성에 성공했습니다.");
    }

    @GetMapping
    public ApiResponse<List<EventQuestionDetailResponse>> getQuestions(@PathVariable Long eventId){
        List<EventQuestionDetailResponse> response = eventQuestionService.getQuestions(eventId);

        return ApiResponse.success(response, "질문을 조회에 성공했습니다.");
    }

    @PutMapping("/{questionId}")
    public ApiResponse<EventQuestionDetailResponse> updateQuestion(
            @PathVariable Long eventId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        EventQuestionDetailResponse response =
                eventQuestionService.updateQuestion(eventId, questionId, request);

        return ApiResponse.success(response, "질문 수정에 성공했습니다.");
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long eventId,
            @PathVariable Long questionId
    ) {
        eventQuestionService.deleteQuestion(eventId, questionId);

        return ApiResponse.success(null, "질문 삭제에 성공했습니다.");
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorderQuestions(
            @PathVariable Long eventId,
            @Valid @RequestBody QuestionReorderRequest request
    ) {
        eventQuestionService.reorderQuestions(eventId, request);

        return ApiResponse.success(null, "질문 순서 변경에 성공했습니다.");
    }
}
