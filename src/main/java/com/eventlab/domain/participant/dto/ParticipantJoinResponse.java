package com.eventlab.domain.participant.dto;

public record ParticipantJoinResponse(
        Long participantId,
        Long participationId,
        Long eventId,
        String nickname,
        String status,
        String reconnectToken,   // 최초 발급 시에만 값이 있고, 이후에는 null
        boolean reconnected      // 이미 참여 중이던 게스트의 재접속이면 true
) {
    public static ParticipantJoinResponse of(ParticipantResponse p, String reconnectToken, boolean reconnected) {
        return new ParticipantJoinResponse(
                p.participantId(), p.participationId(), p.eventId(),
                p.nickname(), p.status(), reconnectToken, reconnected
        );
    }
}