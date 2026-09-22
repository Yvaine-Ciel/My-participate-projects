// 同步播放状态快照。
package com.coview.model;

import java.time.Instant;

public record PlaybackState(
        boolean playing,
        double positionSeconds,
        Instant updatedAt
) {
    // 创建初始停止状态。
    public static PlaybackState stopped() {
        return new PlaybackState(false, 0.0, Instant.now());
    }

    // 根据播放动作生成新的播放状态。
    public PlaybackState withPlayback(boolean newPlaying, double newPositionSeconds) {
        return new PlaybackState(newPlaying, Math.max(0.0, newPositionSeconds), Instant.now());
    }
}
