// 视频来源模式判定结果。
package com.coview.model;

public record SourceDecision(
        PlaybackMode mode,
        String normalizedUrl,
        String reason,
        String contentType,
        boolean directMedia
) {
    // 标记来源可使用同步播放模式。
    public static SourceDecision sync(String normalizedUrl, String reason, String contentType) {
        return new SourceDecision(PlaybackMode.SYNC, normalizedUrl, reason, contentType, true);
    }

    // 标记来源需要使用屏幕共享模式。
    public static SourceDecision screenShare(String normalizedUrl, String reason, String contentType) {
        return new SourceDecision(PlaybackMode.SCREEN_SHARE, normalizedUrl, reason, contentType, false);
    }
}
