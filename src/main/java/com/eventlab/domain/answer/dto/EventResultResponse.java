package com.eventlab.domain.answer.dto;

import java.util.List;

public record EventResultResponse(
        Long eventId,
        String title,
        String status,
        ParticipationSummary participation,
        List<QuestionResult> questions
) {
    public record ParticipationSummary(long totalCount, long submittedCount, long inProgressCount) {
    }

    public record QuestionResult(
            Long questionId,
            String questionType,
            String title,
            Boolean required,
            long answeredCount,
            List<OptionResult> options,   // 선택형만, 그 외는 빈 목록
            RatingResult rating,          // RATING만, 그 외는 null
            List<String> texts            // 서술형 답변 / 선택형의 기타 내용 (최신순, 최대 50개)
    ) {
    }

    public record OptionResult(Long optionId, String optionText, Boolean isOther, long selectedCount, double percentage) {
    }

    public record RatingResult(Double average, List<RatingBucket> distribution) {
    }

    public record RatingBucket(int value, long count) {
    }
}