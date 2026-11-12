package com.eventlab.domain.participant.mapper;

import com.eventlab.domain.participant.dto.NonUserInsertCommand;
import com.eventlab.domain.participant.dto.ParticipantInsertCommand;
import com.eventlab.domain.participant.dto.ParticipantResponse;
import com.eventlab.domain.participant.dto.ParticipationInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipantMapper {
    int insertNonUser(NonUserInsertCommand command);

    Long findNonUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    int touchNonUser(@Param("nonUserId") Long nonUserId);

    int insertParticipant(ParticipantInsertCommand command);

    int insertParticipation(@Param("eventId") Long eventId, @Param("participantId") Long participantId);

    ParticipantResponse findByEventIdAndNonUserId(@Param("eventId") Long eventId, @Param("nonUserId") Long nonUserId);

    ParticipantResponse findByEventIdAndParticipantId(@Param("eventId") Long eventId, @Param("participantId") Long participantId);

    ParticipationInfo findParticipationForUpdate(@Param("eventId") Long eventId, @Param("nonUserId") Long nonUserId);

    int markParticipationSubmitted(@Param("participationId") Long participationId);

}
