package com.eventlab.domain.event.mapper;

import com.eventlab.domain.event.dto.EventResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMapper {
    int countByInviteCode(@Param("inviteCode") String inviteCode);

    int insertEvent(
            @Param("hostUserId") Long hostUserId,
            @Param("title") String title,
            @Param("description") String description,
            @Param("inviteCode") String inviteCode,
            @Param("maxParticipants") Integer maxParticipants
    );

    EventResponse findById(@Param("id") Long id);

    EventResponse findByInviteCode(@Param("inviteCode") String inviteCode);

    List<EventResponse> findByHostUserId(
            @Param("hostUserId") Long hostUserId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countByHostUserId(@Param("hostUserId") Long hostUserId);

    EventResponse findByIdAndHostUserId(@Param("id") Long id, @Param("hostUserId") Long hostUserId);

    int updateEvent(
            @Param("id") Long id,
            @Param("hostUserId") Long hostUserId,
            @Param("title") String title,
            @Param("description") String description,
            @Param("maxParticipants") Integer maxParticipants
    );

    long countParticipantsByEventId(@Param("eventId") Long eventId);

    int updateStatus(
            @Param("id") Long id,
            @Param("hostUserId") Long hostUserId,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus
    );

    EventResponse findByIdForUpdate(@Param("id") Long id);
}
