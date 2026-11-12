package com.eventlab.domain.event.service;

import com.eventlab.domain.event.dto.EventCreateRequest;
import com.eventlab.domain.event.dto.EventResponse;
import com.eventlab.domain.event.dto.EventUpdateRequest;
import com.eventlab.domain.event.mapper.EventMapper;
import com.eventlab.domain.event.mapper.EventQuestionMapper;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import com.eventlab.global.pagination.PageRequest;
import com.eventlab.global.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final EventQuestionMapper eventQuestionMapper;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int INVITE_CODE_LENGTH = 12;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${eventlab.dev-host-user-id}")
    private Long devHostUserId;

    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        String inviteCode = generateUniqueInviteCode();
        eventMapper.insertEvent(
                devHostUserId,
                request.title(),
                request.description(),
                inviteCode,
                request.maxParticipants()
        );
        return findCreatedEvent(inviteCode);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        EventResponse event = eventMapper.findById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    private String generateUniqueInviteCode() {

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {

            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);

            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                int index = secureRandom.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }

            String inviteCode = code.toString();

            if (eventMapper.countByInviteCode(inviteCode) == 0) {
                return inviteCode;
            }
        }

        throw new IllegalStateException("초대 코드 생성에 실패했습니다.");
    }

    private EventResponse findCreatedEvent(String inviteCode) {
        return eventMapper.findByInviteCode(inviteCode);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getEventList(PageRequest request) {
        List<EventResponse> events = eventMapper.findByHostUserId(devHostUserId, request.getOffset(), request.size());

        long totalElements = eventMapper.countByHostUserId(devHostUserId);

        return PageResponse.of(events, totalElements, request);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {
        EventResponse event = eventMapper.findByIdAndHostUserId(id, devHostUserId);
        // 현재 이벤트 참여 인원 조회
        long currentParticipants = eventMapper.countParticipantsByEventId(id);

        if (request.maxParticipants() < currentParticipants) {
            throw new BusinessException(ErrorCode.EVENT_MAX_PARTICIPANTS_TOO_LOW);
        }

        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (!event.status().equals("DRAFT")) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }
        int result = eventMapper.updateEvent(id, devHostUserId, request.title(), request.description(), request.maxParticipants());

        if (result == 0) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }
        return eventMapper.findByIdAndHostUserId(id, devHostUserId);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventByInviteCode(String inviteCode) {
        // 초대 코드는 대문자+숫자로 생성되므로 입력은 대문자로 정규화
        EventResponse event = eventMapper.findByInviteCode(inviteCode.trim().toUpperCase(Locale.ROOT));
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        } else {
            log.info("Event title: {}", event.title());
        }
        return event;
    }

    @Transactional
    public EventResponse changeStatus(Long id, String targetStatus) {
        EventResponse event = eventMapper.findByIdAndHostUserId(id, devHostUserId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        String currentStatus = event.status();
        if (!ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_STATUS_TRANSITION);
        }

        // 시작(DRAFT → RUNNING)하려면 질문이 최소 1개 필요
        if ("DRAFT".equals(currentStatus) && eventQuestionMapper.findQuestionIdsByEventId(id).isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_NO_QUESTIONS);
        }

        int updated = eventMapper.updateStatus(id, devHostUserId, currentStatus, targetStatus);
        if (updated == 0) {
            // 조회 이후 다른 요청이 먼저 상태를 바꾼 경우
            throw new BusinessException(ErrorCode.EVENT_INVALID_STATUS_TRANSITION);
        }

        log.info("이벤트 상태 변경 eventId={}, {} -> {}", id, currentStatus, targetStatus);
        return eventMapper.findByIdAndHostUserId(id, devHostUserId);
    }

    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            "DRAFT", Set.of("RUNNING"),
            "RUNNING", Set.of("PAUSED", "ENDED"),
            "PAUSED", Set.of("RUNNING", "ENDED"),
            "ENDED", Set.of()
    );
}
