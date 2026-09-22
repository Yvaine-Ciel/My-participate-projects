package com.coview.service;

import com.coview.dto.RoomSessionResponse;
import com.coview.dto.JoinRequestView;
import com.coview.model.PlaybackMode;
import com.coview.model.PlaybackState;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceTest {

    private final RoomService roomService = new RoomService(new VideoModeDetector());

    @Test
    void createsTemporaryRoomWithDisplayedLjxRoomId() {
        String roomId = roomService.generateCandidateRoomId();

        RoomSessionResponse response = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");

        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.roomId()).startsWith("LJX-").hasSize(10);
        assertThat(response.participantId()).isNotBlank();
        assertThat(response.room().ownerId()).isEqualTo(response.participantId());
        assertThat(response.room().participants()).hasSize(1);
        assertThat(response.room().source().mode()).isEqualTo(PlaybackMode.SYNC);
    }

    @Test
    void displayedRoomIdOnlyBecomesEffectiveAfterCreate() {
        String roomId = roomService.generateCandidateRoomId();

        assertThatThrownBy(() -> roomService.getRoomView(roomId))
                .isInstanceOf(ResponseStatusException.class);

        roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");

        assertThat(roomService.getRoomView(roomId).id()).isEqualTo(roomId);
    }

    @Test
    void viewerMustUseCorrectPasswordAndOwnerApproval() {
        String roomId = roomService.generateCandidateRoomId();
        RoomSessionResponse owner = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");

        assertThatThrownBy(() -> roomService.createJoinRequest(owner.roomId(), "Bob", "wrong"))
                .isInstanceOf(ResponseStatusException.class);

        JoinRequestView pending = roomService.createJoinRequest(owner.roomId(), "Bob", "1234");
        assertThat(roomService.getRoomView(owner.roomId()).participants()).hasSize(1);

        JoinRequestView approved = roomService.decideJoinRequest(owner.roomId(), owner.participantId(), pending.id(), true);

        assertThat(approved.room().participants()).hasSize(2);
        assertThat(approved.participantId()).isNotEqualTo(owner.participantId());
    }

    @Test
    void joinRequestNeedsOwnerApprovalBeforeViewerEnters() {
        String roomId = roomService.generateCandidateRoomId();
        RoomSessionResponse owner = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");

        JoinRequestView pending = roomService.createJoinRequest(owner.roomId(), "Bob", "1234");

        assertThat(pending.status()).isEqualTo("PENDING");
        assertThat(pending.participantId()).isNull();
        assertThat(roomService.getRoomView(owner.roomId()).participants()).hasSize(1);

        JoinRequestView approved = roomService.decideJoinRequest(owner.roomId(), owner.participantId(), pending.id(), true);

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.participantId()).isNotBlank();
        assertThat(approved.room().participants()).hasSize(2);
    }

    @Test
    void passwordConfirmationMustMatch() {
        String roomId = roomService.generateCandidateRoomId();

        assertThatThrownBy(() -> roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "5678"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void onlyOwnerCanChangeSource() {
        String roomId = roomService.generateCandidateRoomId();
        RoomSessionResponse owner = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");
        RoomSessionResponse viewer = approveViewer(owner, "Bob");

        assertThatThrownBy(() -> roomService.updateSource(owner.roomId(), viewer.participantId(), "https://v.qq.com/demo"))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(roomService.updateSource(owner.roomId(), owner.participantId(), "https://v.qq.com/demo").source().mode())
                .isEqualTo(PlaybackMode.SCREEN_SHARE);
    }

    @Test
    void onlyOwnerCanControlSynchronizedPlayback() {
        String roomId = roomService.generateCandidateRoomId();
        RoomSessionResponse owner = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");
        RoomSessionResponse viewer = approveViewer(owner, "Bob");

        assertThatThrownBy(() -> roomService.applyPlayback(owner.roomId(), viewer.participantId(), "play", 42.5))
                .isInstanceOf(ResponseStatusException.class);

        PlaybackState state = roomService.applyPlayback(owner.roomId(), owner.participantId(), "play", 42.5);

        assertThat(state.playing()).isTrue();
        assertThat(state.positionSeconds()).isEqualTo(42.5);
        assertThat(roomService.getRoomView(owner.roomId()).playback().playing()).isTrue();
    }

    @Test
    void leavingRoomRemovesViewerAndOwnerClosesRoom() {
        String roomId = roomService.generateCandidateRoomId();
        RoomSessionResponse owner = roomService.createRoom(roomId, "Alice", "https://cdn.example.com/movie.mp4", "1234", "1234");
        RoomSessionResponse viewer = approveViewer(owner, "Bob");

        assertThat(roomService.leaveRoom(owner.roomId(), viewer.participantId())).isFalse();
        assertThat(roomService.getRoomView(owner.roomId()).participants()).hasSize(1);

        assertThat(roomService.leaveRoom(owner.roomId(), owner.participantId())).isTrue();
        assertThatThrownBy(() -> roomService.getRoomView(owner.roomId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    private RoomSessionResponse approveViewer(RoomSessionResponse owner, String displayName) {
        JoinRequestView pending = roomService.createJoinRequest(owner.roomId(), displayName, "1234");
        JoinRequestView approved = roomService.decideJoinRequest(owner.roomId(), owner.participantId(), pending.id(), true);
        return new RoomSessionResponse(owner.roomId(), approved.participantId(), approved.room());
    }
}
