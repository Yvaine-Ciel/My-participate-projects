package util;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiClient {
    private static final Gson GSON = new Gson();

    private static final String API_URL =
            System.getenv().getOrDefault(
                    "AI_API_URL",
                    "https://api.deepseek.com/chat/completions"
            );

    private static final String API_KEY =
            System.getenv("AI_API_KEY");

    private static final String MODEL =
            System.getenv().getOrDefault(
                    "AI_MODEL",
                    "deepseek-v4-flash"
            );

    // 获取当前 AI 模型
    public String getModel() {
        return MODEL;
    }

    // 生成文案
    public String generateAiCopywriting(
            String scene,
            String mood,
            String style,
            String keywords
    ) throws Exception {
        if (isBlank(API_KEY)) {
            throw new IllegalStateException("缺少 AI_API_KEY 环境变量");
        }

        if (isBlank(MODEL)) {
            throw new IllegalStateException("缺少 AI_MODEL 环境变量");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("temperature", 0.85);
        payload.put("messages", buildMessages(scene, mood, style, keywords));

        HttpURLConnection connection =
                (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);

        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }

        int statusCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection, statusCode);

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("AI 服务返回错误：HTTP " + statusCode);
        }

        return parseContent(responseBody);
    }

    // 优化文案
    public String optimizeAiCopywriting(
            String scene,
            String mood,
            String style,
            String keywords,
            String currentContent,
            String instruction
    ) throws Exception {
        if (isBlank(API_KEY)) {
            throw new IllegalStateException("缺少 AI_API_KEY 环境变量");
        }

        if (isBlank(MODEL)) {
            throw new IllegalStateException("缺少 AI_MODEL 环境变量");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("temperature", 0.75);
        payload.put("messages", buildOptimizeMessages(
                scene,
                mood,
                style,
                keywords,
                currentContent,
                instruction
        ));

        HttpURLConnection connection =
                (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);

        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }

        int statusCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection, statusCode);

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("AI 服务返回错误：HTTP " + statusCode);
        }

        return parseContent(responseBody);
    }

    // 构建生成消息
    private List<Map<String, String>> buildMessages(
            String scene,
            String mood,
            String style,
            String keywords
    ) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put(
                "content",
                "You are a professional Chinese AI copywriting assistant. "
                        + "Return only the generated copywriting."
        );
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", buildPrompt(scene, mood, style, keywords));
        messages.add(user);

        return messages;
    }

    // 构建优化消息
    private List<Map<String, String>> buildOptimizeMessages(
            String scene,
            String mood,
            String style,
            String keywords,
            String currentContent,
            String instruction
    ) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put(
                "content",
                "You are a professional Chinese AI copywriting assistant. "
                        + "Revise the copywriting according to the user's instruction. "
                        + "Return only the revised copywriting."
        );
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", buildOptimizePrompt(
                scene,
                mood,
                style,
                keywords,
                currentContent,
                instruction
        ));
        messages.add(user);

        return messages;
    }

    // 构建生成提示词
    private String buildPrompt(
            String scene,
            String mood,
            String style,
            String keywords
    ) {
        return "Write 3 pieces of Chinese AI copywriting.\n"
                + "Scene: " + valueOrDefault(scene) + "\n"
                + "Mood: " + valueOrDefault(mood) + "\n"
                + "Style: " + valueOrDefault(style) + "\n"
                + "Keywords: " + valueOrDefault(keywords) + "\n"
                + "Rules: natural, short, friendly, no markdown.";
    }

    // 构建优化提示词
    private String buildOptimizePrompt(
            String scene,
            String mood,
            String style,
            String keywords,
            String currentContent,
            String instruction
    ) {
        return "Optimize this Chinese AI copywriting.\n"
                + "Original scene: " + valueOrDefault(scene) + "\n"
                + "Mood: " + valueOrDefault(mood) + "\n"
                + "Style: " + valueOrDefault(style) + "\n"
                + "Keywords: " + valueOrDefault(keywords) + "\n"
                + "Current copywriting:\n" + valueOrDefault(currentContent) + "\n"
                + "User instruction: " + valueOrDefault(instruction) + "\n"
                + "Rules: keep the same topic, follow the instruction, natural, short, friendly, no markdown.";
    }

    // 空值转默认文本
    private String valueOrDefault(String value) {
        if (isBlank(value)) {
            return "not provided";
        }
        return value;
    }

    // 判断字符串为空
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 读取 AI 响应体
    private String readResponseBody(HttpURLConnection connection, int statusCode)
            throws IOException {
        InputStream inputStream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (inputStream == null) {
            return "";
        }

        try (InputStream in = inputStream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    // 解析 AI 返回内容
    private String parseContent(String body) throws IOException {
        Map<?, ?> result = GSON.fromJson(body, Map.class);
        if (result == null) {
            throw new IOException("AI 返回内容为空");
        }

        Object choicesObject = result.get("choices");

        if (!(choicesObject instanceof List)) {
            throw new IOException("AI 返回结果缺少 choices");
        }

        List<?> choices = (List<?>) choicesObject;
        if (choices.isEmpty() || !(choices.get(0) instanceof Map)) {
            throw new IOException("AI 返回结果 choices 为空");
        }

        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Object messageObject = firstChoice.get("message");

        if (messageObject instanceof Map) {
            Object content = ((Map<?, ?>) messageObject).get("content");
            if (content != null) {
                return String.valueOf(content).trim();
            }
        }

        Object text = firstChoice.get("text");
        if (text != null) {
            return String.valueOf(text).trim();
        }

        throw new IOException("AI 返回文案内容为空");
    }
}
