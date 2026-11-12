package com.eventlab.domain.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionUpdateRequest(
        @NotBlank
        @Pattern(regexp = "SINGLE_CHOICE|MULTIPLE_CHOICE|SHORT_TEXT|LONG_TEXT|RATING")
        String questionType,

        @NotBlank
        @Size(max = 500)
        String title,

        String description,

        Boolean required,

        Integer ratingMin,

        Integer ratingMax,

        @Valid
        List<QuestionOptionUpdateRequest> options
) {
    public QuestionUpdateRequest {
        if (required == null) {
            required = true;
        }
    }

    public record QuestionOptionUpdateRequest(
            Long id,

            @NotBlank
            @Size(max = 500)
            String optionText,

            Boolean isOther
    ) {
        public QuestionOptionUpdateRequest {
            if (isOther == null) {
                isOther = false;
            }
        }
    }
}