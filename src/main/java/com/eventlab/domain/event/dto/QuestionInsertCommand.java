package com.eventlab.domain.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionInsertCommand {
    private Long id;
    private Long eventId;
    private String questionType;
    private String title;
    private String description;
    private Boolean required;
    private Integer sortOrder;
    private Integer ratingMin;
    private Integer ratingMax;
}
