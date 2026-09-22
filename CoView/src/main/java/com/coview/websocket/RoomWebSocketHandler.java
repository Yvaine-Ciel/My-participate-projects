package com.coview.websocket;

import com.coview.dto.RoomView;
import com.coview.model.Participant;
import com.coview.model.PlaybackState;
import com.coview.model.Room;
import com.coview.service.RoomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private static final String ROOM_ID_ATTR = "roomId";
    private static final String PARTICIPANT_ID_ATTR = "participantId";

    private final RoomService roomService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ConcurrentMap<String, Set<WebSocketSession>>> sessionsByRoom = new ConcurrentHashMap<>();

    public RoomWebSocketHandler(RoomService roomService, ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String requestedRoomId = extractRoomId(uri);
        String participantId = extractParticipantId(uri);
        Optional<Room> room = roomService.findRoom(requestedRoomId);

        if (requestedRoomId == null
                || participantId == null
                || room.isEmpty()
                || room.get().findParticipant(participantId).isEmpty()) {
            session.close(new CloseStatus(1008, "房间或成员身份无效。"));
            return;
        }

        String roomId = room.get().getId();
        session.getAttributes().put(ROOM_ID_ATTR, roomId);
        session.getAttributes().put(PARTICIPANT_ID_ATTR, participantId);
        sessionsByRoom
                .computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(participantId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);

        roomService.touchParticipant(roomId, participantId);
        send(session, snapshotMessage(roomService.getRoomView(roomId)));
        broadcast(roomId, roomMessage("presence", roomService.getRoomView(roomId)), null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = attr(session, ROOM_ID_ATTR);
        String participantId = attr(session, PARTICIPANT_ID_ATTR);
        if (roomId == null || participantId == null) {
            sendError(session, "当前连接未绑定到房间。");
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = text(root, "type").orElse("");
            switch (type) {
                case "playback" -> handlePlayback(roomId, participantId, root);
                case "playback-request" -> handlePlaybackRequest(roomId, participantId, root);
                case "playback-request-decision" -> handlePlaybackRequestDecision(roomId, participantId, root);
                case "webrtc-signal" -> handleWebRtcSignal(roomId, participantId, root);
                case "screen-share" -> handleScreenShare(roomId, participantId, root);
                case "chat" -> handleChat(roomId, participantId, root);
                case "leave" -> handleLeave(roomId, participantId);
                case "room-refresh" -> broadcast(roomId, snapshotMessage(roomService.getRoomView(roomId)), null);
                case "ping" -> send(session, simpleMessage("pong"));
                default -> sendError(session, "未知的消息类型。");
            }
        } catch (ResponseStatusException ex) {
            sendError(session, Optional.ofNullable(ex.getReason()).orElse("请求已被拒绝。"));
        } catch (Exception ex) {
            sendError(session, "消息处理失败。");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = attr(session, ROOM_ID_ATTR);
        String participantId = attr(session, PARTICIPANT_ID_ATTR);
        if (roomId == null || participantId == null) {
            return;
        }

        sessionsByRoom.computeIfPresent(roomId, (ignored, participants) -> {
            participants.computeIfPresent(participantId, (ignoredParticipant, sockets) -> {
                sockets.remove(session);
                return sockets.isEmpty() ? null : sockets;
            });
            return participants.isEmpty() ? null : participants;
        });

        roomService.touchParticipant(roomId, participantId);
        roomService.findRoom(roomId)
                .ifPresent(room -> broadcast(roomId, roomMessage("presence", roomService.toView(room)), null));
    }

    private void handlePlayback(String roomId, String participantId, JsonNode root) {
        String action = normalizePlaybackAction(text(root, "action").orElse(""), true);
        double position = Math.max(0.0, root.path("positionSeconds").asDouble(0.0));
        PlaybackState state = roomService.applyPlayback(roomId, participantId, action, position);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "playback");
        payload.put("senderId", participantId);
        payload.put("action", action);
        payload.put("state", state);
        broadcast(roomId, payload, null);
    }

    private void handlePlaybackRequest(String roomId, String participantId, JsonNode root) {
        Room room = roomService.requireRoom(roomId);
        Participant participant = roomService.requireParticipant(roomId, participantId);
        String action = normalizePlaybackAction(text(root, "action").orElse(""), false, true);
        double position = Math.max(0.0, root.path("positionSeconds").asDouble(0.0));

        if (participant.isOwner()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房主可以直接控制播放。");
        }
        participant.touch();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "playback-request");
        payload.put("id", UUID.randomUUID().toString());
        payload.put("requesterId", participant.getId());
        payload.put("requesterName", participant.getDisplayName());
        payload.put("action", action);
        payload.put("positionSeconds", position);
        payload.put("requestedAt", Instant.now());
        sendToParticipant(roomId, room.getOwnerId(), payload);
    }

    private void handlePlaybackRequestDecision(String roomId, String participantId, JsonNode root) {
        Room room = roomService.requireRoom(roomId);
        if (!room.getOwnerId().equals(participantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有房主可以处理播放请求。");
        }

        String requestId = text(root, "requestId").orElse("");
        String requesterId = text(root, "requesterId").orElse("");
        boolean approved = root.path("approved").asBoolean(false);
        String action = normalizePlaybackAction(text(root, "action").orElse(""), false, true);
        double position = Math.max(0.0, root.path("positionSeconds").asDouble(0.0));
        if (requestId.isBlank() || requesterId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少播放请求信息。");
        }
        room.findParticipant(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求的房客已不在房间中。"));

        if (approved && isPlaybackStateAction(action)) {
            PlaybackState state = roomService.applyPlayback(roomId, participantId, action, position);
            Map<String, Object> playbackPayload = new LinkedHashMap<>();
            playbackPayload.put("type", "playback");
            playbackPayload.put("senderId", participantId);
            playbackPayload.put("action", action);
            playbackPayload.put("state", state);
            broadcast(roomId, playbackPayload, null);
        }

        Map<String, Object> decisionPayload = new LinkedHashMap<>();
        decisionPayload.put("type", "playback-request-decision");
        decisionPayload.put("requestId", requestId);
        decisionPayload.put("requesterId", requesterId);
        decisionPayload.put("approved", approved);
        decisionPayload.put("action", action);
        decisionPayload.put("positionSeconds", position);
        decisionPayload.put("decidedAt", Instant.now());
        sendToParticipant(roomId, room.getOwnerId(), decisionPayload);
        if (!room.getOwnerId().equals(requesterId)) {
            sendToParticipant(roomId, requesterId, decisionPayload);
        }
    }

    private void handleWebRtcSignal(String roomId, String participantId, JsonNode root) {
        String targetId = text(root, "targetId").orElse("");
        JsonNode payloadNode = root.path("payload");
        if (targetId.isBlank() || payloadNode.isMissingNode() || payloadNode.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 WebRTC 信令目标或内容。");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "webrtc-signal");
        payload.put("fromId", participantId);
        payload.put("targetId", targetId);
        payload.put("payload", payloadNode);
        sendToParticipant(roomId, targetId, payload);
    }

    private void handleScreenShare(String roomId, String participantId, JsonNode root) {
        boolean active = root.path("active").asBoolean(false);
        RoomView view = roomService.setScreenShareActive(roomId, participantId, active);
        broadcast(roomId, roomMessage("screen-share", view), null);
    }

    private void handleChat(String roomId, String participantId, JsonNode root) {
        Participant participant = roomService.requireParticipant(roomId, participantId);
        String text = text(root, "text").orElse("").trim();
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能发送空消息。");
        }
        if (text.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单条消息不能超过 500 个字符。");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat");
        payload.put("id", UUID.randomUUID().toString());
        payload.put("senderId", participant.getId());
        payload.put("senderName", participant.getDisplayName());
        payload.put("owner", participant.isOwner());
        payload.put("text", text);
        payload.put("sentAt", Instant.now());
        broadcast(roomId, payload, null);
    }

    private void handleLeave(String roomId, String participantId) {
        boolean roomClosed = roomService.leaveRoom(roomId, participantId);
        if (roomClosed) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "room-closed");
            payload.put("message", "房主已退出，房间已关闭。");
            broadcast(roomId, payload, null);
            sessionsByRoom.remove(roomId);
            return;
        }

        sessionsByRoom.computeIfPresent(roomId, (ignored, participants) -> {
            participants.remove(participantId);
            return participants.isEmpty() ? null : participants;
        });
        roomService.findRoom(roomId)
                .ifPresent(room -> broadcast(roomId, roomMessage("presence", roomService.toView(room)), null));
    }

    private void broadcast(String roomId, Map<String, Object> payload, String exceptParticipantId) {
        ConcurrentMap<String, Set<WebSocketSession>> participants = sessionsByRoom.get(roomId);
        if (participants == null) {
            return;
        }

        participants.forEach((participantId, sockets) -> {
            if (participantId.equals(exceptParticipantId)) {
                return;
            }
            sockets.removeIf(session -> !session.isOpen());
            sockets.forEach(session -> send(session, payload));
        });
    }

    private void sendToParticipant(String roomId, String participantId, Map<String, Object> payload) {
        ConcurrentMap<String, Set<WebSocketSession>> participants = sessionsByRoom.get(roomId);
        if (participants == null) {
            return;
        }
        Set<WebSocketSession> sockets = participants.get(participantId);
        if (sockets == null) {
            return;
        }
        sockets.removeIf(session -> !session.isOpen());
        sockets.forEach(session -> send(session, payload));
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException ignored) {
                // Closed sockets are cleaned up on close callbacks or later broadcasts.
            }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("message", message);
        send(session, payload);
    }

    private Map<String, Object> snapshotMessage(RoomView view) {
        return roomMessage("snapshot", view);
    }

    private Map<String, Object> roomMessage(String type, RoomView view) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("room", view);
        return payload;
    }

    private Map<String, Object> simpleMessage(String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        return payload;
    }

    private String extractRoomId(URI uri) {
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        String prefix = "/ws/rooms/";
        if (path == null || !path.startsWith(prefix) || path.length() <= prefix.length()) {
            return null;
        }
        return path.substring(prefix.length()).trim();
    }

    private String extractParticipantId(URI uri) {
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("participantId");
    }

    private String attr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value == null ? null : value.toString();
    }

    private Optional<String> text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return Optional.empty();
        }
        String text = value.asText();
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private String normalizePlaybackAction(String rawAction) {
        return normalizePlaybackAction(rawAction, false, false);
    }

    private String normalizePlaybackAction(String rawAction, boolean allowState) {
        return normalizePlaybackAction(rawAction, allowState, false);
    }

    private String normalizePlaybackAction(String rawAction, boolean allowState, boolean allowRequestOnly) {
        String action = Optional.ofNullable(rawAction).orElse("").toLowerCase(Locale.ROOT);
        if ("play".equals(action) || "pause".equals(action) || "seek".equals(action)
                || (allowState && "state".equals(action))
                || (allowRequestOnly && isRequestOnlyAction(action))) {
            return action;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知的播放操作。");
    }

    private boolean isPlaybackStateAction(String action) {
        return "play".equals(action) || "pause".equals(action) || "seek".equals(action);
    }

    private boolean isRequestOnlyAction(String action) {
        return "danmaku".equals(action) || "danmaku-on".equals(action) || "danmaku-off".equals(action);
    }
}
