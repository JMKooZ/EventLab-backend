package com.eventlab.domain.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(
        @NotBlank(message = "이벤트 제목을 입력해주세요.")
        @Size(max = 200, message = "이벤트 제목은 200자 이하여야 합니다.")
        String title,

        String description,

        @Min(value = 1, message = "최대 참여자 수는 1명 이상이어야 합니다.")
        @Max(value = 10000, message = "최대 참여자 수는 10,000명 이하여야 합니다.")
        Integer maxParticipants
) {
    public EventCreateRequest {
        if (maxParticipants == null) {
            maxParticipants = 100;
        }
    }
}
