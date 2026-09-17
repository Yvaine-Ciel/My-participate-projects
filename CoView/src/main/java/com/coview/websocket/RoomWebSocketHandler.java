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
        String action = text(root, "action").orElse("");
        double position = root.path("positionSeconds").asDouble(0.0);
        PlaybackState state = roomService.applyPlayback(roomId, participantId, action, position);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "playback");
        payload.put("senderId", participantId);
        payload.put("action", action);
        payload.put("state", state);
        broadcast(roomId, payload, null);
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
                // 断开的连接会在关闭回调或下一次广播时清理。
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
}
