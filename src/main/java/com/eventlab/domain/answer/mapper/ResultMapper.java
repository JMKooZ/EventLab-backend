package com.eventlab.domain.answer.mapper;

import com.eventlab.domain.answer.dto.ResultRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResultMapper {

    ResultRows.ParticipationStats findParticipationStats(@Param("eventId") Long eventId);

    List<ResultRows.AnswerCountRow> findAnswerCounts(@Param("eventId") Long eventId);

    List<ResultRows.OptionCountRow> findOptionCounts(@Param("eventId") Long eventId);

    List<ResultRows.RatingCountRow> findRatingCounts(@Param("eventId") Long eventId);

    List<ResultRows.TextAnswerRow> findTextAnswers(@Param("eventId") Long eventId, @Param("limit") int limit);
}