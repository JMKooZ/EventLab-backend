package com.eventlab.domain.event.dto;

import java.time.LocalDateTime;

public record EventQuestionResponse(
        Long id,
        Long eventId,
        String questionType,
        String title,
        String description,
        Boolean required,
        Integer sortOrder,
        Integer ratingMin,
        Integer ratingMax,
        LocalDateTime createdAt
) {
    public record QuestionOptionResponse(
            Long id,
            Long questionId,
            String optionText,
            Integer sortOrder,
            Boolean isOther
    ) {
    }
}