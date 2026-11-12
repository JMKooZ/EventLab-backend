package com.eventlab.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NonUserInsertCommand {
    private Long id;
    private String reconnectTokenHash;
}