package com.eventlab.domain.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionReorderRequest(
        @NotEmpty
        List<@NotNull Long> questionIds
) {
}