package com.eventlab.domain.answer.mapper;

import com.eventlab.domain.answer.dto.AnswerInsertCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerMapper {

    int insertAnswer(AnswerInsertCommand command);

    int insertAnswerOptions(
            @Param("answerId") Long answerId,
            @Param("questionId") Long questionId,
            @Param("optionIds") List<Long> optionIds
    );
}