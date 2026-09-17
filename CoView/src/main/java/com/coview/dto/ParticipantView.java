package com.coview.dto;

import java.time.Instant;

public record ParticipantView(
        String id,
        String displayName,
        boolean owner,
        Instant joinedAt,
        Instant lastSeenAt
) {
}
