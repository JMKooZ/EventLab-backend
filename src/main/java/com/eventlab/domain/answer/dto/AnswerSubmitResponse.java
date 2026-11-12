package com.eventlab.domain.answer.dto;

public record AnswerSubmitResponse(
        Long participationId,
        String status,
        int answeredCount
) {
}