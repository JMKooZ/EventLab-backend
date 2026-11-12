package com.eventlab.domain.answer.service;

import com.eventlab.domain.answer.dto.EventResultResponse;
import com.eventlab.domain.answer.dto.ResultRows;
import com.eventlab.domain.answer.mapper.ResultMapper;
import com.eventlab.domain.event.dto.EventQuestionResponse;
import com.eventlab.domain.event.dto.EventResponse;
import com.eventlab.domain.event.mapper.EventMapper;
import com.eventlab.domain.event.mapper.EventQuestionMapper;
import com.eventlab.domain.participant.dto.ParticipantResponse;
import com.eventlab.domain.participant.service.ParticipantService;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventResultService {

    private static final int TEXT_LIMIT_PER_QUESTION = 50;

    private final EventMapper eventMapper;
    private final EventQuestionMapper eventQuestionMapper;
    private final ResultMapper resultMapper;
    private final ParticipantService participantService;

    @Value("${eventlab.dev-host-user-id}")
    private Long devHostUserId;

    // readOnly 트랜잭션: 여러 쿼리가 같은 스냅샷을 봐서 집계끼리 어긋나지 않는다
    @Transactional(readOnly = true)
    public EventResultResponse getResultsForHost(Long eventId) {
        EventResponse event = eventMapper.findByIdAndHostUserId(eventId, devHostUserId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return buildResults(event, true);
    }

    // 참여자용: 제출을 마친 참여자만 볼 수 있고, 서술형 원문은 제외한 집계만 내려간다
    @Transactional(readOnly = true)
    public EventResultResponse getResultsForParticipant(Long eventId, String reconnectToken) {
        ParticipantResponse me = participantService.getMe(eventId, reconnectToken);
        if (!"SUBMITTED".equals(me.status())) {
            throw new BusinessException(ErrorCode.RESULT_NOT_AVAILABLE);
        }

        EventResponse event = eventMapper.findById(eventId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return buildResults(event, false);
    }

    // 호스트 확인 없이 집계만 만든다. 참여자용 공개 조회를 만들 때 재사용할 수 있다.
    private EventResultResponse buildResults(EventResponse event, boolean includeTexts) {
        Long eventId = event.id();

        ResultRows.ParticipationStats stats = resultMapper.findParticipationStats(eventId);
        List<EventQuestionResponse> questions = eventQuestionMapper.findQuestionsByEventId(eventId);

        Map<Long, Long> answeredCounts = resultMapper.findAnswerCounts(eventId).stream()
                .collect(Collectors.toMap(ResultRows.AnswerCountRow::questionId, ResultRows.AnswerCountRow::answeredCount));
        Map<Long, List<ResultRows.OptionCountRow>> optionRows = resultMapper.findOptionCounts(eventId).stream()
                .collect(Collectors.groupingBy(ResultRows.OptionCountRow::questionId));
        Map<Long, List<ResultRows.RatingCountRow>> ratingRows = resultMapper.findRatingCounts(eventId).stream()
                .collect(Collectors.groupingBy(ResultRows.RatingCountRow::questionId));
        Map<Long, List<String>> textsByQuestionId = includeTexts
                ? resultMapper.findTextAnswers(eventId, TEXT_LIMIT_PER_QUESTION).stream()
                .collect(Collectors.groupingBy(
                        ResultRows.TextAnswerRow::questionId,
                        Collectors.mapping(ResultRows.TextAnswerRow::textValue, Collectors.toList())
                ))
                : Map.of();

        List<EventResultResponse.QuestionResult> results = new ArrayList<>();
        for (EventQuestionResponse question : questions) {
            long answered = answeredCounts.getOrDefault(question.id(), 0L);
            List<EventResultResponse.OptionResult> options = List.of();
            EventResultResponse.RatingResult rating = null;

            switch (question.questionType()) {
                case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> options = optionRows
                        .getOrDefault(question.id(), List.of()).stream()
                        .map(row -> new EventResultResponse.OptionResult(
                                row.optionId(),
                                row.optionText(),
                                row.isOther(),
                                row.selectedCount(),
                                percentage(row.selectedCount(), answered)
                        ))
                        .toList();
                case "RATING" -> rating = buildRating(question, ratingRows.getOrDefault(question.id(), List.of()));
                default -> {
                    // SHORT_TEXT, LONG_TEXT: texts만 사용
                }
            }

            results.add(new EventResultResponse.QuestionResult(
                    question.id(),
                    question.questionType(),
                    question.title(),
                    question.required(),
                    answered,
                    options,
                    rating,
                    textsByQuestionId.getOrDefault(question.id(), List.of())
            ));
        }

        return new EventResultResponse(
                eventId,
                event.title(),
                event.status(),
                new EventResultResponse.ParticipationSummary(
                        stats.totalCount(), stats.submittedCount(), stats.inProgressCount()
                ),
                results
        );
    }

    private EventResultResponse.RatingResult buildRating(EventQuestionResponse question, List<ResultRows.RatingCountRow> rows) {
        Map<Integer, Long> countByValue = rows.stream()
                .collect(Collectors.toMap(ResultRows.RatingCountRow::ratingValue, ResultRows.RatingCountRow::ratingCount));

        // 질문의 min~max 전 구간을 채워서 반환 (응답이 없는 점수는 0)
        List<EventResultResponse.RatingBucket> distribution = new ArrayList<>();
        long total = 0;
        long sum = 0;
        for (int value = question.ratingMin(); value <= question.ratingMax(); value++) {
            long count = countByValue.getOrDefault(value, 0L);
            distribution.add(new EventResultResponse.RatingBucket(value, count));
            total += count;
            sum += (long) value * count;
        }

        Double average = total == 0 ? null : Math.round(sum * 100.0 / total) / 100.0;
        return new EventResultResponse.RatingResult(average, distribution);
    }

    private static double percentage(long count, long answered) {
        if (answered == 0) {
            return 0.0;
        }
        return Math.round(count * 1000.0 / answered) / 10.0;
    }
}