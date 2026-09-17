package com.coview.model;

public record SourceDecision(
        PlaybackMode mode,
        String normalizedUrl,
        String reason,
        String contentType,
        boolean directMedia
) {
    public static SourceDecision sync(String normalizedUrl, String reason, String contentType) {
        return new SourceDecision(PlaybackMode.SYNC, normalizedUrl, reason, contentType, true);
    }

    public static SourceDecision screenShare(String normalizedUrl, String reason, String contentType) {
        return new SourceDecision(PlaybackMode.SCREEN_SHARE, normalizedUrl, reason, contentType, false);
    }
}
