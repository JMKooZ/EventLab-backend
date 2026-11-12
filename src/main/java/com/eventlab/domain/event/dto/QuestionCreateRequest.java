package com.eventlab.domain.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionCreateRequest(

        @NotBlank(message = "질문 유형을 입력해주세요.")
        @Pattern(
                regexp = "SINGLE_CHOICE|MULTIPLE_CHOICE|SHORT_TEXT|LONG_TEXT|RATING",
                message = "올바르지 않은 질문 유형입니다."
        )
        String questionType,

        @NotBlank(message = "질문 제목을 입력해주세요.")
        @Size(max = 500, message = "질문 제목은 500자 이하여야 합니다.")
        String title,

        String description,

        Boolean required,

        Integer ratingMin,

        Integer ratingMax,

        @Valid
        List<QuestionOptionRequest> options
) {
    public QuestionCreateRequest {
        if (required == null) {
            required = true;
        }
    }

    public record QuestionOptionRequest(
            @NotBlank(message = "선택지 내용을 입력해주세요.")
            @Size(max = 500, message = "선택지는 500자 이하여야 합니다.")
            String optionText,

            Boolean isOther
    ) {
        public QuestionOptionRequest {
            if (isOther == null) {
                isOther = false;
            }
        }
    }
}