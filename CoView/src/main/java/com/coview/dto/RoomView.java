// 房间当前状态响应视图。
package com.coview.dto;

import com.coview.model.PlaybackState;
import com.coview.model.SourceDecision;

import java.time.Instant;
import java.util.List;

public record RoomView(
        String id,
        String ownerId,
        Instant createdAt,
        Instant lastActiveAt,
        SourceDecision source,
        PlaybackState playback,
        boolean screenShareActive,
        List<ParticipantView> participants
) {
}
