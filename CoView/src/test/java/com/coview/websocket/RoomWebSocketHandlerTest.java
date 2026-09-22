package com.coview.websocket;

import com.coview.dto.JoinRequestView;
import com.coview.dto.RoomSessionResponse;
import com.coview.service.RoomService;
import com.coview.service.VideoModeDetector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomWebSocketHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RoomService roomService = new RoomService(new VideoModeDetector());
    private final RoomWebSocketHandler handler = new RoomWebSocketHandler(roomService, objectMapper);

    @Test
    void guestPlaybackRequestIsForwardedToOwner() throws Exception {
        RoomSessionResponse owner = createRoom();
        RoomSessionResponse viewer = approveViewer(owner, "Bob");
        WebSocketSession ownerSocket = connectedSocket(owner.roomId(), owner.participantId());
        WebSocketSession viewerSocket = connectedSocket(owner.roomId(), viewer.participantId());
        clearInvocations(ownerSocket, viewerSocket);

        handler.handleMessage(viewerSocket, new TextMessage("""
                {"type":"playback-request","action":"pause","positionSeconds":42.5}
                """));

        JsonNode payload = lastPayload(ownerSocket);
        assertThat(payload.path("type").asText()).isEqualTo("playback-request");
        assertThat(payload.path("requesterId").asText()).isEqualTo(viewer.participantId());
        assertThat(payload.path("requesterName").asText()).isEqualTo("Bob");
        assertThat(payload.path("action").asText()).isEqualTo("pause");
        assertThat(payload.path("positionSeconds").asDouble()).isEqualTo(42.5);
        verify(viewerSocket, never()).sendMessage(any());
    }

    @Test
    void ownerCanBroadcastStatePlaybackUpdate() throws Exception {
        RoomSessionResponse owner = createRoom();
        WebSocketSession ownerSocket = connectedSocket(owner.roomId(), owner.participantId());
        clearInvocations(ownerSocket);

        handler.handleMessage(ownerSocket, new TextMessage("""
                {"type":"playback","action":"state","positionSeconds":12.25}
                """));

        JsonNode payload = lastPayload(ownerSocket);
        assertThat(payload.path("type").asText()).isEqualTo("playback");
        assertThat(payload.path("senderId").asText()).isEqualTo(owner.participantId());
        assertThat(payload.path("action").asText()).isEqualTo("state");
        assertThat(payload.path("state").path("positionSeconds").asDouble()).isEqualTo(12.25);
        assertThat(roomService.getRoomView(owner.roomId()).playback().positionSeconds()).isEqualTo(12.25);
    }

    @Test
    void danmakuRequestsAreForwardedAndDecisionIsSentBackToViewer() throws Exception {
        RoomSessionResponse owner = createRoom();
        RoomSessionResponse viewer = approveViewer(owner, "Bob");
        WebSocketSession ownerSocket = connectedSocket(owner.roomId(), owner.participantId());
        WebSocketSession viewerSocket = connectedSocket(owner.roomId(), viewer.participantId());

        for (String action : List.of("danmaku-on", "danmaku-off")) {
            clearInvocations(ownerSocket, viewerSocket);

            handler.handleMessage(viewerSocket, new TextMessage("""
                    {"type":"playback-request","action":"%s","positionSeconds":0}
                    """.formatted(action)));

            JsonNode request = lastPayload(ownerSocket);
            assertThat(request.path("type").asText()).isEqualTo("playback-request");
            assertThat(request.path("action").asText()).isEqualTo(action);
            var playbackBeforeDecision = roomService.getRoomView(owner.roomId()).playback();
            clearInvocations(ownerSocket, viewerSocket);

            handler.handleMessage(ownerSocket, new TextMessage("""
                    {"type":"playback-request-decision","requestId":"%s","requesterId":"%s","action":"%s","approved":true}
                    """.formatted(request.path("id").asText(), viewer.participantId(), action)));

            JsonNode ownerDecision = lastPayload(ownerSocket);
            JsonNode viewerDecision = lastPayload(viewerSocket);
            assertThat(ownerDecision.path("type").asText()).isEqualTo("playback-request-decision");
            assertThat(viewerDecision.path("type").asText()).isEqualTo("playback-request-decision");
            assertThat(viewerDecision.path("action").asText()).isEqualTo(action);
            assertThat(viewerDecision.path("approved").asBoolean()).isTrue();
            assertThat(roomService.getRoomView(owner.roomId()).playback()).isEqualTo(playbackBeforeDecision);
        }
    }

    private RoomSessionResponse createRoom() {
        String roomId = roomService.generateCandidateRoomId();
        return roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");
    }

    private RoomSessionResponse approveViewer(RoomSessionResponse owner, String displayName) {
        JoinRequestView pending = roomService.createJoinRequest(owner.roomId(), displayName, "1234");
        JoinRequestView approved = roomService.decideJoinRequest(owner.roomId(), owner.participantId(), pending.id(), true);
        return new RoomSessionResponse(owner.roomId(), approved.participantId(), approved.room());
    }

    private WebSocketSession connectedSocket(String roomId, String participantId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/rooms/" + roomId + "?participantId=" + participantId));
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        return session;
    }

    private JsonNode lastPayload(WebSocketSession session) throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return objectMapper.readTree(captor.getValue().getPayload());
    }
}
