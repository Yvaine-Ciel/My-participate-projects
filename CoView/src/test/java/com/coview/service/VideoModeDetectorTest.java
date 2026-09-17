package com.coview.service;

import com.coview.model.PlaybackMode;
import com.coview.model.SourceDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoModeDetectorTest {

    private final VideoModeDetector detector = new VideoModeDetector();

    @Test
    void directMediaExtensionUsesSyncMode() {
        SourceDecision decision = detector.decide("https://cdn.example.com/watch/movie.mp4");

        assertThat(decision.mode()).isEqualTo(PlaybackMode.SYNC);
        assertThat(decision.normalizedUrl()).isEqualTo("https://cdn.example.com/watch/movie.mp4");
    }

    @Test
    void protectedPlatformUsesScreenShareMode() {
        SourceDecision decision = detector.decide("https://v.qq.com/x/cover/demo.html");

        assertThat(decision.mode()).isEqualTo(PlaybackMode.SCREEN_SHARE);
        assertThat(decision.directMedia()).isFalse();
    }

    @Test
    void shareTextExtractsFirstUrl() {
        SourceDecision decision = detector.decide("这个视频很好看，复制链接 https://www.bilibili.com/video/BV1234567?p=1 一起看");

        assertThat(decision.mode()).isEqualTo(PlaybackMode.SCREEN_SHARE);
        assertThat(decision.normalizedUrl()).isEqualTo("https://www.bilibili.com/video/BV1234567?p=1");
    }

    @Test
    void missingSchemeIsNormalizedToHttps() {
        SourceDecision decision = detector.decide("media.example.com/video.webm");

        assertThat(decision.mode()).isEqualTo(PlaybackMode.SYNC);
        assertThat(decision.normalizedUrl()).isEqualTo("https://media.example.com/video.webm");
    }
}
