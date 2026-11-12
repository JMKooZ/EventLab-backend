package com.eventlab.domain.event.mapper;

import com.eventlab.domain.event.dto.EventQuestionResponse;
import com.eventlab.domain.event.dto.QuestionInsertCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventQuestionMapper {

    int insertQuestion(QuestionInsertCommand command);

    int insertOption(@Param("questionId") Long questionId,
                     @Param("optionText") String optionText,
                     @Param("sortOrder") Integer sortOrder,
                     @Param("isOther") Boolean isOther);

    int findNextSortOrder(@Param("eventId") Long eventId);

    EventQuestionResponse findQuestionById(@Param("questionId") Long questionId);

    List<EventQuestionResponse.QuestionOptionResponse> findOptionsByQuestionId(@Param("questionId") Long questionId);

    List<EventQuestionResponse> findQuestionsByEventId(@Param("eventId") Long eventId);

    List<EventQuestionResponse.QuestionOptionResponse> findOptionsByEventId(@Param("eventId") Long eventId);

    EventQuestionResponse findQuestionByIdAndEventId(
            @Param("questionId") Long questionId,
            @Param("eventId") Long eventId
    );

    List<Long> findQuestionIdsByEventId(
            @Param("eventId") Long eventId
    );

    long countAnswersByQuestionId(
            @Param("questionId") Long questionId
    );

    long countAnswerOptionsByOptionId(
            @Param("optionId") Long optionId
    );

    int updateQuestion(
            @Param("questionId") Long questionId,
            @Param("eventId") Long eventId,
            @Param("questionType") String questionType,
            @Param("title") String title,
            @Param("description") String description,
            @Param("required") Boolean required,
            @Param("ratingMin") Integer ratingMin,
            @Param("ratingMax") Integer ratingMax
    );

    int updateOption(
            @Param("optionId") Long optionId,
            @Param("questionId") Long questionId,
            @Param("optionText") String optionText,
            @Param("isOther") Boolean isOther
    );

    int deleteOption(
            @Param("optionId") Long optionId,
            @Param("questionId") Long questionId
    );

    int deleteOptionsByQuestionId(
            @Param("questionId") Long questionId
    );

    int deleteQuestion(
            @Param("questionId") Long questionId,
            @Param("eventId") Long eventId
    );

    int updateQuestionSortOrder(
            @Param("questionId") Long questionId,
            @Param("eventId") Long eventId,
            @Param("sortOrder") Integer sortOrder
    );
}
