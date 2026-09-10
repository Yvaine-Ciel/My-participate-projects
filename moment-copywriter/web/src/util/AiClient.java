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

    public String getModel() {
        return MODEL;
    }

    public String generateMomentCopywriting(
            String scene,
            String mood,
            String style,
            String keywords
    ) throws Exception {
        if (isBlank(API_KEY)) {
            throw new IllegalStateException("Environment variable AI_API_KEY is required");
        }

        if (isBlank(MODEL)) {
            throw new IllegalStateException("Environment variable AI_MODEL is required");
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
            throw new IOException("AI service error: HTTP " + statusCode);
        }

        return parseContent(responseBody);
    }

    public String optimizeMomentCopywriting(
            String scene,
            String mood,
            String style,
            String keywords,
            String currentContent,
            String instruction
    ) throws Exception {
        if (isBlank(API_KEY)) {
            throw new IllegalStateException("Environment variable AI_API_KEY is required");
        }

        if (isBlank(MODEL)) {
            throw new IllegalStateException("Environment variable AI_MODEL is required");
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
            throw new IOException("AI service error: HTTP " + statusCode);
        }

        return parseContent(responseBody);
    }

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
                "You are a professional Chinese WeChat Moments copywriter. "
                        + "Return only the generated captions."
        );
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", buildPrompt(scene, mood, style, keywords));
        messages.add(user);

        return messages;
    }

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
                "You are a professional Chinese WeChat Moments copywriter. "
                        + "Revise the captions according to the user's instruction. "
                        + "Return only the revised captions."
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

    private String buildPrompt(
            String scene,
            String mood,
            String style,
            String keywords
    ) {
        return "Write 3 Chinese WeChat Moments captions.\n"
                + "Scene: " + valueOrDefault(scene) + "\n"
                + "Mood: " + valueOrDefault(mood) + "\n"
                + "Style: " + valueOrDefault(style) + "\n"
                + "Keywords: " + valueOrDefault(keywords) + "\n"
                + "Rules: natural, short, friendly, no markdown.";
    }

    private String buildOptimizePrompt(
            String scene,
            String mood,
            String style,
            String keywords,
            String currentContent,
            String instruction
    ) {
        return "Optimize these Chinese WeChat Moments captions.\n"
                + "Original scene: " + valueOrDefault(scene) + "\n"
                + "Mood: " + valueOrDefault(mood) + "\n"
                + "Style: " + valueOrDefault(style) + "\n"
                + "Keywords: " + valueOrDefault(keywords) + "\n"
                + "Current captions:\n" + valueOrDefault(currentContent) + "\n"
                + "User instruction: " + valueOrDefault(instruction) + "\n"
                + "Rules: keep the same topic, follow the instruction, natural, short, friendly, no markdown.";
    }

    private String valueOrDefault(String value) {
        if (isBlank(value)) {
            return "not provided";
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

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

    private String parseContent(String body) throws IOException {
        Map<?, ?> result = GSON.fromJson(body, Map.class);
        if (result == null) {
            throw new IOException("AI response is empty");
        }

        Object choicesObject = result.get("choices");

        if (!(choicesObject instanceof List)) {
            throw new IOException("AI response does not contain choices");
        }

        List<?> choices = (List<?>) choicesObject;
        if (choices.isEmpty() || !(choices.get(0) instanceof Map)) {
            throw new IOException("AI response choices are empty");
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

        throw new IOException("AI response content is empty");
    }
}
