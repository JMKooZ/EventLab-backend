package com.eventlab.domain.answer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerInsertCommand {
    private Long id;
    private Long eventId;
    private Long participationId;
    private Long questionId;
    private String answerText;
    private Integer ratingValue;
    private String otherText;
}