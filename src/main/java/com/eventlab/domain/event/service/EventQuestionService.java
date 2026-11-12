package com.eventlab.domain.event.service;

import com.eventlab.domain.event.dto.*;
import com.eventlab.domain.event.mapper.EventMapper;
import com.eventlab.domain.event.mapper.EventQuestionMapper;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventQuestionService {
    private final EventMapper eventMapper;
    private final EventQuestionMapper eventQuestionMapper;

    @Value("${eventlab.dev-host-user-id}")
    private Long devHostUserId;

    @Transactional
    public EventQuestionDetailResponse createQuestion(Long eventId, QuestionCreateRequest request) {
        EventResponse event = eventMapper.findByIdAndHostUserId(eventId, devHostUserId);

        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        if (!"DRAFT".equals(event.status())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }

        validateQuestion(request);

        int sortOrder = eventQuestionMapper.findNextSortOrder(eventId);

        QuestionInsertCommand command = new QuestionInsertCommand(
                null,
                eventId,
                request.questionType(),
                request.title(),
                request.description(),
                request.required(),
                sortOrder,
                request.ratingMin(),
                request.ratingMax()
        );

        eventQuestionMapper.insertQuestion(command);

        Long questionId = command.getId();

        if (request.options() != null) {
            int optionOrder = 1;
            for (QuestionCreateRequest.QuestionOptionRequest option : request.options()) {
                eventQuestionMapper.insertOption(
                        questionId,
                        option.optionText(),
                        optionOrder++,
                        option.isOther()
                );
            }
        }

        return getQuestionResponse(questionId);
    }

    @Transactional
    public List<EventQuestionDetailResponse> getQuestions(Long eventId) {
        EventResponse event = eventMapper.findByIdAndHostUserId(eventId, devHostUserId);

        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        return getQuestionDetails(eventId);
    }

    // 호스트 확인 없이 질문 + 선택지를 조립 (참여자 조회에서도 사용)
    @Transactional(readOnly = true)
    public List<EventQuestionDetailResponse> getQuestionDetails(Long eventId) {
        List<EventQuestionResponse> questions = eventQuestionMapper.findQuestionsByEventId(eventId);

        List<EventQuestionResponse.QuestionOptionResponse> options = eventQuestionMapper.findOptionsByEventId(eventId);

        Map<Long, List<EventQuestionResponse.QuestionOptionResponse>> optionMap = options.stream().collect(
                Collectors.groupingBy(
                        option -> option.questionId()
                )
        );

        return questions.stream()
                .map(question -> new EventQuestionDetailResponse(
                        question.id(),
                        question.eventId(),
                        question.questionType(),
                        question.title(),
                        question.description(),
                        question.required(),
                        question.sortOrder(),
                        question.ratingMin(),
                        question.ratingMax(),
                        question.createdAt(),
                        optionMap.getOrDefault(question.id(), List.of())
                ))
                .toList();
    }

    @Transactional
    public EventQuestionDetailResponse updateQuestion(Long eventId, Long questionId, QuestionUpdateRequest request) {
        validateEventEditable(eventId);

        EventQuestionResponse current = eventQuestionMapper.findQuestionByIdAndEventId(questionId, eventId);
        if (current == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        validateQuestion(request.questionType(), request.ratingMin(), request.ratingMax(), request.options());

        List<EventQuestionResponse.QuestionOptionResponse> currentOptions = eventQuestionMapper.findOptionsByQuestionId(questionId);
        List<QuestionUpdateRequest.QuestionOptionUpdateRequest> requestedOptions = request.options() == null ? List.of() : request.options();

        long answerCount = eventQuestionMapper.countAnswersByQuestionId(questionId);

        if(answerCount > 0) {
            validateAnsweredQuestionUpdate(
                    current,
                    currentOptions,
                    request,
                    requestedOptions
            );
        }

        eventQuestionMapper.updateQuestion(
                questionId,
                eventId,
                request.questionType(),
                request.title(),
                request.description(),
                request.required(),
                request.ratingMin(),
                request.ratingMax()
        );

        updateOptions(questionId, currentOptions, requestedOptions);

        return getQuestionResponse(questionId);
    }

    @Transactional
    public void deleteQuestion(Long eventId, Long questionId) {
        validateEventEditable(eventId);

        EventQuestionResponse question =
                eventQuestionMapper.findQuestionByIdAndEventId(questionId, eventId);

        if (question == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        long answerCount =
                eventQuestionMapper.countAnswersByQuestionId(questionId);

        if (answerCount > 0) {
            throw new BusinessException(ErrorCode.QUESTION_HAS_ANSWERS);
        }

        eventQuestionMapper.deleteOptionsByQuestionId(questionId);

        eventQuestionMapper.deleteQuestion(questionId, eventId);
    }

    @Transactional
    public void reorderQuestions(Long eventId, QuestionReorderRequest request) {
        validateEventEditable(eventId);

        List<Long> currentIds =
                eventQuestionMapper.findQuestionIdsByEventId(eventId);

        List<Long> requestedIds = request.questionIds();

        if (currentIds.size() != requestedIds.size()
                || new java.util.HashSet<>(requestedIds).size() != requestedIds.size()
                || !new java.util.HashSet<>(currentIds)
                .equals(new java.util.HashSet<>(requestedIds))) {
            throw new BusinessException(ErrorCode.INVALID_QUESTION_ORDER);
        }

        for (int i = 0; i < requestedIds.size(); i++) {
            eventQuestionMapper.updateQuestionSortOrder(
                    requestedIds.get(i),
                    eventId,
                    i + 1
            );
        }
    }

    private void validateQuestion(QuestionCreateRequest request) {
        String type = request.questionType();
        List<QuestionCreateRequest.QuestionOptionRequest> options = request.options() == null ? List.of() : request.options();

        if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {

            if (options.size() < 2) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            long otherCount = options.stream()
                    .filter(option -> Boolean.TRUE.equals(option.isOther()))
                    .count();

            if (otherCount > 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            if (request.ratingMin() != null || request.ratingMax() != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            return;
        }

        if ("RATING".equals(type)) {

            if (request.ratingMin() == null || request.ratingMax() == null || request.ratingMax() <= request.ratingMin()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            if (!options.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            return;
        }

        if (!options.isEmpty() || request.ratingMin() != null || request.ratingMax() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateEventEditable(Long eventId) {
        EventResponse event =
                eventMapper.findByIdAndHostUserId(eventId, devHostUserId);

        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        if (!"DRAFT".equals(event.status())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }
    }

    private void validateQuestion(String type, Integer ratingMin, Integer ratingMax, List<?> options) {
        if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
            if (options.size() < 2 || ratingMin != null || ratingMax != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            long otherCount = options.stream()
                    .filter(option -> {
                        if (option instanceof QuestionCreateRequest.QuestionOptionRequest o) {
                            return Boolean.TRUE.equals(o.isOther());
                        }
                        if (option instanceof QuestionUpdateRequest.QuestionOptionUpdateRequest o) {
                            return Boolean.TRUE.equals(o.isOther());
                        }
                        return false;
                    })
                    .count();

            if (otherCount > 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            return;
        }

        if ("RATING".equals(type)) {
            if (ratingMin == null || ratingMax == null || ratingMax <= ratingMin
                    || !options.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return;
        }

        if (!options.isEmpty() || ratingMin != null || ratingMax != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateAnsweredQuestionUpdate(EventQuestionResponse current, List<EventQuestionResponse.QuestionOptionResponse> currentOptions, QuestionUpdateRequest request, List<QuestionUpdateRequest.QuestionOptionUpdateRequest> requestedOptions) {
        boolean typeChanged =
                !current.questionType().equals(request.questionType());

        boolean ratingChanged =
                !java.util.Objects.equals(current.ratingMin(), request.ratingMin())
                        || !java.util.Objects.equals(current.ratingMax(), request.ratingMax());

        if (typeChanged || ratingChanged) {
            throw new BusinessException(ErrorCode.QUESTION_HAS_ANSWERS);
        }

        if (currentOptions.size() != requestedOptions.size()) {
            throw new BusinessException(ErrorCode.QUESTION_HAS_ANSWERS);
        }

        Map<Long, EventQuestionResponse.QuestionOptionResponse> currentMap =
                currentOptions.stream().collect(
                        Collectors.toMap(
                                EventQuestionResponse.QuestionOptionResponse::id,
                                option -> option
                        )
                );

        for (QuestionUpdateRequest.QuestionOptionUpdateRequest option : requestedOptions) {
            if (option.id() == null) {
                throw new BusinessException(ErrorCode.QUESTION_HAS_ANSWERS);
            }

            EventQuestionResponse.QuestionOptionResponse existing =
                    currentMap.get(option.id());

            if (existing == null
                    || !existing.optionText().equals(option.optionText())
                    || !java.util.Objects.equals(existing.isOther(), option.isOther())) {
                throw new BusinessException(ErrorCode.QUESTION_HAS_ANSWERS);
            }
        }
    }

    private void updateOptions(Long questionId, List<EventQuestionResponse.QuestionOptionResponse> currentOptions, List<QuestionUpdateRequest.QuestionOptionUpdateRequest> requestedOptions) {
        Map<Long, EventQuestionResponse.QuestionOptionResponse> currentMap =
                currentOptions.stream().collect(
                        Collectors.toMap(
                                EventQuestionResponse.QuestionOptionResponse::id,
                                option -> option
                        )
                );

        java.util.Set<Long> retainedIds = requestedOptions.stream()
                .map(QuestionUpdateRequest.QuestionOptionUpdateRequest::id)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        for (EventQuestionResponse.QuestionOptionResponse current : currentOptions) {
            if (!retainedIds.contains(current.id())) {
                long usedCount =
                        eventQuestionMapper.countAnswerOptionsByOptionId(current.id());

                if (usedCount > 0) {
                    throw new BusinessException(ErrorCode.QUESTION_OPTION_IN_USE);
                }

                eventQuestionMapper.deleteOption(current.id(), questionId);
            }
        }

        int sortOrder = 1;

        for (QuestionUpdateRequest.QuestionOptionUpdateRequest option : requestedOptions) {
            if (option.id() == null) {
                eventQuestionMapper.insertOption(
                        questionId,
                        option.optionText(),
                        sortOrder++,
                        option.isOther()
                );
                continue;
            }

            EventQuestionResponse.QuestionOptionResponse existing =
                    currentMap.get(option.id());

            if (existing == null) {
                throw new BusinessException(ErrorCode.QUESTION_OPTION_NOT_FOUND);
            }

            boolean changed =
                    !existing.optionText().equals(option.optionText())
                            || !java.util.Objects.equals(existing.isOther(), option.isOther());

            if (changed) {
                long usedCount =
                        eventQuestionMapper.countAnswerOptionsByOptionId(option.id());

                if (usedCount > 0) {
                    throw new BusinessException(ErrorCode.QUESTION_OPTION_IN_USE);
                }

                eventQuestionMapper.updateOption(
                        option.id(),
                        questionId,
                        option.optionText(),
                        option.isOther()
                );
            }

            sortOrder++;
        }
    }

    private EventQuestionDetailResponse getQuestionResponse(Long questionId) {

        EventQuestionResponse question =
                eventQuestionMapper.findQuestionById(questionId);

        List<EventQuestionResponse.QuestionOptionResponse> options =
                eventQuestionMapper.findOptionsByQuestionId(questionId);

        return new EventQuestionDetailResponse(
                question.id(),
                question.eventId(),
                question.questionType(),
                question.title(),
                question.description(),
                question.required(),
                question.sortOrder(),
                question.ratingMin(),
                question.ratingMax(),
                question.createdAt(),
                options
        );
    }
}
