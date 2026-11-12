package com.eventlab.domain.answer.dto;

public final class ResultRows {

    private ResultRows() {
    }

    public record ParticipationStats(Long totalCount, Long submittedCount, Long inProgressCount) {
    }

    public record AnswerCountRow(Long questionId, Long answeredCount) {
    }

    public record OptionCountRow(Long questionId, Long optionId, String optionText, Boolean isOther, Long selectedCount) {
    }

    public record RatingCountRow(Long questionId, Integer ratingValue, Long ratingCount) {
    }

    public record TextAnswerRow(Long questionId, String textValue) {
    }
}