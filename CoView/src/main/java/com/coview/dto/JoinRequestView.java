// 加入申请状态响应视图。
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
