package com.coview.controller;

import com.coview.dto.CreateRoomRequest;
import com.coview.dto.JoinRequestDecisionRequest;
import com.coview.dto.JoinRequestView;
import com.coview.dto.JoinRoomRequest;
import com.coview.dto.RoomSessionResponse;
import com.coview.dto.RoomView;
import com.coview.dto.UpdateSourceRequest;
import com.coview.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/rooms/candidate-id")
    public Map<String, String> candidateRoomId() {
        return Map.of("roomId", roomService.generateCandidateRoomId());
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomSessionResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.roomId(), request.displayName(), request.sourceUrl(), request.password(), request.confirmPassword());
    }

    @GetMapping("/rooms/{roomId}")
    public RoomView getRoom(@PathVariable String roomId) {
        return roomService.getRoomView(roomId);
    }

    @PostMapping("/rooms/{roomId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequestView createJoinRequest(@PathVariable String roomId, @Valid @RequestBody JoinRoomRequest request) {
        return roomService.createJoinRequest(roomId, request.displayName(), request.password());
    }

    @GetMapping("/rooms/{roomId}/join-requests")
    public List<JoinRequestView> listJoinRequests(@PathVariable String roomId, @RequestParam String participantId) {
        return roomService.listPendingJoinRequests(roomId, participantId);
    }

    @GetMapping("/rooms/{roomId}/join-requests/{requestId}")
    public JoinRequestView getJoinRequest(@PathVariable String roomId, @PathVariable String requestId) {
        return roomService.getJoinRequest(roomId, requestId);
    }

    @PostMapping("/rooms/{roomId}/join-requests/{requestId}/decision")
    public JoinRequestView decideJoinRequest(
            @PathVariable String roomId,
            @PathVariable String requestId,
            @Valid @RequestBody JoinRequestDecisionRequest request) {
        return roomService.decideJoinRequest(roomId, request.participantId(), requestId, request.approved());
    }

    @PutMapping("/rooms/{roomId}/source")
    public RoomView updateSource(@PathVariable String roomId, @Valid @RequestBody UpdateSourceRequest request) {
        return roomService.updateSource(roomId, request.participantId(), request.sourceUrl());
    }
}
