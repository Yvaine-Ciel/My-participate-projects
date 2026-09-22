// 房间、加入申请和视频来源 REST 接口。
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

    // 提供服务健康检查。
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // 生成尚未生效的候选房间号。
    @GetMapping("/rooms/candidate-id")
    public Map<String, String> candidateRoomId() {
        return Map.of("roomId", roomService.generateCandidateRoomId());
    }

    // 创建房间并返回房主会话信息。
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomSessionResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.roomId(), request.displayName(), request.sourceUrl(), request.password(), request.confirmPassword());
    }

    // 读取房间当前快照。
    @GetMapping("/rooms/{roomId}")
    public RoomView getRoom(@PathVariable String roomId) {
        return roomService.getRoomView(roomId);
    }

    // 提交房客加入申请。
    @PostMapping("/rooms/{roomId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequestView createJoinRequest(@PathVariable String roomId, @Valid @RequestBody JoinRoomRequest request) {
        return roomService.createJoinRequest(roomId, request.displayName(), request.password());
    }

    // 房主读取待处理加入申请。
    @GetMapping("/rooms/{roomId}/join-requests")
    public List<JoinRequestView> listJoinRequests(@PathVariable String roomId, @RequestParam String participantId) {
        return roomService.listPendingJoinRequests(roomId, participantId);
    }

    // 房客查询自己的加入申请状态。
    @GetMapping("/rooms/{roomId}/join-requests/{requestId}")
    public JoinRequestView getJoinRequest(@PathVariable String roomId, @PathVariable String requestId) {
        return roomService.getJoinRequest(roomId, requestId);
    }

    // 房主同意或拒绝加入申请。
    @PostMapping("/rooms/{roomId}/join-requests/{requestId}/decision")
    public JoinRequestView decideJoinRequest(
            @PathVariable String roomId,
            @PathVariable String requestId,
            @Valid @RequestBody JoinRequestDecisionRequest request) {
        return roomService.decideJoinRequest(roomId, request.participantId(), requestId, request.approved());
    }

    // 房主更换视频来源并重新判定播放模式。
    @PutMapping("/rooms/{roomId}/source")
    public RoomView updateSource(@PathVariable String roomId, @Valid @RequestBody UpdateSourceRequest request) {
        return roomService.updateSource(roomId, request.participantId(), request.sourceUrl());
    }
}
