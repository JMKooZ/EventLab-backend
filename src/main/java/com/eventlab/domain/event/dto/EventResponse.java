package com.eventlab.domain.event.dto;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        Long hostUserId,
        String title,
        String description,
        String inviteCode,
        String status,
        Integer maxParticipants,
        LocalDateTime createdAt
) {
}
