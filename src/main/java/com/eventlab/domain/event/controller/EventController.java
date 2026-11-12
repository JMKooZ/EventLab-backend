package com.eventlab.domain.event.controller;

import com.eventlab.domain.event.dto.EventCreateRequest;
import com.eventlab.domain.event.dto.EventResponse;
import com.eventlab.domain.event.dto.EventStatusUpdateRequest;
import com.eventlab.domain.event.dto.EventUpdateRequest;
import com.eventlab.domain.event.service.EventService;
import com.eventlab.global.pagination.PageRequest;
import com.eventlab.global.pagination.PageResponse;
import com.eventlab.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "이벤트가 생성되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        EventResponse response = eventService.getEvent(id);
        return ResponseEntity.ok(ApiResponse.success(response, "이벤트 조회에 성공했습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> getEventList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        PageRequest request = PageRequest.of(page, size);
        PageResponse<EventResponse> response = eventService.getEventList(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "이벤트 목록 조회에 성공했습니다.")
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request) {
        EventResponse response = eventService.updateEvent(id, request);

        return ApiResponse.success(response, "이벤트 수정에 성공했습니다.");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<EventResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody EventStatusUpdateRequest request) {
        EventResponse response = eventService.changeStatus(id, request.status());

        return ApiResponse.success(response, "이벤트 상태가 변경되었습니다.");
    }

    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventByInviteCode(@PathVariable String inviteCode) {
        EventResponse response = eventService.getEventByInviteCode(inviteCode);
        return ResponseEntity.ok(ApiResponse.success(response, "이벤트 조회에 성공했습니다."));
    }
}
