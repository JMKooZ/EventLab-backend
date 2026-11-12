package com.eventlab.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantInsertCommand {
    private Long id;
    private Long eventId;
    private Long nonUserId;
    private String nickname;
    private String nicknameKey;
}