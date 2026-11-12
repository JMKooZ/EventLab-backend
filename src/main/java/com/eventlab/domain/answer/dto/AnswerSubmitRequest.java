package com.eventlab.domain.answer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnswerSubmitRequest(
        @NotNull(message = "답변 목록을 입력해주세요.")
        List<@NotNull(message = "답변 항목이 올바르지 않습니다.") @Valid AnswerItem> answers
) {
    public record AnswerItem(
            @NotNull(message = "질문 ID를 입력해주세요.")
            Long questionId,

            List<Long> optionIds,       // SINGLE_CHOICE / MULTIPLE_CHOICE
            String text,                // SHORT_TEXT / LONG_TEXT
            Integer ratingValue,        // RATING

            @Size(max = 1000, message = "기타 내용은 1000자 이하여야 합니다.")
            String otherText            // 기타 선택지를 고른 경우
    ) {
    }
}