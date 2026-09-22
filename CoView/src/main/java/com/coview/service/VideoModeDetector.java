// 视频来源可播放性与模式判定服务。
package com.coview.service;

import com.coview.model.SourceDecision;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoModeDetector {

    private static final Set<String> DIRECT_MEDIA_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".ogv", ".ogg", ".m4v", ".mov", ".m3u8"
    );

    private static final Set<String> PROTECTED_PLATFORM_DOMAINS = Set.of(
            "v.qq.com",
            "qq.com",
            "douyin.com",
            "iesdouyin.com",
            "tiktok.com",
            "b23.tv",
            "bilibili.com",
            "iqiyi.com",
            "youku.com",
            "mgtv.com",
            "netflix.com",
            "disneyplus.com",
            "primevideo.com",
            "hulu.com",
            "youtube.com",
            "youtu.be",
            "kuaishou.com",
            "weibo.com",
            "xigua.com",
            "ixigua.com"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s\"'<>，。！？；、]+|www\\.[^\\s\"'<>，。！？；、]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;

    public VideoModeDetector() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    // 根据 URL、平台和响应类型选择播放模式。
    public SourceDecision decide(String rawSourceUrl) {
        URI uri = normalize(rawSourceUrl);
        String normalized = uri.toString();
        String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);

        if (!isHttpUrl(uri)) {
            return SourceDecision.screenShare(normalized, "只有 http 或 https 视频资源才能被其他成员直接加载。", null);
        }
        if (isPrivateOrLocalHost(host)) {
            return SourceDecision.screenShare(normalized, "本地或内网资源无法让其他成员直接访问，请由房主使用屏幕共享。", null);
        }
        if (isProtectedPlatform(host)) {
            return SourceDecision.screenShare(normalized, "该平台页面通常无法被系统嵌入或远程控制，请使用屏幕共享。", null);
        }
        if (hasDirectMediaExtension(uri)) {
            return SourceDecision.sync(normalized, "该地址看起来是浏览器可直接播放的视频资源。", null);
        }

        return probeWithHead(uri)
                .map(contentType -> decideByContentType(normalized, contentType))
                .orElseGet(() -> SourceDecision.screenShare(normalized, "暂时无法确认该资源可直接播放，建议使用屏幕共享。", null));
    }

    // 根据 Content-Type 判断是否可直接播放。
    private SourceDecision decideByContentType(String normalized, String contentType) {
        String cleanType = contentType.toLowerCase(Locale.ROOT);
        if (cleanType.startsWith("video/")
                || cleanType.equals("application/vnd.apple.mpegurl")
                || cleanType.equals("application/x-mpegurl")
                || cleanType.equals("audio/mpegurl")
                || cleanType.equals("audio/x-mpegurl")) {
            return SourceDecision.sync(normalized, "服务器返回的是可直接播放的视频类型。", contentType);
        }
        if (cleanType.startsWith("text/html")) {
            return SourceDecision.screenShare(normalized, "该地址指向网页页面，使用屏幕共享更稳妥。", contentType);
        }
        return SourceDecision.screenShare(normalized, "该资源类型不是浏览器可直接播放的视频。", contentType);
    }

    // 使用 HEAD 请求探测远端资源类型。
    private Optional<String> probeWithHead(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "CoView/1.0 mode-detector")
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return response.headers().firstValue("content-type")
                        .map(value -> value.split(";", 2)[0].trim());
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    // 从用户输入或分享文案中规范化出 URI。
    private URI normalize(String rawSourceUrl) {
        if (rawSourceUrl == null || rawSourceUrl.isBlank()) {
            throw new IllegalArgumentException("请填写视频地址。");
        }

        String candidate = extractFirstUrl(rawSourceUrl.trim());
        if (!candidate.contains("://")) {
            candidate = "https://" + candidate;
        }

        try {
            return new URI(candidate);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("视频地址格式不正确。");
        }
    }

    // 提取输入文本中的第一个网址。
    private String extractFirstUrl(String rawText) {
        Matcher matcher = URL_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return trimTrailingUrlJunk(matcher.group(1));
        }
        return rawText;
    }

    // 去掉复制链接末尾常见标点。
    private String trimTrailingUrlJunk(String url) {
        String clean = url.trim();
        while (!clean.isEmpty() && "，。！？；、,.)]}>\"'".indexOf(clean.charAt(clean.length() - 1)) >= 0) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    // 判断是否是 http/https 地址。
    private boolean isHttpUrl(URI uri) {
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("").toLowerCase(Locale.ROOT);
        return scheme.equals("http") || scheme.equals("https");
    }

    // 判断路径是否带有常见直链媒体后缀。
    private boolean hasDirectMediaExtension(URI uri) {
        String path = Optional.ofNullable(uri.getPath()).orElse("").toLowerCase(Locale.ROOT);
        return DIRECT_MEDIA_EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    // 判断是否属于常见受限视频平台。
    private boolean isProtectedPlatform(String host) {
        return PROTECTED_PLATFORM_DOMAINS.stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }

    // 拦截本机和内网地址，避免房客无法访问。
    private boolean isPrivateOrLocalHost(String host) {
        if (host.equals("localhost") || host.endsWith(".localhost") || host.equals("0.0.0.0") || host.equals("::1")) {
            return true;
        }
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return false;
        }

        String[] parts = host.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 10
                || first == 127
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)
                || (first == 169 && second == 254);
    }
}
