package com.coview.model;

import java.time.Instant;

public record PlaybackState(
        boolean playing,
        double positionSeconds,
        Instant updatedAt
) {
    public static PlaybackState stopped() {
        return new PlaybackState(false, 0.0, Instant.now());
    }

    public PlaybackState withPlayback(boolean newPlaying, double newPositionSeconds) {
        return new PlaybackState(newPlaying, Math.max(0.0, newPositionSeconds), Instant.now());
    }
}
