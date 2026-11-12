package com.eventlab.domain.event.dto;

import jakarta.validation.constraints.*;

public record EventUpdateRequest(
        @NotBlank(message = "이벤트 제목을 입력해주세요.")
        @Size(max = 200, message = "이벤트 제목은 200자 이하여야 합니다.")
        String title,

        String description,

        @NotNull(message = "최대 참여자 수를 입력해주세요.")
        @Min(value = 1, message = "최대 참여자 수는 1명 이상이어야 합니다.")
        @Max(value = 10000, message = "최대 참여자 수는 10,000명 이하여야 합니다.")
        Integer maxParticipants
) {
}
