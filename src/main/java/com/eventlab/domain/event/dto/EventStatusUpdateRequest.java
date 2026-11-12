package com.eventlab.domain.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EventStatusUpdateRequest(
        @NotBlank(message = "변경할 상태를 입력해주세요.")
        @Pattern(regexp = "RUNNING|PAUSED|ENDED", message = "상태는 RUNNING, PAUSED, ENDED 중 하나여야 합니다.")
        String status
) {
}