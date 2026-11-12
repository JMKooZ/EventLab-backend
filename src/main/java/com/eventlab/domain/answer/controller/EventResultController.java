package com.eventlab.domain.answer.controller;

import com.eventlab.domain.answer.dto.EventResultResponse;
import com.eventlab.domain.answer.service.EventResultService;
import com.eventlab.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/results")
public class EventResultController {

    private final EventResultService eventResultService;

    @GetMapping
    public ApiResponse<EventResultResponse> getResults(@PathVariable Long eventId) {
        return ApiResponse.success(eventResultService.getResultsForHost(eventId), "이벤트 결과 조회에 성공했습니다.");
    }
}