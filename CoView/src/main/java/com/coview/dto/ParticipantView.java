// 房间成员响应视图。
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
