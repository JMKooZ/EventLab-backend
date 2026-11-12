package com.eventlab.domain.participant.service;

import com.eventlab.domain.event.dto.EventQuestionDetailResponse;
import com.eventlab.domain.event.dto.EventResponse;
import com.eventlab.domain.event.mapper.EventMapper;
import com.eventlab.domain.event.service.EventQuestionService;
import com.eventlab.domain.participant.dto.*;
import com.eventlab.domain.participant.mapper.ParticipantMapper;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private static final int NICKNAME_KEY_MAX_LENGTH = 100; // participants.nickname_key 길이
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final EventQuestionService eventQuestionService;

    @Transactional
    public ParticipantJoinResponse join(Long eventId, ParticipantCreateRequest request, String reconnectToken) {
        // 1) 이벤트 행 잠금 + 조회. 반드시 이 트랜잭션의 첫 SQL이어야 한다.
        //    (REPEATABLE READ 스냅샷이 잠금 획득 이후에 만들어져야 아래 COUNT가 최신 커밋을 본다)
        EventResponse event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (!"RUNNING".equals(event.status())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_OPEN);
        }

        // 2) 재접속 토큰이 유효하면 기존 게스트. 모르는 토큰이면 토큰이 없는 것으로 취급한다.
        Long nonUserId = null;
        if (reconnectToken != null && !reconnectToken.isBlank()) {
            nonUserId = participantMapper.findNonUserIdByTokenHash(sha256Hex(reconnectToken));
        }

        if (nonUserId != null) {
            participantMapper.touchNonUser(nonUserId);

            // 이미 이 이벤트에 참여 중이면 기존 참여 정보를 그대로 반환 (정원 체크보다 먼저)
            ParticipantResponse existing = participantMapper.findByEventIdAndNonUserId(eventId, nonUserId);
            if (existing != null) {
                log.info("게스트 재접속 eventId={}, participantId={}", eventId, existing.participantId());
                return ParticipantJoinResponse.of(existing, null, true);
            }
        }

        // 3) 정원 확인
        if (eventMapper.countParticipantsByEventId(eventId) >= event.maxParticipants()) {
            throw new BusinessException(ErrorCode.EVENT_FULL);
        }

        // 4) 닉네임 정규화
        String nickname = request.nickname().trim().replaceAll("\\s+", " ");
        String nicknameKey = toNicknameKey(nickname);
        if (nicknameKey.length() > NICKNAME_KEY_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 5) non_users: 신규 게스트면 토큰 발급 (원문은 응답으로만, DB에는 해시만 저장)
        String issuedToken = null;
        if (nonUserId == null) {
            issuedToken = generateToken();
            NonUserInsertCommand nonUser = new NonUserInsertCommand(null, sha256Hex(issuedToken));
            participantMapper.insertNonUser(nonUser);
            nonUserId = nonUser.getId();
        }

        // 6) participants → participations
        ParticipantInsertCommand participant =
                new ParticipantInsertCommand(null, eventId, nonUserId, nickname, nicknameKey);
        try {
            participantMapper.insertParticipant(participant);
        } catch (DuplicateKeyException e) {
            // uk_participants_nickname (event_id, nickname_key)
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        participantMapper.insertParticipation(eventId, participant.getId());

        ParticipantResponse created =
                participantMapper.findByEventIdAndParticipantId(eventId, participant.getId());

        log.info("게스트 참여 시작 eventId={}, participantId={}, newGuest={}",
                eventId, participant.getId(), issuedToken != null);

        return ParticipantJoinResponse.of(created, issuedToken, false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ParticipationInfo getParticipationForUpdate(Long eventId, String reconnectToken) {
        Long nonUserId = resolveNonUserId(reconnectToken);

        ParticipationInfo participation = participantMapper.findParticipationForUpdate(eventId, nonUserId);
        if (participation == null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }
        return participation;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markSubmitted(Long participationId) {
        if (participantMapper.markParticipationSubmitted(participationId) == 0) {
            throw new BusinessException(ErrorCode.ALREADY_SUBMITTED);
        }
    }

    // 내 참여 정보 조회 (새로고침 시 상태 복원용). 조회만 하므로 readOnly
    @Transactional(readOnly = true)
    public ParticipantResponse getMe(Long eventId, String reconnectToken) {
        Long nonUserId = resolveNonUserId(reconnectToken);

        ParticipantResponse me = participantMapper.findByEventIdAndNonUserId(eventId, nonUserId);
        if (me == null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }
        return me;
    }

    // 게스트용 질문 조회: 이 이벤트의 참여자만 볼 수 있다
    @Transactional(readOnly = true)
    public List<EventQuestionDetailResponse> getQuestionsForParticipant(Long eventId, String reconnectToken) {
        getMe(eventId, reconnectToken);
        return eventQuestionService.getQuestionDetails(eventId);
    }

    private Long resolveNonUserId(String reconnectToken) {
        if (reconnectToken == null || reconnectToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_RECONNECT_TOKEN);
        }

        Long nonUserId = participantMapper.findNonUserIdByTokenHash(sha256Hex(reconnectToken));
        if (nonUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_RECONNECT_TOKEN);
        }
        return nonUserId;
    }

    private static String toNicknameKey(String nickname) {
        return Normalizer.normalize(nickname, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}