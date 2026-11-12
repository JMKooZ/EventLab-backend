package com.eventlab.domain.answer.service;

import com.eventlab.domain.answer.dto.AnswerInsertCommand;
import com.eventlab.domain.answer.dto.AnswerSubmitRequest;
import com.eventlab.domain.answer.dto.AnswerSubmitResponse;
import com.eventlab.domain.answer.mapper.AnswerMapper;
import com.eventlab.domain.event.dto.EventQuestionResponse;
import com.eventlab.domain.event.dto.EventResponse;
import com.eventlab.domain.event.mapper.EventMapper;
import com.eventlab.domain.event.mapper.EventQuestionMapper;
import com.eventlab.domain.participant.dto.ParticipationInfo;
import com.eventlab.domain.participant.service.ParticipantService;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    // 정책 미정: 임시 상한값 (answers.answer_text는 TEXT 타입, other_text는 varchar(1000))
    private static final int SHORT_TEXT_MAX_LENGTH = 500;
    private static final int LONG_TEXT_MAX_LENGTH = 5000;

    private final EventMapper eventMapper;
    private final EventQuestionMapper eventQuestionMapper;
    private final AnswerMapper answerMapper;
    private final ParticipantService participantService;

    private record PreparedAnswer(AnswerInsertCommand command, List<Long> optionIds) {
    }

    @Transactional
    public AnswerSubmitResponse submit(Long eventId, String reconnectToken, AnswerSubmitRequest request) {
        EventResponse event = eventMapper.findById(eventId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (!"RUNNING".equals(event.status())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_OPEN);
        }

        // 본인 참여 행 잠금: 같은 사람의 동시 중복 제출 방지
        ParticipationInfo participation = participantService.getParticipationForUpdate(eventId, reconnectToken);
        if ("SUBMITTED".equals(participation.status())) {
            throw new BusinessException(ErrorCode.ALREADY_SUBMITTED);
        }

        List<EventQuestionResponse> questions = eventQuestionMapper.findQuestionsByEventId(eventId);
        Map<Long, List<EventQuestionResponse.QuestionOptionResponse>> optionsByQuestionId =
                eventQuestionMapper.findOptionsByEventId(eventId).stream()
                        .collect(Collectors.groupingBy(EventQuestionResponse.QuestionOptionResponse::questionId));
        Map<Long, AnswerSubmitRequest.AnswerItem> itemsByQuestionId = indexByQuestionId(request.answers(), questions);

        // 전부 검증하고 나서 저장 (검증 실패 시 아무것도 저장되지 않음)
        List<PreparedAnswer> prepared = new ArrayList<>();
        for (EventQuestionResponse question : questions) {
            PreparedAnswer answer = prepare(
                    eventId,
                    participation.participationId(),
                    question,
                    optionsByQuestionId.getOrDefault(question.id(), List.of()),
                    itemsByQuestionId.get(question.id())
            );
            if (answer != null) {
                prepared.add(answer);
            }
        }

        for (PreparedAnswer answer : prepared) {
            answerMapper.insertAnswer(answer.command());
            if (!answer.optionIds().isEmpty()) {
                answerMapper.insertAnswerOptions(
                        answer.command().getId(),
                        answer.command().getQuestionId(),
                        answer.optionIds()
                );
            }
        }

        participantService.markSubmitted(participation.participationId());

        log.info("답변 제출 eventId={}, participationId={}, answeredCount={}",
                eventId, participation.participationId(), prepared.size());

        return new AnswerSubmitResponse(participation.participationId(), "SUBMITTED", prepared.size());
    }

    private Map<Long, AnswerSubmitRequest.AnswerItem> indexByQuestionId(
            List<AnswerSubmitRequest.AnswerItem> items,
            List<EventQuestionResponse> questions
    ) {
        Set<Long> questionIds = questions.stream()
                .map(EventQuestionResponse::id)
                .collect(Collectors.toSet());

        Map<Long, AnswerSubmitRequest.AnswerItem> result = new HashMap<>();
        for (AnswerSubmitRequest.AnswerItem item : items) {
            if (!questionIds.contains(item.questionId())) {
                throw new BusinessException(ErrorCode.INVALID_ANSWER,
                        "이 이벤트에 속하지 않는 질문입니다. (questionId=" + item.questionId() + ")");
            }
            if (result.put(item.questionId(), item) != null) {
                throw new BusinessException(ErrorCode.INVALID_ANSWER,
                        "같은 질문에 대한 답변이 중복되었습니다. (questionId=" + item.questionId() + ")");
            }
        }
        return result;
    }

    /** 검증 후 저장할 답변을 만든다. 선택 질문을 건너뛴 경우 null. */
    private PreparedAnswer prepare(
            Long eventId,
            Long participationId,
            EventQuestionResponse question,
            List<EventQuestionResponse.QuestionOptionResponse> options,
            AnswerSubmitRequest.AnswerItem item
    ) {
        boolean required = Boolean.TRUE.equals(question.required());
        List<Long> optionIds = (item == null || item.optionIds() == null) ? List.of() : item.optionIds();
        String text = blankToNull(item == null ? null : item.text());
        String otherText = blankToNull(item == null ? null : item.otherText());
        Integer rating = item == null ? null : item.ratingValue();

        switch (question.questionType()) {
            case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> {
                if (text != null || rating != null) {
                    throw invalid(question, "선택형 질문에는 optionIds로만 답할 수 있습니다.");
                }
                if (optionIds.isEmpty()) {
                    if (otherText != null) {
                        throw invalid(question, "기타 내용은 기타 선택지를 고른 경우에만 입력할 수 있습니다.");
                    }
                    return skipOrFail(question, required);
                }
                validateChoice(question, options, optionIds, otherText);
            }
            case "SHORT_TEXT", "LONG_TEXT" -> {
                if (!optionIds.isEmpty() || rating != null || otherText != null) {
                    throw invalid(question, "서술형 질문에는 text로만 답할 수 있습니다.");
                }
                if (text == null) {
                    return skipOrFail(question, required);
                }
                int max = "SHORT_TEXT".equals(question.questionType())
                        ? SHORT_TEXT_MAX_LENGTH : LONG_TEXT_MAX_LENGTH;
                if (text.length() > max) {
                    throw invalid(question, "답변은 " + max + "자 이하여야 합니다.");
                }
            }
            case "RATING" -> {
                if (!optionIds.isEmpty() || text != null || otherText != null) {
                    throw invalid(question, "별점 질문에는 ratingValue로만 답할 수 있습니다.");
                }
                if (rating == null) {
                    return skipOrFail(question, required);
                }
                if (rating < question.ratingMin() || rating > question.ratingMax()) {
                    throw invalid(question, "점수는 " + question.ratingMin() + "~" + question.ratingMax() + " 사이여야 합니다.");
                }
            }
            default -> throw new IllegalStateException("알 수 없는 질문 유형: " + question.questionType());
        }

        AnswerInsertCommand command = new AnswerInsertCommand(
                null, eventId, participationId, question.id(), text, rating, otherText
        );
        return new PreparedAnswer(command, optionIds);
    }

    private void validateChoice(
            EventQuestionResponse question,
            List<EventQuestionResponse.QuestionOptionResponse> options,
            List<Long> optionIds,
            String otherText
    ) {
        if ("SINGLE_CHOICE".equals(question.questionType()) && optionIds.size() != 1) {
            throw invalid(question, "단일 선택 질문은 선택지를 1개만 고를 수 있습니다.");
        }
        if (new HashSet<>(optionIds).size() != optionIds.size()) {
            throw invalid(question, "중복된 선택지가 있습니다.");
        }

        Map<Long, EventQuestionResponse.QuestionOptionResponse> optionMap = options.stream()
                .collect(Collectors.toMap(EventQuestionResponse.QuestionOptionResponse::id, Function.identity()));

        boolean otherSelected = false;
        for (Long optionId : optionIds) {
            EventQuestionResponse.QuestionOptionResponse option = optionMap.get(optionId);
            if (option == null) {
                throw invalid(question, "이 질문에 속하지 않는 선택지입니다.");
            }
            if (Boolean.TRUE.equals(option.isOther())) {
                otherSelected = true;
            }
        }

        if (otherSelected && otherText == null) {
            throw invalid(question, "기타를 선택한 경우 내용을 입력해야 합니다.");
        }
        if (!otherSelected && otherText != null) {
            throw invalid(question, "기타 내용은 기타 선택지를 고른 경우에만 입력할 수 있습니다.");
        }
    }

    private PreparedAnswer skipOrFail(EventQuestionResponse question, boolean required) {
        if (required) {
            throw invalid(question, "필수 응답 질문입니다.");
        }
        return null;
    }

    private static BusinessException invalid(EventQuestionResponse question, String reason) {
        return new BusinessException(ErrorCode.INVALID_ANSWER, "질문 " + question.id() + "번: " + reason);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}