package com.eventlab.domain.event.dto;

import java.util.List;

public record EventQuestionDetailResponse(
        Long id,
        Long eventId,
        String questionType,
        String title,
        String description,
        Boolean required,
        Integer sortOrder,
        Integer ratingMin,
        Integer ratingMax,
        java.time.LocalDateTime createdAt,
        List<EventQuestionResponse.QuestionOptionResponse> options
) {
}