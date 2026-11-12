package com.eventlab.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),

    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "페이지 번호 또는 페이지 크기가 올바르지 않습니다."),

    EVENT_NOT_EDITABLE(HttpStatus.CONFLICT, "현재 이벤트 상태에서는 수정할 수 없습니다."),
    EVENT_MAX_PARTICIPANTS_TOO_LOW(HttpStatus.CONFLICT, "최대 참여자 수는 현재 참여자 수보다 적을 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    QUESTION_HAS_ANSWERS(HttpStatus.CONFLICT, "응답이 존재하는 질문은 해당 항목을 변경하거나 삭제할 수 없습니다."),
    QUESTION_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택지를 찾을 수 없습니다."),
    QUESTION_OPTION_IN_USE(HttpStatus.CONFLICT, "응답에 사용된 선택지는 삭제하거나 내용을 변경할 수 없습니다."),
    INVALID_QUESTION_ORDER(HttpStatus.BAD_REQUEST, "질문 순서 정보가 올바르지 않습니다."),

    EVENT_NOT_OPEN(HttpStatus.CONFLICT, "참여할 수 없는 이벤트 상태입니다."),
    EVENT_FULL(HttpStatus.CONFLICT, "최대 참여자 수에 도달했습니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    EVENT_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 이벤트 상태에서는 해당 상태로 변경할 수 없습니다."),
    EVENT_NO_QUESTIONS(HttpStatus.CONFLICT, "질문이 하나 이상 있어야 이벤트를 시작할 수 있습니다."),

    INVALID_RECONNECT_TOKEN(HttpStatus.UNAUTHORIZED, "참여자 인증 정보가 올바르지 않습니다."),
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "이 이벤트의 참여 정보를 찾을 수 없습니다."),
    ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 답변을 제출했습니다."),
    INVALID_ANSWER(HttpStatus.BAD_REQUEST, "답변 내용이 올바르지 않습니다."),
    RESULT_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "답변을 제출한 뒤에 결과를 볼 수 있습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
