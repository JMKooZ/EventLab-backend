package com.eventlab.domain.participant.dto;

public record ParticipantResponse(
        Long participantId,
        Long participationId,
        Long eventId,
        String nickname,
        String status
) {
}