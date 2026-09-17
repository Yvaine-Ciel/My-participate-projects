package com.coview.dto;

public record RoomSessionResponse(
        String roomId,
        String participantId,
        RoomView room
) {
}
