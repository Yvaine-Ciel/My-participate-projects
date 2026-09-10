package servlet;

import dao.CopywritingRecordDao;
import dao.CopywritingRecordStepDao;
import dao.UserTagDao;
import entity.CopywritingRecord;
import util.AiClient;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/copywriting/generate")
public class GenerateCopywritingServlet extends BaseApiServlet {
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        String scene = JsonUtil.getString(request, body, "scene");
        String mood = JsonUtil.getString(request, body, "mood");
        String style = JsonUtil.getString(request, body, "style");
        String keywords = JsonUtil.getString(request, body, "keywords");
        int userId = requireUserId(request, response);

        if (userId <= 0) {
            return;
        }

        if (scene == null) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "scene is required");
            return;
        }

        String promptKeywords = appendUserTags(
                keywords,
                new UserTagDao().listByUserId(userId)
        );
        AiClient aiClient = new AiClient();
        String content;

        try {
            content = aiClient.generateMomentCopywriting(scene, mood, style, promptKeywords);
        } catch (IllegalStateException e) {
            writeFail(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            writeFail(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "AI service request failed");
            return;
        }

        CopywritingRecord record = new CopywritingRecord();
        record.setUserId(userId);
        record.setScene(scene);
        record.setMood(mood);
        record.setStyle(style);
        record.setKeywords(keywords);
        record.setGeneratedContent(content);
        record.setAiModel(aiClient.getModel());

        int recordId = new CopywritingRecordDao().add(record);
        if (recordId > 0) {
            new CopywritingRecordStepDao().add(recordId, 1, scene, content);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("recordId", recordId);
        data.put("keywords", keywords);
        data.put("saved", recordId > 0);
        data.put("model", aiClient.getModel());

        writeSuccess(response, data);
    }

    private String appendUserTags(String keywords, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return keywords;
        }

        if (keywords != null && keywords.contains("用户标签")) {
            return keywords;
        }

        String tagText = String.join("、", tags);
        if (keywords == null || keywords.trim().isEmpty()) {
            return "用户标签：" + tagText;
        }

        return keywords.trim() + "；用户标签：" + tagText;
    }
}
