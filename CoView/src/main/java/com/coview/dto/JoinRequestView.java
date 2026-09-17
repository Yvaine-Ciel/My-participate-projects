package com.coview.dto;

import java.time.Instant;

public record JoinRequestView(
        String id,
        String roomId,
        String displayName,
        String status,
        Instant requestedAt,
        String participantId,
        RoomView room,
        String message
) {
}
